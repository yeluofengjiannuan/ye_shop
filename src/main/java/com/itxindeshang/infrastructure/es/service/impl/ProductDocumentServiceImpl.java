package com.itxindeshang.infrastructure.es.service.impl;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.infrastructure.es.enums.EsIndexEnum;
import com.itxindeshang.infrastructure.es.service.ProductDocumentService;
import com.itxindeshang.pojo.enums.CommonStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
public class ProductDocumentServiceImpl implements ProductDocumentService {


    private final ElasticsearchClient esClient;

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
}
