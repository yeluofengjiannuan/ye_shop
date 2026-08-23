package com.itxindeshang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itxindeshang.common.result.CursorCommonEntity;
import com.itxindeshang.common.result.CursorCommonResult;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.ProductDTO;
import com.itxindeshang.pojo.entity.Product;
import jakarta.validation.Valid;

public interface ProductService extends IService<Product> {
    Result addProduct(ProductDTO productDTO);

    Result<CursorCommonResult> getCategoryProductList(@Valid CursorCommonEntity cursorCommonEntity, Long categoryId);

    Result<CursorCommonResult> searchProductList(@Valid CursorCommonEntity cursorCommonEntity, String keyword);

    Result getProductDetail(String productId);


    Result offShelfProduct(String productId);

    Result onShelfProduct(String productId);
}
