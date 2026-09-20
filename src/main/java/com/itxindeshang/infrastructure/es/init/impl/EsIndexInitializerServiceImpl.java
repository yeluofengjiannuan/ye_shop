package com.itxindeshang.infrastructure.es.init.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.util.ObjectBuilder;
import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.infrastructure.es.enums.EsIndexEnum;
import com.itxindeshang.infrastructure.es.init.EsIndexInitializerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.function.Function;

@RequiredArgsConstructor
@Slf4j
@Component
public class EsIndexInitializerServiceImpl implements EsIndexInitializerService {
    private final ElasticsearchClient elasticsearchClient;

    @Override
    public void initProductIndex() {
        String indexName = EsIndexEnum.PRODUCT.getIndexName();
        String pipelineName = "product_index_update_time";


        try {
                elasticsearchClient.ingest().putPipeline(p -> p
                        .id(pipelineName)
                        .description("Auto set updateTime on every write/update for product index")
                        .processors(proc -> proc
                                .date(date -> date
                                        .field("_ingest.timestamp")
                                        .targetField("updateTime")
                                        .formats("ISO8601")
                                        .outputFormat("yyyy-MM-dd'T'HH:mm:ss")
                                        .timezone("Asia/Shanghai")
                                )
                        )
                );
                log.info("ES Pipeline [{}] 创建成功", pipelineName);

            boolean exists = elasticsearchClient.indices()
                    .exists(e -> e.index(indexName))
                    .value();
            if (exists) {
                log.info("ES索引[{}]已存在，跳过初始化", indexName);
                return;
            }
            Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>> request = builder ->
                    builder.index(indexName)
                            .settings(IndexSettings.of(s -> s
                                    .numberOfShards("1")
                                    .numberOfReplicas("0")
                                    .defaultPipeline(pipelineName)
                            ))
                            .mappings(m -> m
                                    .properties(ProductDocument.Fields.id, p -> p.long_(l -> l))
                                    .properties(ProductDocument.Fields.categoryId, p -> p.long_(l -> l))
                                    .properties(ProductDocument.Fields.name, p -> p.text(t -> t
                                            .analyzer("ik_max_word")
                                            .searchAnalyzer("ik_smart")
                                            .fields("keyword", k -> k.keyword(kw -> kw.ignoreAbove(256)))
                                    ))
                                    .properties(ProductDocument.Fields.image, p -> p.keyword(k -> k.index(false)))
                                    .properties(ProductDocument.Fields.sellPoint, p -> p.text(t -> t
                                            .analyzer("ik_max_word")
                                            .searchAnalyzer("ik_smart")
                                    ))
                                    .properties(ProductDocument.Fields.price, p -> p.scaledFloat(f -> f.scalingFactor(100.0)))
                                    // 商品状态：枚举 字符串：
                                    .properties(ProductDocument.Fields.status, p -> p.keyword(k -> k))
                                    // 浏览量：排序、筛选
                                    .properties(ProductDocument.Fields.viewCount, p -> p.long_(l -> l))
                                    // 销量：排序、筛选
                                    .properties(ProductDocument.Fields.salesCount, p -> p.long_(l -> l))
                                    .properties(ProductDocument.Fields.createTime, p -> p.date(d -> d.format("date_optional_time||yyyy-MM-dd HH:mm:ss||epoch_millis")))
                                    .properties(ProductDocument.Fields.updateTime, p -> p.date(d -> d.format("date_optional_time||yyyy-MM-dd HH:mm:ss||epoch_millis")))
                            );
            elasticsearchClient.indices().create(request);
            log.info("ES索引[{}]初始化成功", indexName);
        } catch (IOException e) {
            log.error("ES索引[{}]初始化失败", indexName, e);
            throw new RuntimeException("商品 ES文档初始化异常", e);
        }
    }
}
