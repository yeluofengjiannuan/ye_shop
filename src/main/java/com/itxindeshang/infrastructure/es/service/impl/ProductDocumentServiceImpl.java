package com.itxindeshang.infrastructure.es.service.impl;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.infrastructure.es.enums.EsIndexEnum;
import com.itxindeshang.infrastructure.es.repository.ProductEsRepository;
import com.itxindeshang.infrastructure.es.service.ProductDocumentService;
import com.itxindeshang.pojo.enums.CommonSortTypeEnum;
import com.itxindeshang.pojo.enums.CommonStatus;
import com.itxindeshang.pojo.enums.ProductSortTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Component
@RequiredArgsConstructor
@Slf4j
public class ProductDocumentServiceImpl implements ProductDocumentService {


    private final ElasticsearchClient esClient;

    private final ProductEsRepository productEsRepository;

    /**
     * 保存/更新商品到ES（id存在则覆盖，不存在则新增）
     */
    @Override
    public void save(ProductDocument doc) {
        try {
            esClient.index(i -> i
                    .index(EsIndexEnum.PRODUCT.getIndexName())
                    .id(String.valueOf(doc.getId()))
                    .document(doc)
            );
            log.info("商品同步到ES成功, productId={}", doc.getId());
        } catch (IOException e) {
            log.error("商品同步到ES失败, productId={}", doc.getId(), e);
            throw new RuntimeException("ES写入失败", e);
        }
    }

    @Override
    public void updateStatus(Long productId,CommonStatus status) {
        try {
            // 只更新 status 一个字段，其他字段不动
            esClient.update(u -> u
                            .index(EsIndexEnum.PRODUCT.getIndexName())
                            .id(String.valueOf(productId))
                            .doc(Map.of("status", status))  // 局部更新
                            .retryOnConflict(3)             // 版本冲突自动重试3次
                    , Object.class);

            log.info("ES更新商品状态成功, productId={}, status={}", productId,status);
        } catch (IOException e) {
            log.error("ES更新商品状态失败, productId={}", productId, e);
            throw new RuntimeException("ES更新状态失败", e);
        }
    }

    /**
     * 根据商品关键词进行查询
     * @param limit 查询数
     * @param productSortTypeEnum 商品排序枚举
     * @param sortValue 游标开始值
     * @param productId 商品 id
     * @param keyword 关键词
     * @return 查询文档列表
     */
    @Override
    public List<ProductDocument> searchByCursorByName(Integer limit, ProductSortTypeEnum productSortTypeEnum, String sortValue, Long productId, String keyword) {
        //首次进行游标查询
        if (Objects.isNull(sortValue) || Objects.isNull(productId)) {
            return productEsRepository.searchLimitByProductSortTypeAndProductName(productSortTypeEnum, keyword, limit);
        }
        //游标查询
        return productEsRepository.searchCursorByProductSortTypeAndProductName(productSortTypeEnum, keyword, limit, sortValue, productId);

    }

    /**
     * 查询指定数量的热门商品
     * @param limit 查询数量
     * @return 热门商品文档列表
     */
    @Override
    public List<ProductDocument> searchLimitHotProductDocument(Integer limit) {
        //TODO: 第一版先用销售额，后续我的思路是saleCount和viewCount(7天内吧)
        return productEsRepository.searchLimitOrderByField(limit,ProductDocument.Fields.salesCount, CommonSortTypeEnum.DESC);    }

    /**
     * 根据商品名关键词查询
     * @param productNameKeyword 商品名关键词
     * @param limit 查询数量
     * @return
     */
    @Override
    public List<ProductDocument> getProductDocumentByProductNameKeyword(String productNameKeyword, int limit) {
        if (StringUtils.isBlank(productNameKeyword)) {
            return Collections.emptyList();
        }
        return productEsRepository.searchByName(productNameKeyword,limit);
    }

    /**
     * 获取最大商品文档 id
     * @return 最大商品文档 id
     */
    @Override
    public Long getMaxProductDocumentId() {
        return productEsRepository.getMaxId();
    }

    /**
     * 查询指定商品id之后的指定数量的商品文档
     * @param limit 查询数量
     * @param productId 商品 id
     * @return  商品文档列表
     */
    @Override
    public List<ProductDocument> searchLimitAfterProductId(Integer limit, Long productId) {
        return productEsRepository.searchLimitAfterId(limit,productId);
    }
}
