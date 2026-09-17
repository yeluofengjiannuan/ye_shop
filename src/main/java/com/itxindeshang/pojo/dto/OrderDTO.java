package com.itxindeshang.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderDTO {

    private Long couponUserId;

    private Long addressId;

    private String remark;

    private BigDecimal freight;

    private BigDecimal totalAmount;

    private List<OrderItemDTO> orderItems;
}
