package com.itxindeshang.infrastructure.es.service.impl;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.infrastructure.es.enums.EsIndexEnum;
import com.itxindeshang.infrastructure.es.service.ProductDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;


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
}
