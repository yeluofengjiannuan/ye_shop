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
    // 工具方法
    private long parseToMillis(String dateStr) {
        // 根据你实际的日期格式解析
        LocalDateTime ldt = LocalDateTime.parse(dateStr,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        return ldt.atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
    }

}
