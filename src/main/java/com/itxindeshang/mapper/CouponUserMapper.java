package com.itxindeshang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itxindeshang.pojo.entity.Coupon;
import com.itxindeshang.pojo.entity.CouponUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CouponUserMapper extends BaseMapper<CouponUser> {

    /*@Select("SELECT c.* from coupon c left join coupon_user cu on cu.coupon_id = c.id")
    Coupon getcoupon(Long couponUserId);*/
}
