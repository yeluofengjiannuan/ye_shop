package com.itxindeshang.pojo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCartQuantityDTO {
    @NotNull(message = "购物车ID不能为空")
    private Long cartId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最少为1")
    private Integer quantity;
}