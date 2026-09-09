package com.itxindeshang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.CouponCreateDTO;
import com.itxindeshang.pojo.entity.Coupon;
import com.itxindeshang.pojo.entity.CouponReceiveMessage;
import com.itxindeshang.pojo.entity.CouponUser;
import com.itxindeshang.pojo.vo.CouponUserVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface CouponService extends IService<Coupon> {
    Result<Long> saveCouponAdmin(@Valid @NotNull CouponCreateDTO couponCreateDTO);

    Result<CouponUser> receiveCoupon(Long couponId,Integer quantity);

    void syncSave(CouponReceiveMessage message);

    void updateCouponRedisCache();

    Result<?> onShlef(Long couponId);

    Result<?> offShelf(Long couponId);

    Result<List<Coupon>> showCouponActivityList();

    Result<List<CouponUserVO>> showCouponUserList();
}
