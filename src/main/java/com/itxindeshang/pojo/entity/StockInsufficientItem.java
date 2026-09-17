package com.itxindeshang.pojo.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockInsufficientItem {

    private Long productId;

    private Long specId;

    private String productName;

    private Integer requiredQuantity;

//    private Integer availableQuantity;

//    private Integer shortage;
}