package com.itxindeshang.pojo.dto;

import com.itxindeshang.pojo.entity.ProductImage;
import com.itxindeshang.pojo.entity.ProductSpec;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductUpdateDTO {
    private Long id;

    private String name;

    private String sellPoint;

    private BigDecimal enterprisePrice;

    private Long stock;

    private String image;

    private String description;

    private List<ProductImage> imageUrls;

    private List<ProductSpec> specList;
}
