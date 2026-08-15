package com.itxindeshang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.ProductDTO;
import com.itxindeshang.pojo.entity.Product;

public interface ProductService extends IService<Product> {
    Result addProduct(ProductDTO productDTO);
}
