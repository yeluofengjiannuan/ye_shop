package com.itxindeshang.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CouponReceiveMessage {
    private Long couponId;
    private Long userId;
    private Integer quantity;
}
