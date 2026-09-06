package com.itxindeshang.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.itxindeshang.common.constant.DatePatternConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户持有优惠券表
 */
@Data
@TableName("coupon_user")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 券模板ID
     */
    @TableField("coupon_id")
    private Long couponId;

    /**
     * 总领取数量
     */
    @TableField("quantity")
    private Integer quantity;

    /**
     * 未使用数量
     */
    @TableField("unused_count")
    private Integer unusedCount;

    /**
     * 已锁定数量（下单时锁定，未支付）
     */
    @TableField("locked_count")
    private Integer lockedCount;

    /**
     * 已使用数量
     */
    @TableField("used_count")
    private Integer usedCount;

    /**
     * 已过期数量
     */
    @TableField("expired_count")
    private Integer expiredCount;

    /**
     * 已作废数量（运营手动作废）
     */
    @TableField("invalidated_count")
    private Integer invalidatedCount;

    /**
     * 已退款数量
     */
    @TableField("refunded_count")
    private Integer refundedCount;

    /**
     * 过期时间
     */
    @DateTimeFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    @JsonFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @DateTimeFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    @JsonFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @DateTimeFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    @JsonFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    private LocalDateTime updateTime;
}