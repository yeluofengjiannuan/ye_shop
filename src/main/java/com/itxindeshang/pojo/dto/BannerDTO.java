package com.itxindeshang.pojo.dto;

import com.itxindeshang.pojo.enums.CommonStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Banner 数据传输对象
 */
@Data
public class BannerDTO {

    /**
     * 标题
     */
    private String title;

    /**
     * 图片 URL
     */
    @NotBlank(message = "轮播图图片地址不能为空")
    private String imageUrl;

    /**
     * 链接 URL
     */
    private String linkUrl;

    /**
     * 排序
     */
    @NotNull(message = "排序值不能为空")
    private Integer sort;

    /**
     * 状态（枚举值：active=启用；inactive=禁用）
     */
    private CommonStatus status;
}