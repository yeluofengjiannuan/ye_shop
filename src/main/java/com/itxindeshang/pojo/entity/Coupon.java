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
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 活动名称
     */
    @TableField("activity_name")
    private String activityName;

    /**
     * 优惠券模板编号
     */
    @TableField("coupon_no")
    private String couponNo;

    /**
     * 券名称
     */
    @TableField("name")
    private String name;

    /**
     * 类型：1满减 2折扣 3无门槛
     */
    @TableField("type")
    private Integer type;

    /**
     * 满减门槛金额
     */
    @TableField("condition_amount")
    private BigDecimal conditionAmount;

    /**
     * 优惠金额/折扣率
     */
    @TableField("discount_amount")
    private BigDecimal discountAmount;

    /**
     * 折扣券封顶金额
     */
    @TableField("max_discount")
    private BigDecimal maxDiscount;

    /**
     * 总库存
     */
    @TableField("total_qty")
    private Integer totalQty;

    /**
     * 已领取数量
     */
    @TableField("receive_qty")
    private Integer receiveQty;

    /**
     * 使用范围：1全场 2指定商品 3指定分类
     */
    @TableField("use_scope")
    private Integer useScope;

    /**
     * 用户限制类型：1全部 2新人 3会员 4指定人群
     */
    @TableField("user_limit_type")
    private Integer userLimitType;

    /**
     * 每人限领数量
     */
    @TableField("per_user_qty")
    private Integer perUserQty;

    /**
     * 已核销数
     */
    @TableField("used_qty")
    private Integer usedQty;

    /**
     * 领取后有效天数
     */
    @TableField("valid_days")
    private Integer validDays;

    /**
     * 1固定时间 2领券后N天
     */
    @TableField("valid_mode")
    private Integer validMode;

    /**
     * 固定开始时间
     */
    @TableField("valid_start")
    @DateTimeFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    @JsonFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    private LocalDateTime validStart;

    /**
     * 发行时间
     */
    @TableField("release_time")
    @DateTimeFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    @JsonFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    private LocalDateTime releaseTime;

    /**
     * 固定结束时间
     */
    @TableField("valid_end")
    @DateTimeFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    @JsonFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    private LocalDateTime validEnd;

    /**
     * 状态：0下架 1上架
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @DateTimeFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    @JsonFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @DateTimeFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    @JsonFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    private LocalDateTime updateTime;
}