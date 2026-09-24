package com.itxindeshang.infrastructure.es.repository.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.infrastructure.es.enums.EsIndexEnum;
import com.itxindeshang.infrastructure.es.repository.ProductEsRepository;
import com.itxindeshang.pojo.enums.CommonSortTypeEnum;
import com.itxindeshang.pojo.enums.CommonStatus;
import com.itxindeshang.pojo.enums.ProductSortTypeEnum;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductEsRepositoryImpl implements ProductEsRepository {
    private final ElasticsearchClient esClient;

    /**
     * 根据查询种类和商品名 进行首次游标查询(无需开始游标)
     * @param productSortTypeEnum 商品排序格式
     * @param keyword 商品关键词
     * @param limit 查询数
     * @return 商品文档列表
     */
    @Override
    public List<ProductDocument> searchLimitByProductSortTypeAndProductName(ProductSortTypeEnum productSortTypeEnum, String keyword, Integer limit) {
        //校验参数
        if (StringUtils.isBlank(keyword)) {
            return Collections.emptyList();
        }
        String sortField = productSortTypeEnum.getSortField();
        CommonSortTypeEnum commonSortTypeEnum = productSortTypeEnum.getCommonSortTypeEnum();
        SortOrder sortOrder = commonSortTypeEnum.isAsc() ? SortOrder.Asc :SortOrder.Desc;

        //构造请求参数
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(EsIndexEnum.PRODUCT.getIndexName())
                .sort(s -> s.field(f -> f.field(sortField).order(sortOrder)))
                .sort(s -> s.field(f -> f.field(ProductDocument.Fields.id).order(SortOrder.Asc)))
                .query(q -> q.bool(b -> b
                        .must(m -> m.multiMatch(mm -> mm
                                .query(keyword)
                                .fields(ProductDocument.Fields.name + "^3", ProductDocument.Fields.sellPoint)//名字分数权重3倍，这里查sellpoint和name
                                .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                                .fuzziness("AUTO")
                                .tieBreaker(0.3)
                        ))
                        .filter(m -> m.term(t -> t.field(ProductDocument.Fields.status).value(CommonStatus.ACTIVE.getValue())))
                ))
                .size(limit)
                .build();

        try {
            SearchResponse<ProductDocument> searchResponse = esClient.search(searchRequest, ProductDocument.class);
            return searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("es 游标 limit 查询失败",e);
        }
    }

    /**
     * 根据查询种类和商品名 进行游标查询(需要开始游标)
     * @param productSortTypeEnum 商品排序格式
     * @param keyword 商品关键词
     * @param limit 查询数
     * @param sortValue 开始游标值
     * @param productId 开始商品 id
     * @return 商品文档列表
     */
    @Override
    public List<ProductDocument> searchCursorByProductSortTypeAndProductName(ProductSortTypeEnum productSortTypeEnum, String keyword, Integer limit, String sortValue, Long productId) {
        //校验参数
        if (StringUtils.isBlank(keyword)) {
            return Collections.emptyList();
        }
        String sortField = productSortTypeEnum.getSortField();
        CommonSortTypeEnum commonSortTypeEnum = productSortTypeEnum.getCommonSortTypeEnum();
        SortOrder sortOrder = commonSortTypeEnum.isAsc() ? SortOrder.Asc :SortOrder.Desc;

        //构造请求参数
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(EsIndexEnum.PRODUCT.getIndexName())
                .sort(s -> s.field(f -> f.field(sortField).order(sortOrder)))
                .sort(s -> s.field(f -> f.field(ProductDocument.Fields.id).order(SortOrder.Asc)))
                .query(q -> q.bool(b -> b
                        .must(m -> m.multiMatch(mm -> mm
                                .query(keyword)
                                .fields(ProductDocument.Fields.name + "^3", ProductDocument.Fields.sellPoint)
                                .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                                .fuzziness("AUTO")
                                .tieBreaker(0.3)
                        ))
                        .filter(m -> m.term(t -> t.field(ProductDocument.Fields.status).value(CommonStatus.ACTIVE.getValue())))
                ))
                .searchAfter(Arrays.asList(
                        FieldValue.of(sortValue),
                        FieldValue.of(productId)
                ))
                .size(limit)
                .build();

        try {
            SearchResponse<ProductDocument> searchResponse = esClient.search(searchRequest, ProductDocument.class);
            return searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("es 游标 limit 查询失败",e);
        }

    }

    /**
     * 根据字段指定排序 查询指定数量商品文档
     * @param limit 查询数
     * @param fieldName 字段名
     * @param commonSortTypeEnum 排序顺序
     * @return 商品文档列表
     */
    @Override
    public List<ProductDocument> searchLimitOrderByField(Integer limit, String fieldName, CommonSortTypeEnum commonSortTypeEnum) {
        if (StringUtils.isBlank(fieldName) || Objects.isNull(limit) || Objects.isNull(commonSortTypeEnum)) {
            return Collections.emptyList();
        }
        try {
            //获取排序方式
            SortOrder sortOrder = commonSortTypeEnum.isAsc() ? SortOrder.Asc:SortOrder.Desc;
            SearchResponse<ProductDocument> searchResponse = esClient.search(s -> s.query(q -> q.term(
                                    t -> t.field(ProductDocument.Fields.status)
                                            .value(CommonStatus.ACTIVE.getValue())
                            ))
                            .sort(so -> so.field(f -> f.field(fieldName).order(sortOrder)))
                            .size(limit)
                    , ProductDocument.class);
            return searchResponse.hits().hits()
                    .stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("es 字段排序查询失败 ");
        }

    }

    /**
     * 根据商品文档名进行查询
     * @param name 商品文档名
     * @param limit 查询数量
     * @return 查询商品文档列表
     */
    @Override
    public List<ProductDocument> searchByName(String name, int limit) {
        if (StringUtils.isBlank(name)) {
            return List.of();
        }
        try {
            //匹配name以及过滤掉禁用状态的
            SearchResponse<ProductDocument> searchResponse = esClient.search(s -> s
                            .index(EsIndexEnum.PRODUCT.getIndexName())
                            .size(limit)
                            .query(q -> q.bool(b -> b
                                            .must(m -> m
                                                    .match(ma -> ma.field(ProductDocument.Fields.name)
                                                            .query(name)))
                                            .filter(f -> f.term(
                                                    t -> t.field(ProductDocument.Fields.status)
                                                            .value(CommonStatus.ACTIVE.getValue())
                                            ))
                                    )
                            ),
                    ProductDocument.class
            );
            return searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("ES 按名称搜索商品失败", e);
        }
    }

    /**
     * 获取最大商品文档 id
     * @return 最大商品文档 id
     */
    @Override
    public Long getMaxId() {
        try {
            SearchResponse<ProductDocument> searchResponse = esClient.search(s -> s
                            .index(EsIndexEnum.PRODUCT.getIndexName())
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.matchAll(ma -> ma))
                                    .filter(f -> f.term(t -> t
                                            .field(ProductDocument.Fields.status)
                                            .value(CommonStatus.ACTIVE.getValue())
                                    ))
                            ))
                            .sort(sort -> sort.field(f -> f.field(ProductDocument.Fields.id).order(SortOrder.Desc)))
                            .size(1)
                            .source(src -> src.filter(f -> f.includes(ProductDocument.Fields.id)))
                    , ProductDocument.class);
            if (searchResponse.hits().total() != null && searchResponse.hits().total().value() == 0) {
                throw new RuntimeException("es 商品数据数量为 0 ,未初始化数据...");
            }
            ProductDocument maxIdProductDocument = searchResponse.hits().hits().get(0).source();
            if (Objects.isNull(maxIdProductDocument)){
                throw new RuntimeException("es 最大id 商品文档数据异常 ");
            }
            return maxIdProductDocument.getId();
        } catch (IOException e) {
            throw new RuntimeException("查询ES最大ID失败", e);
        }

    }

    /**
     * 查询指定id后的指定数量的商品文档
     * @param limit
     * @param productId
     * @return
     */
    @Override
    public List<ProductDocument> searchLimitAfterId(Integer limit, Long productId) {
        try {
            SearchResponse<ProductDocument> searchResponse = esClient.search(s -> s
                            .index(EsIndexEnum.PRODUCT.getIndexName())
                            .size(limit)
                            .trackTotalHits(t -> t.enabled(false))
                            .query(q -> q.bool(b -> b.must(m -> m.range(r -> r.number(n -> n
                                            .field(ProductDocument.Fields.id).gt(Double.valueOf(productId)))))
                                    .filter(m -> m.term(t -> t.field(ProductDocument.Fields.status).value(CommonStatus.ACTIVE.getValue())))))
                            .sort(sort -> sort.field(f -> f.field(ProductDocument.Fields.id).order(SortOrder.Asc)))
                    , ProductDocument.class);

            return searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("ES商品查询异常", e);
        }

    }


    // 工具方法
    private long parseToMillis(String dateStr) {
        // 根据你实际的日期格式解析
        LocalDateTime ldt = LocalDateTime.parse(dateStr,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        return ldt.atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
    }

}
