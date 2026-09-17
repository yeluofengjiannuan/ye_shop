package com.itxindeshang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itxindeshang.pojo.entity.CouponCategory;

import java.util.Arrays;
import java.util.List;

public interface CouponCategoryService extends IService<CouponCategory> {
    List<Long> selectApplicableProductIds(Long couponId, List<Long> productIds);
}
