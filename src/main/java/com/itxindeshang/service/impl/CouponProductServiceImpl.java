package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.mapper.CouponProductMapper;
import com.itxindeshang.pojo.entity.CouponProduct;
import com.itxindeshang.service.CouponProductService;
import org.springframework.stereotype.Service;

@Service
public class CouponProductServiceImpl extends ServiceImpl<CouponProductMapper, CouponProduct>   implements CouponProductService {
}
