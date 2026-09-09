package com.itxindeshang.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum CouponStatusEnum {
    DRAFT(0,"草稿"),        // 刚创建，未上架
    ON_SHELF(1, "已上架"),   // 可被用户领取
    OFF_SHELF(2, "已下架"),  // 主动下架，不可再领
    EXPIRED(3, "已过期"),    // 时间到期自动失效
    VOIDED(4, "已作废");     // 管理员强制作废（用户持有的也作废）

    @EnumValue
    private final Integer code;


    private final String desc;

    CouponStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CouponStatusEnum getByNumber(Integer number) {
        for (CouponStatusEnum status : values()) {
            if (status.getCode().equals(number)) {
                return status;
            }
        }
        return null;
    }
}
