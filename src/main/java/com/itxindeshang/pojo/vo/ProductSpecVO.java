package com.itxindeshang.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductSpecVO {
    private Long specId;

    private Long productId;

    private Integer stock;

    private BigDecimal price;

    private String productName;

    private Integer productStatus;



}
