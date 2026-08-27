package com.itxindeshang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itxindeshang.common.result.CursorCommonEntity;
import com.itxindeshang.common.result.CursorCommonResult;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.ProductDTO;
import com.itxindeshang.pojo.dto.ProductUpdateDTO;
import com.itxindeshang.pojo.entity.Product;
import jakarta.validation.Valid;

public interface ProductService extends IService<Product> {
    Result<Long> addProduct(ProductDTO productDTO);

    Result<CursorCommonResult> getCategoryProductList(@Valid CursorCommonEntity cursorCommonEntity, Long categoryId);

    Result<CursorCommonResult> searchProductList(@Valid CursorCommonEntity cursorCommonEntity, String keyword);

    Result getProductDetail(Long productId);


    Result offShelfProduct(Long productId);

    Result onShelfProduct(Long productId);

    Result updateProduct(ProductUpdateDTO productUpdateDTO);
}
