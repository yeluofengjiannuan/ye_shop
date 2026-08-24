package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.common.constant.DataConstant;
import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.common.exception.BusinessException;
import com.itxindeshang.common.exception.ProductException;
import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.common.result.CursorCommonEntity;
import com.itxindeshang.common.result.CursorCommonResult;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.context.BaseContext;
import com.itxindeshang.infrastructure.redis.connect.RedisConnector;
import com.itxindeshang.infrastructure.redis.connect.StringRedisConnector;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.infrastructure.redis.properties.RedisCacheTtlProperties;
import com.itxindeshang.mapper.ProductImageMapper;
import com.itxindeshang.mapper.ProductMapper;
import com.itxindeshang.mapper.ProductSpecMapper;
import com.itxindeshang.pojo.dto.ProductDTO;
import com.itxindeshang.pojo.dto.ProductUpdateDTO;
import com.itxindeshang.pojo.entity.*;
import com.itxindeshang.pojo.enums.CommonStatus;
import com.itxindeshang.pojo.enums.ProductSortTypeEnum;
import com.itxindeshang.pojo.vo.ProductVO;
import com.itxindeshang.service.*;
import com.itxindeshang.util.JacksonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
    //TODO:查询商品浏览量提高在redis展示修改，身份唯一标识防止浏览量暴涨，后续补充redis相关
    //TODO:前端如果传个 "abc" productId 的校验就是问题了，要解决

    @Resource
    private ProductMapper productMapper;

    @Resource
    private CopyMapper copyMapper;

    @Resource
    private CategoryService categoryService;

    @Resource
    private  RedisCacheTtlProperties redisCacheTtlProperties;

    @Resource
    private CollectionService collectionService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ProductSpecService productSpecService;

    @Resource
    private ProductImageService productImageService;

    @Resource
    private ProductImageMapper productImageMapper;

    @Resource
    private ProductSpecMapper productSpecMapper;

    /**
     *  新增商品
     * @param productDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> addProduct(ProductDTO productDTO) {
        Product product = copyMapper.productDTOToProduct(productDTO);
        Category category = categoryService.getById(product.getCategoryId());
        List<ProductImage> images =productDTO.getImageUrls();
        List<ProductSpec> specList = productDTO.getSpecList();
        if (CollectionUtils.isEmpty(images) || CollectionUtils.isEmpty(specList)) {
            throw new ProductException(MessageConstant.PRODUCT_IMAGE_OR_SPEC_EMPTY);
        }
        if (category == null) {
            throw new ProductException(MessageConstant.PRODUCT_CATEGORY_INVALID);
        }
        Long categoryParentId = category.getParentId();
        //商品不得出现在第一级分类
        //TODO:这是二级分类下才能这样，后续考虑增加分级代码无法复用，考虑优化
        if (DataConstant.ZERO_LONG.equals(categoryParentId)) {
            //TODO:事务只认异常，这里要抛异常
            throw new ProductException(MessageConstant.SQL_MESSAGE_SAVE_ERROR);
        }
        boolean isSuccess = save(product);
        if (!isSuccess) {
            throw new ProductException(MessageConstant.SQL_MESSAGE_SAVE_ERROR);
        }
        Long productId = product.getId();
        images.forEach(image -> {
            image.setProductId(productId);
            image.setId(null);
        });
        specList.forEach(spec -> {
            spec.setProductId(productId);
            spec.setId(null);
        });
        isSuccess = productSpecService.saveBatch(specList);
        if (!isSuccess) {
            throw new ProductException(MessageConstant.SQL_MESSAGE_SAVE_ERROR);
        }
        isSuccess = productImageService.saveBatch(images);
        if (!isSuccess) {
            throw new ProductException(MessageConstant.SQL_MESSAGE_SAVE_ERROR);
        }
        lambdaUpdate()
                .eq(Product::getId, productId)
                // 让数据库自己求和，Java 一行代码搞定！
                .setSql("stock = (SELECT IFNULL(SUM(stock), 0) FROM product_spec WHERE product_id = " + productId + ")")
                // 让数据库自己求最低规格价
                .setSql("price = (SELECT IFNULL(MIN(price), 0) FROM product_spec WHERE product_id = " + productId + ")")
                .setSql("enterprice_prise = (SELECT IFNULL(MIN(enterprise_price), 0) FROM product_spec WHERE product_id = " + productId + ")")
                .update();
        return Result.success(productId);
    }
    /**
     * 游标查询分类下所有商品列表
     * @return
     */
    @Override
    public Result<CursorCommonResult> getCategoryProductList(CursorCommonEntity cursorCommonEntity, Long categoryId) {
        //TODO: 确保查询categoryId是二级分类
        //在值相同时确保不重复
        Long sortId = cursorCommonEntity.getSortId();
        String sortType = cursorCommonEntity.getSortType();
        String sortValue = cursorCommonEntity.getSortValue();
        ProductSortTypeEnum productSortTypeEnum = ProductSortTypeEnum.getByValue(sortType);
        //上一次查询的最后值/定位
        sortValue = ProductSortTypeEnum.filterFormatSortValue(productSortTypeEnum, sortValue);
        Integer querySize = cursorCommonEntity.getQuerySize();
        String sortField = productSortTypeEnum.getSortField();
        String dbValue = productSortTypeEnum.getDbValue();//TODO:未使用字段，后续可以考虑删除或者功能复用
        boolean isAsc = productSortTypeEnum.getCommonSortTypeEnum().isAsc();
        List<ProductVO> queryList = productMapper.getCategoryProductList(categoryId, sortField, sortId, sortValue, isAsc, querySize);
        return getCursorCommonResult(queryList,querySize, productSortTypeEnum, sortType);
    }

    /**
     * 游标分类查询简介商品列表
     * @param cursorCommonEntity 分类游标通用实体
     * @param keyword 关键词
     * @return 分类游标通用实体
     */
    //TODO:空传判断AOP
    @Override
    public Result<CursorCommonResult> searchProductList(CursorCommonEntity cursorCommonEntity, String keyword) {
        Integer querySize = cursorCommonEntity.getQuerySize();
        String sortType = cursorCommonEntity.getSortType();
        Long sortId = cursorCommonEntity.getSortId();
        String sortValue = cursorCommonEntity.getSortValue();
        ProductSortTypeEnum productSortTypeEnum = ProductSortTypeEnum.getByValue(sortType);
        String sortField = productSortTypeEnum.getSortField();
        sortValue = ProductSortTypeEnum.filterFormatSortValue(productSortTypeEnum, sortValue);
        boolean isAsc = productSortTypeEnum.getCommonSortTypeEnum().isAsc();
        List<ProductVO> queryList = productMapper.searchProductList(sortField, sortValue,sortId, isAsc, querySize, keyword);

        return getCursorCommonResult(queryList,querySize, productSortTypeEnum, sortType);

    }

    /**
     * 根据商品id查询商品详情
     * @param productId 商品id
     * TODO：后续看是否加事务
     * @return
     */
    @Override
    public Result getProductDetail(String productId) {
        //TODO:这里要增加浏览量的，redis储存加异步增加
        //方案：uv:product:{id}:{今天日期} {userId},ttl 7天吗，还要考虑异步存硬件如clickHouse，看来得后续rocketmq补充功能了,那现在就先在第一次查出redis的情况下先存sql
        //这个常量思考下
        if (StringUtils.isBlank(productId)) {
            return Result.error(MessageConstant.TOM_CAT_ERROR);
        }
        String userId = BaseContext.getUserId();
        Long userIdLong = StringUtils.isNotBlank(userId) ? Long.valueOf(userId) : null;
        //第一步：查商品详情缓存
        String productDetailKey = RedisKeyGenerator.productDetail(Long.valueOf(productId));
        Map<String, Object> productDetailMap = RedisConnector.opsForHash().entries(productDetailKey);

        Product resultProduct;

        // 1.1 缓存命中，且不是空对象（防穿透的空对象 size=1）
        if (!productDetailMap.isEmpty() && productDetailMap.size() > 1) {
            resultProduct = JacksonUtils.fromMap(productDetailMap, Product.class);
        }  else {
            // 1.2 缓存未命中，使用 Redisson 防击穿
            String lockKey = RedisKeyGenerator.lockProductDetail(productId);
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean isLocked = lock.tryLock(5, -1, TimeUnit.SECONDS);

                if (isLocked) {
                    // 【双重检查】
                    Map<String, Object> doubleCheckMap = RedisConnector.opsForHash().entries(productDetailKey);
                    if (!CollectionUtils.isEmpty(doubleCheckMap) && doubleCheckMap.size() > 1) {
                        resultProduct = JacksonUtils.fromMap(doubleCheckMap, Product.class);
                    } else {
                        // 真正去查数据库
                        Product product = productMapper.selectByProductId(productId);
                        if (Objects.isNull(product)) {

                            StringRedisConnector.opsForHash().putAll(productDetailKey, Map.of(Product.Fields.id, productId));
                            StringRedisConnector.expire(productDetailKey, 60, TimeUnit.SECONDS);

                            //resultProduct 设为 null，作为“数据不存在”的标识
                            resultProduct = null;
                        } else {
                            // 查到真实数据，写入缓存
                            Map<String, Object> productDetailResultMap = JacksonUtils.toMap(product);
                            RedisConnector.opsForHash().putAll(productDetailKey, productDetailResultMap);
                            StringRedisConnector.expire(productDetailKey, redisCacheTtlProperties.getProductDetailTtl(), TimeUnit.SECONDS);
                            resultProduct = product;
                        }
                    }
                } else {
                    // 没抢到锁，休眠重试
                    Thread.sleep(50);
                    Map<String, Object> retryMap = RedisConnector.opsForHash().entries(productDetailKey);
                    if (!retryMap.isEmpty() && retryMap.size() > 1) {
                        resultProduct = JacksonUtils.fromMap(retryMap, Product.class);
                    } else {
                        // 如果休眠后依然没缓存，说明对方查库也失败了，直接返回错误
                        return Result.error(MessageConstant.DATA_ERROR);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                //常量
                throw new RuntimeException(MessageConstant.LOCK_ERROR, e);
            } finally {
                // 【核心】：安全释放锁，无论中间发生了什么，这里一定会执行
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

// 第二步：异步记录浏览量
//如果上面查不到数据，直接跳过后续逻辑
        if (resultProduct != null && userIdLong != null) {
            String productViewKey = RedisKeyGenerator.productView(productId, userIdLong);
            Boolean isFirstView = RedisConnector.opsForValue().setIfAbsent(productViewKey, "1", 7, TimeUnit.DAYS);
            if (Boolean.TRUE.equals(isFirstView)) {
                String countKey = RedisKeyGenerator.productViewCount(productId);
                RedisConnector.opsForValue().increment(countKey);
                // TODO: 后续异步更新数据库,这里的逻辑后续我再优化为异步，现在直接sql加一
                try {
                    lambdaUpdate()
                            .eq(Product::getId, productId)
                            .setSql("view_count = view_count + 1") // 【修复1】：正确的字段名
                            .update();
                } catch (Exception e) {
                    // 浏览量统计属于非核心业务，即使失败也不应阻断商品详情的返回,TODO：常量修改
                    log.error("更新商品浏览量失败, productId: {}", productId, e);
                }
            }
        }
// ================= 第三步：处理收藏状态 =================
// 【修复】：同样加上 null 保护，防止空指针异常
        if (resultProduct != null) {
            resultProduct.setIsCollection(CommonStatus.INACTIVE.getNumber());
            if (userIdLong != null) {
                // ... 收藏逻辑保持不变 ...
                String collectionKey = RedisKeyGenerator.productCollection(Long.valueOf(productId));
                Set<Object> userIdSet = (Set<Object>) RedisConnector.opsForValue().get(collectionKey);

                // 如果 Redis 里没有收藏列表，查库并回填
                if (CollectionUtils.isEmpty(userIdSet)) {
                    List<ProductCollection> productCollectionList = collectionService.lambdaQuery()
                            .eq(ProductCollection::getProductId, productId).list();
                    userIdSet = productCollectionList.stream()
                            .map(ProductCollection::getUserId).collect(Collectors.toSet());

                    if (!userIdSet.isEmpty()) {
                        RedisConnector.opsForValue().set(collectionKey, userIdSet);
                        RedisConnector.expire(collectionKey, redisCacheTtlProperties.getProductCollectionTtl(), TimeUnit.SECONDS);
                    }
                }
                // 判断当前用户是否在收藏集合中
                if (userIdSet != null && userIdSet.contains(userIdLong)) {
                    resultProduct.setIsCollection(CommonStatus.ACTIVE.getNumber());
                }
            }
            return Result.success(resultProduct);
        } else {
            // 数据确实不存在，在这里统一返回错误
            return Result.error(MessageConstant.DATA_ERROR);
        }
    }

    /**
     * 下架商品
     * @param productId 商品id
     * @return
     */
    @Override
    public Result offShelfProduct(String productId) {
        // 1. 参数校验
        if (StringUtils.isBlank(productId)) {
            return Result.error(MessageConstant.TOM_CAT_ERROR);
        }
        // 2. 更新数据库状态为下架（假设 0 为下架状态）
        boolean isSuccess =lambdaUpdate()
                .eq(Product::getId, productId)
                .eq(Product::getStatus,CommonStatus.ACTIVE.getNumber())
                .set(Product::getStatus, CommonStatus.INACTIVE.getNumber())
                .update();
        if (!isSuccess) {
            return Result.error(MessageConstant.DATA_ERROR);
        }
        String productDetailKey = RedisKeyGenerator.productDetail(Long.valueOf(productId));
        RedisConnector.delete(productDetailKey);
        //多余的key采取ttl自然过期策略
        return Result.success();
    }

    /**
     * 上架商品
     * @param productId 商品id
     * @return
     */
    @Override
    public Result onShelfProduct(String productId) {
        if (StringUtils.isBlank(productId)) {
            return Result.error(MessageConstant.TOM_CAT_ERROR);
        }
        boolean isSuccess = lambdaUpdate()
                .eq(Product::getId, productId)
                .eq(Product::getStatus,CommonStatus.INACTIVE.getNumber())
                .set(Product::getStatus, CommonStatus.ACTIVE.getNumber())
                .update();

        if (!isSuccess) {
            return Result.error(MessageConstant.DATA_ERROR);
        }
        return Result.success();
    }

    /**
     * 更新商品信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateProduct(ProductUpdateDTO productUpdateDTO) {
        Product existProduct = this.getById(productUpdateDTO.getId());
        if (existProduct == null) {
            throw new ProductException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        Product updateProduct = copyMapper.productUpdateDTOToProduct(productUpdateDTO);
        Long productId =updateProduct.getId();
        List<ProductImage> imageUrls = productUpdateDTO.getImageUrls();
        List<ProductSpec> specList = productUpdateDTO.getSpecList();
        boolean isSuccess = updateById(updateProduct);
        if (!isSuccess) {
            throw new ProductException(MessageConstant.SQL_MESSAGE_SAVE_ERROR);
        }
        if (!CollectionUtils.isEmpty(imageUrls)) {
            productImageMapper.delete(new LambdaQueryWrapper<ProductImage>()
                    .eq(ProductImage::getProductId, updateProduct.getId()));
            imageUrls.forEach(imageUrl -> {
                imageUrl.setId(null);
                imageUrl.setProductId(updateProduct.getId());
            });
            isSuccess = productImageService.saveBatch(imageUrls);
            if (!isSuccess) {
                throw new ProductException(MessageConstant.SQL_MESSAGE_SAVE_ERROR);
            }
        }
        if (!CollectionUtils.isEmpty(specList)) {
            productSpecMapper.delete(new LambdaQueryWrapper<ProductSpec>()
                    .eq(ProductSpec::getProductId, updateProduct.getId()));
            specList.forEach(spec -> {
                spec.setId(null);
                spec.setProductId(updateProduct.getId());
            });
            isSuccess = productSpecService.saveBatch(specList);
            if (!isSuccess) {
                throw new ProductException(MessageConstant.SQL_MESSAGE_SAVE_ERROR);
            }
            lambdaUpdate()
                    .eq(Product::getId,productId)
                    // 让数据库自己求和，Java 一行代码搞定！
                    .setSql("stock = (SELECT IFNULL(SUM(stock), 0) FROM product_spec WHERE product_id = " + productId + ")")
                    // 让数据库自己求最低规格价
                    .setSql("price = (SELECT IFNULL(MIN(price), 0) FROM product_spec WHERE product_id = " + productId + ")")
                    .setSql("enterprise_price = (SELECT IFNULL(MIN(enterprise_price), 0) FROM product_spec WHERE product_id = " + productId + ")")
                    .update();
        }

        String productDetailKey = RedisKeyGenerator.productDetail(updateProduct.getId());
        RedisConnector.delete(productDetailKey);
        return Result.success();
    }

    /**
     * 游标结果封装
     * @param queryList 查询列表
     * @param querySize 查询数量
     * @param productSortTypeEnum 商品排序种类枚举
     * @param sortType 排序种类字符串
     * @return 游标结果
     */
    private Result<CursorCommonResult> getCursorCommonResult(List<ProductVO> queryList, Integer querySize, ProductSortTypeEnum productSortTypeEnum, String sortType) {
        boolean isEnd = false;
        if (CollectionUtils.isEmpty(queryList)) {
            CursorCommonResult result = CursorCommonResult.builder()
                    .isEnd(true)
                    .list(Collections.emptyList())
                    .build();
            return Result.success(result);
        }
        if (querySize > queryList.size()) {
            isEnd = true;
        }
        ProductVO productVO = queryList.get(queryList.size() - 1);
        Product endProduct = getById(productVO.getId());
        String sortValueByProduct = ProductSortTypeEnum.getSortValueByProduct(productSortTypeEnum, endProduct);
        CursorCommonEntity cursorCommonEntityResult = CursorCommonEntity.builder()
                .sortType(sortType)
                .querySize(querySize)
                .sortId(endProduct.getId())
                .sortValue(sortValueByProduct)
                .build();
        CursorCommonResult result = CursorCommonResult.builder()
                .isEnd(isEnd)
                .list(queryList)
                .cursorCommonEntity(cursorCommonEntityResult)
                .build();
        return Result.success(result);

    }


}
