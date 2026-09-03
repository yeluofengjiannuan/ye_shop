package com.itxindeshang.pojo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponCreateDTO {

    @NotBlank(message = "活动名称不能为空")
    private String activityName;

    @NotNull(message = "优惠券类型不能为空")
    private Integer type;

    @NotBlank(message = "券名称不能为空")
    private String name;

    /**
     * 满减门槛金额（满减券必填，无门槛券可不传）
     */
    private BigDecimal conditionAmount;

    /**
     * 优惠金额 / 折扣率
     */
    @NotNull(message = "优惠金额/折扣率不能为空")
    private BigDecimal discountAmount;

    /**
     * 折扣券封顶金额（仅折扣券需要）
     */
    private BigDecimal maxDiscount;

    @NotNull(message = "总发行量不能为空")
    @Min(value = 1, message = "总发行量至少为1")
    private Integer totalQty;

    @NotNull(message = "有效期模式不能为空")
    private Integer validMode;

    /**
     * 固定开始时间（validMode=1时必填）
     */
    private LocalDateTime validStart;

    /**
     * 固定结束时间（validMode=1时必填）
     */
    private LocalDateTime validEnd;

    /**
     * 领取后有效天数（validMode=2时必填）
     */
    private Integer receiveValidDays;

    @NotNull(message = "单人限领张数不能为空")
    @Min(value = 1, message = "单人限领张数至少为1")
    private Integer perUserQty;

    @NotNull(message = "用户限制类型不能为空")
    private Integer userLimitType;

    @NotNull(message = "使用范围不能为空")
    private Integer useScope;

    @NotNull(message = "发行时间不能为空")
    private LocalDateTime releaseTime;
}