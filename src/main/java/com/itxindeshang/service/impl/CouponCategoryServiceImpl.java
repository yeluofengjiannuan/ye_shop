package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.mapper.CouponCategoryMapper;
import com.itxindeshang.pojo.entity.CouponCategory;
import com.itxindeshang.service.CouponCategoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CouponCategoryServiceImpl extends ServiceImpl<CouponCategoryMapper, CouponCategory> implements CouponCategoryService {
    @Resource
    private CouponCategoryMapper couponCategoryMapper;

    /**
     * 查询分类优惠券可以匹配的订单商品ids集合
     * @param couponId 优惠券id
     * @param productIds 订单商品id
     * @return
     */
    @Override
    public List<Long> selectApplicableProductIds(Long couponId, List<Long> productIds) {
        return couponCategoryMapper.selectApplicableProductIds(couponId,productIds);
    }
}
