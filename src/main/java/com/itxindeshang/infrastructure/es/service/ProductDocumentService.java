package com.itxindeshang.infrastructure.es.service;

import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.pojo.enums.CommonStatus;
import com.itxindeshang.pojo.enums.ProductSortTypeEnum;

import java.util.Arrays;
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

    /**
     * 查询指定数量的热门商品
     * @param limit 查询数量
     * @return 热门商品文档列表
     */
    List<ProductDocument> searchLimitHotProductDocument(Integer limit);

    /**
     * 根据商品名关键词查询
     * @param productNameKeyword 商品名关键词
     * @param limit 查询数量
     * @return
     */
    List<ProductDocument> getProductDocumentByProductNameKeyword(String productNameKeyword, int limit);

    /**
     * 获取最大商品文档 id
     * @return 最大商品文档 id
     */
    Long getMaxProductDocumentId();

    /**
     * 查询指定商品id之后的指定数量的商品文档
     * @param limit 查询数量
     * @param productId 商品 id
     * @return  商品文档列表
     */
    List<ProductDocument> searchLimitAfterProductId(Integer limit, Long productId);
}
