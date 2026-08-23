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
import java.time.LocalDateTime;

/**
 * 商品规格表
 * 对应数据表：product_spec
 *
 */
@Data
@TableName(value = "product_image")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 收藏ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联商品ID（外键）
     */
    @TableField(value = "product_id")
    private Long productId;

    /**
     * 商品图片URL列表
     */
    @TableField(value = "image_url")
    private String imageUrl;

    /**
     * 排序
     */
    @TableField(value = "sort")
    private Integer sort;

    /**
     * 收藏时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @DateTimeFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    @JsonFormat(pattern = DatePatternConstants.DATE_TIME_FORM)
    private LocalDateTime createTime;
}
