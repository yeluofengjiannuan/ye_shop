package com.itxindeshang.infrastructure.es.repository;

import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.pojo.enums.ProductSortTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

public interface ProductEsRepository {
    /**
     * 根据查询种类和商品名 进行首次游标查询(无需开始游标)
     * @param productSortTypeEnum 商品排序格式
     * @param keyword 商品关键词
     * @param limit 查询数
     * @return 商品文档列表
     */
    List<ProductDocument> searchLimitByProductSortTypeAndProductName(ProductSortTypeEnum productSortTypeEnum, String keyword, Integer limit);

    /**
     * 根据查询种类和商品名 进行游标查询(需要开始游标)
     * @param productSortTypeEnum 商品排序格式
     * @param keyword 商品关键词
     * @param limit 查询数
     * @param sortValue 开始游标值
     * @param productId 开始商品 id
     * @return 商品文档列表
     */
    List<ProductDocument> searchCursorByProductSortTypeAndProductName(ProductSortTypeEnum productSortTypeEnum, String keyword, Integer limit, String sortValue, Long productId);

}
