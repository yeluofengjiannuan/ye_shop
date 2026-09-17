package com.itxindeshang.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDTO {

    private Long productId;

    private Long specId;

    private Integer quantity;

    private BigDecimal price;

    private String productName;

    private String productImage;

    private String specText;

}
