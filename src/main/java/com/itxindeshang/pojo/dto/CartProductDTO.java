package com.itxindeshang.pojo.dto;


import lombok.Data;

@Data
public class CartProductDTO {
    /**
     * 商品 id
     */
    private Long productId;
    /**
     * 规格 id
     */
    private Long specId;
    /**
     * 数量
     */
    private Integer quantity;


}
