package com.itxindeshang.infrastructure.es.repository;

import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.pojo.enums.CommonSortTypeEnum;
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

    /**
     * 根据字段指定排序 查询指定数量商品文档
     * @param limit 查询数
     * @param fieldName 字段名
     * @param commonSortTypeEnum 排序顺序
     * @return 商品文档列表
     */
    List<ProductDocument> searchLimitOrderByField(Integer limit, String fieldName, CommonSortTypeEnum commonSortTypeEnum);

    /**
     * 根据商品文档名进行查询
     * @param name 商品文档名
     * @param limit 查询数量
     * @return 查询商品文档列表
     */
    List<ProductDocument> searchByName(String name, int limit);

    /**
     * 获取最大商品文档 id
     * @return 最大商品文档 id
     */
    Long getMaxId();

    /**
     * 查询指定id后的指定数量的商品文档
     * @param limit
     * @param productId
     * @return
     */
    List<ProductDocument> searchLimitAfterId(Integer limit, Long productId);

}
