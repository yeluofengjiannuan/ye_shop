package com.itxindeshang.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponUserVO {
    //持有但未
    private Long couponUserId;      // 关联记录ID
    private Integer unusedCount;    // 当前未使用数量（核心展示字段）
    private Integer lockedCount;    // 锁定中数量
    private LocalDateTime expireTime; // 过期时间（前端可能需要做倒计时）
    // --- 来自 Coupon 表（模板维度，冗余进来） ---
    private Long couponId;          // 券模板ID
    private String couponName;      // 券名称（如：满100减20）
    private Integer type;     // 券类型 ：1满减 2折扣 3无门槛
    private BigDecimal conditionAmount; // 使用门槛
    private BigDecimal discountAmount; // 优惠金额/折扣率
}
