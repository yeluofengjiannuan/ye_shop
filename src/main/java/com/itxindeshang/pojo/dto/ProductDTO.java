package com.itxindeshang.pojo.dto;

import com.itxindeshang.pojo.entity.ProductSpec;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDTO {
    @NotNull(message = "商品分类不能为空")
    private Long categoryId;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String sellPoint;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    private BigDecimal price;

    private BigDecimal enterprisePrice;

    private Long stock;

    @NotBlank(message = "商品封面图不能为空")
    private String image;

    // 富文本详情可能很大，视情况而定
    private String description;

    private List<String> imageUrls;

    // 规格列表（如果有专门的规格DTO，这里就用 List<ProductSpecAddDTO>）
    private List<ProductSpec> specList;
}
