package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.mapper.CouponCategoryMapper;
import com.itxindeshang.pojo.entity.CouponCategory;
import com.itxindeshang.service.CouponCategoryService;
import org.springframework.stereotype.Service;

@Service
public class CouponCategoryServiceImpl extends ServiceImpl<CouponCategoryMapper, CouponCategory> implements CouponCategoryService {
}
