package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.mapper.ProductImageMapper;
import com.itxindeshang.pojo.entity.ProductImage;
import com.itxindeshang.service.ProductImageService;
import org.springframework.stereotype.Service;

@Service
public class ProductImageServiceImpl extends ServiceImpl<ProductImageMapper, ProductImage> implements ProductImageService {
}
