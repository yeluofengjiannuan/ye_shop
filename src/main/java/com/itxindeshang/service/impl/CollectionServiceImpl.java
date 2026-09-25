package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.common.result.SimpleCursorCommonEntity;
import com.itxindeshang.common.result.SimpleCursorCommonResult;
import com.itxindeshang.context.BaseContext;
import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.infrastructure.es.service.ProductDocumentService;
import com.itxindeshang.infrastructure.redis.connect.RedisConnector;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.mapper.CollectionMapper;
import com.itxindeshang.pojo.entity.ProductCollection;
import com.itxindeshang.pojo.vo.SimpleProductVO;
import com.itxindeshang.service.CollectionService;
import com.itxindeshang.util.DateUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CollectionServiceImpl  extends ServiceImpl<CollectionMapper, ProductCollection> implements CollectionService {
    @Resource
    private ProductDocumentService productDocumentService;

    @Resource
    private CopyMapper copyMapper;

    /**
     * 用户收藏商品
     * @param productId 商品id
     */
    @Override
    public Result<?> addCollection(Long productId) {
        String collectionKey = RedisKeyGenerator.productCollection(productId);
        String userId = BaseContext.getUserId();
        ProductCollection productCollection = ProductCollection.builder().productId(productId).userId(Long.valueOf(userId)).build();
        boolean isSuccess = save(productCollection);
        if (!isSuccess) {
            return Result.error(MessageConstant.TOM_CAT_ERROR);
        }
        RedisConnector.delete(collectionKey);

        return Result.success();
    }

    /**
     * 删除收藏
     * @param productIds 商品id集合
     */
    @Override
    public Result<?> deleteCollection(List<Long> productIds) {
        String userId = BaseContext.getUserId();
        LambdaQueryWrapper<ProductCollection> lambdaQueryWrapper = new LambdaQueryWrapper<ProductCollection>().eq(ProductCollection::getUserId, userId).in(ProductCollection::getProductId, productIds);
        boolean isSuccess = remove(lambdaQueryWrapper);
        if (!isSuccess) {
            return Result.error(MessageConstant.TOM_CAT_ERROR);
        }
        //删除缓存
        RedisConnector.executePipelined(new SessionCallback<>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                for (Long productId : productIds) {
                    String key = RedisKeyGenerator.productCollection(productId);
                    operations.delete((K) key);
                }
                return null;
            }
        });
        return Result.success();
    }

    /**
     * 获取用户收藏商品列表
     */
    @Override
    public Result<SimpleCursorCommonResult> getCollectionList(SimpleCursorCommonEntity simpleCursorCommonEntity) {
        String userId = BaseContext.getUserId();
        Integer querySize = simpleCursorCommonEntity.getQuerySize();

        // 1. 一次性查出所有收藏
        List<ProductCollection> allCollections = queryAllCollections(userId);
        if (allCollections.isEmpty()) {
            return Result.success(buildEmptyResult(querySize));
        }

        // 2. 根据游标找到起始位置
        int startIndex = findStartIndex(allCollections, simpleCursorCommonEntity.getSortValue(), simpleCursorCommonEntity.getSortId());

        // 3. 从起始位置开始，凑够 querySize 个有效商品，同时返回实际扫描到的 cursor
        CollectResult collectResult = collectValidProducts(allCollections, startIndex, querySize);

        // 4. 组装返回结果
        return Result.success(buildResult(allCollections, collectResult, querySize));
    }


    /** 查询用户所有收藏，按创建时间倒序 + ID 倒序 */
    private List<ProductCollection> queryAllCollections(String userId) {
        return lambdaQuery()
                .eq(ProductCollection::getUserId, userId)
                .orderByDesc(ProductCollection::getCreateTime, ProductCollection::getId)
                .list();
    }

    /** 空结果 */
    private SimpleCursorCommonResult buildEmptyResult(Integer querySize) {
        return SimpleCursorCommonResult.builder()
                .isEnd(true)
                .simpleCursorCommonEntity(SimpleCursorCommonEntity.builder()
                        .querySize(querySize)
                        .build())
                .list(Collections.emptyList())
                .build();
    }

    /** 根据游标定位起始下标，二分查找优化 */
    private int findStartIndex(List<ProductCollection> collections, String sortValue, Long sortId) {
        if (StringUtils.isBlank(sortValue) || sortId == null) {
            return 0;
        }
        LocalDateTime beginTime = DateUtils.parseToLocalDateTime(sortValue);

        // 二分查找：找第一个满足 (createTime < beginTime) || (createTime == beginTime && id < sortId) 的位置
        int left = 0, right = collections.size();
        while (left < right) {
            int mid = (left + right) / 2;
            ProductCollection pc = collections.get(mid);
            if (pc.getCreateTime().isBefore(beginTime)

                    || (pc.getCreateTime().isEqual(beginTime) && pc.getId() < sortId)) {
                right = mid;  // mid 可能是答案，继续往左找
            } else {
                left = mid + 1;  // mid 不满足，往右找
            }
        }
        return left;  // left 就是第一个满足条件的位置
    }

    /** 收集结果 + 游标位置 */
    private static class CollectResult {
        List<SimpleProductVO> products;
        int cursor;          // 实际扫描到的下标（下一个要查的位置）
        int lastValidIndex;  // 最后一个有效商品在 allCollections 中的下标

        CollectResult(List<SimpleProductVO> products, int cursor, int lastValidIndex) {
            this.products = products;
            this.cursor = cursor;
            this.lastValidIndex = lastValidIndex;
        }
    }

    /** 从 startIndex 开始，循环查 ES 凑够 querySize 个有效商品 */
    private CollectResult collectValidProducts(List<ProductCollection> allCollections,
                                               int startIndex, int querySize) {
        List<SimpleProductVO> result = new ArrayList<>();
        int cursor = startIndex;
        int lastValidIndex = -1;
        Map<Long, ProductDocument> docMap = new HashMap<>();  // 挪到循环内部

        while (result.size() < querySize && cursor < allCollections.size()) {
            int endIndex = Math.min(cursor + querySize, allCollections.size());
            List<ProductCollection> batch = allCollections.subList(cursor, endIndex);

            // 每批重新查 ES，docMap 只存当前批次
            List<Long> productIds = batch.stream()
                    .map(ProductCollection::getProductId)
                    .toList();
            docMap.clear();  // 清空上一批
            productDocumentService.searchByIdList(productIds)
                    .forEach(doc -> docMap.put(doc.getId(), doc));

            for (int i = 0; i < batch.size(); i++) {
                if (result.size() >= querySize) break;
                ProductCollection pc = batch.get(i);
                ProductDocument doc = docMap.get(pc.getProductId());
                if (doc != null) {
                    result.add(copyMapper.ProductDocumentToSimpleProductVO(doc));
                    lastValidIndex = cursor + i;
                }
            }

            cursor = endIndex;
        }
        return new CollectResult(result, cursor, lastValidIndex);
    }

    /** 组装返回结果 */
    private SimpleCursorCommonResult buildResult(List<ProductCollection> allCollections,
                                                 CollectResult collectResult,
                                                 int querySize) {
        // isEnd 用 cursor 判断，不是 resultList.size()
        boolean isEnd = collectResult.cursor >= allCollections.size();

        SimpleCursorCommonEntity nextCursor = null;
        if (!collectResult.products.isEmpty()) {
            // 用记录好的 lastValidIndex 直接取，不用反查
            ProductCollection lastPc = allCollections.get(collectResult.lastValidIndex);
            nextCursor = SimpleCursorCommonEntity.builder()
                    .sortId(lastPc.getId())
                    .sortValue(DateUtils.formatLocalDateTime(lastPc.getCreateTime()))
                    .querySize(querySize)  // 存客户端请求的 querySize，不是实际返回数
                    .build();
        }

        return SimpleCursorCommonResult.builder()
                .isEnd(isEnd)
                .simpleCursorCommonEntity(nextCursor)
                .list(collectResult.products)
                .build();
    }
}
