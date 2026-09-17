package com.itxindeshang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itxindeshang.pojo.entity.CouponCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CouponCategoryMapper extends BaseMapper<CouponCategory> {
    /**
     *
     * @param couponId 优惠券id
     * @param productIds 订单商品id
     * @return
     */
    List<Long> selectApplicableProductIds(@Param("couponId") Long couponId,
                                          @Param("productIds") List<Long> productIds);
}
