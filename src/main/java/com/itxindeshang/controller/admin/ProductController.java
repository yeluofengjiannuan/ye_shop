package com.itxindeshang.controller.admin;

import com.itxindeshang.common.result.CursorCommonEntity;
import com.itxindeshang.common.result.CursorCommonResult;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.ProductDTO;
import com.itxindeshang.service.ProductService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/product")
public class ProductController {
    @Resource
    private ProductService productService;

    /**
     * 新增商品
     */
    @PostMapping("/add")
    public Result addProduct(@RequestBody @Validated ProductDTO productDTO) {
        return productService.addProduct(productDTO);
    }

    /**
     * 查询分类下所有商品
     */
    @GetMapping("/category/list")
    public Result<CursorCommonResult> getCategoryProductList(@Validated CursorCommonEntity cursorCommonEntity
            , Long categoryId) {
        return productService.getCategoryProductList(cursorCommonEntity, categoryId);
    }
}
