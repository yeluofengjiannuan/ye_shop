package com.itxindeshang.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;


@Data
public class CartProductSpecDTO {

    /**
     * 规格ID
     */
    private Long specId;
    /**
     * 商品ID
     */
    private Long productId;
    /**
     * 商品名称
     */
    private String productName;
    /**
     * 商品封面图 URL
     */
    private String productImage;

    /**
     * 规格单价
     */
    private BigDecimal price;

    /**
     * 规格库存
     */
    private Integer stock;

    /**
     * 规格描述
     */
    private String specText;

}
