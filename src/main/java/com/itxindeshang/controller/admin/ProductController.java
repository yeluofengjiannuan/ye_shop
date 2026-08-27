package com.itxindeshang.controller.admin;

import com.itxindeshang.common.result.CursorCommonEntity;
import com.itxindeshang.common.result.CursorCommonResult;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.ProductDTO;
import com.itxindeshang.pojo.dto.ProductUpdateDTO;
import com.itxindeshang.service.ProductService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
public class ProductController {
    //TODO：序列器配置好了，后续把id的接收改回Long；
    @Resource
    private ProductService productService;

    /**
     * 新增商品
     */
    @PostMapping("/add")
    public Result<Long> addProduct(@RequestBody @Validated ProductDTO productDTO) {
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

    /**
     * 关键词查询商品
     */
    @GetMapping("/search")
    public Result<CursorCommonResult> searchProductList(@Validated CursorCommonEntity cursorCommonEntity, String keyword) {
        return productService.searchProductList(cursorCommonEntity, keyword);
    }

    /**
     * 获取商品详情
     */
    @GetMapping("/detail")
    public Result getProductDetail(Long productId) {
        return productService.getProductDetail(productId);
    }

    /**
     * 下架商品
     */
    @PutMapping("/offShelf")
    public Result offShelfProduct(String productId) {
        return productService.offShelfProduct(productId);
    }

    /**
     * 上架商品
     */
    @PutMapping("/onShelf")
    public Result onShelfProduct(String productId) {
        return productService.onShelfProduct(productId);
    }

    /**
     * 更新商品
     */
    @PutMapping("/update")
    public Result<?> updateProduct(@RequestBody  @Validated ProductUpdateDTO productUpdateDTO) {
        return productService.updateProduct(productUpdateDTO);

    }
}
