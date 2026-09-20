package com.itxindeshang.infrastructure.es.service;

import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.pojo.enums.CommonStatus;
import com.itxindeshang.pojo.enums.ProductSortTypeEnum;

import java.util.List;

public interface ProductDocumentService {

    void save(ProductDocument doc);

    void updateStatus(Long productId, CommonStatus status);

    /**
     * 根据商品关键词进行查询
     * @param limit 查询数
     * @param productSortTypeEnum 商品排序枚举
     * @param sortValue 游标开始值
     * @param productId 商品 id
     * @param keyword 关键词
     * @return 查询文档列表
     */
    List<ProductDocument> searchByCursorByName(Integer limit, ProductSortTypeEnum productSortTypeEnum ,String sortValue, Long productId, String keyword);
}
