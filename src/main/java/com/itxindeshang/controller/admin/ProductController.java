package com.itxindeshang.controller.admin;

import com.itxindeshang.common.result.CursorCommonEntity;
import com.itxindeshang.common.result.CursorCommonResult;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.ProductDTO;
import com.itxindeshang.pojo.dto.ProductUpdateDTO;
import com.itxindeshang.pojo.entity.ProductSearchKeyword;
import com.itxindeshang.pojo.vo.SimpleProductVO;
import com.itxindeshang.service.ProductSearchKeywordService;
import com.itxindeshang.service.ProductService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {
    @Resource
    private ProductService productService;

    @Resource
    private ProductSearchKeywordService productSearchKeywordService;

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
    public Result offShelfProduct(Long productId) {
        return productService.offShelfProduct(productId);
    }

    /**
     * 上架商品
     */
    @PutMapping("/onShelf")
    public Result onShelfProduct(Long productId) {
        return productService.onShelfProduct(productId);
    }

    /**
     * 更新商品
     */
    @PutMapping("/update")
    public Result<?> updateProduct(@RequestBody  @Validated ProductUpdateDTO productUpdateDTO) {
        return productService.updateProduct(productUpdateDTO);
    }

    /**
     * 获取热门商品
     */
    @GetMapping("/hot")
    public Result<List<SimpleProductVO>> getHotProduct(@RequestParam(name = "limit", defaultValue = "10") Integer limit){
        return productService.getHotProduct(limit);
    }

    /**
     * 查询商品规格价格
     * @param productId 商品id
     * @param specId 规格id
     * @return
     */
    @GetMapping("/spec/price")
    public Result<?> getProductSpecPrice( Long productId,Long specId){
        return productService.getProductSpecPrice(productId,specId);
    }

    /**
     * 用户获取热门搜索关键词列表
     */
    @GetMapping("/user/keyword/list")
    public Result<List<String>> getProductSearchKeywordListUser() {
        return productSearchKeywordService.getProductSearchKeywordListUser();
    }

    /**
     * 管理员获取搜索关键词列表
     */
    @GetMapping("/admin/keyword/list")
    public Result<List<ProductSearchKeyword>> getProductSearchKeywordListAdmin() {
        return productSearchKeywordService.getProductSearchKeywordListAdmin();
    }
}
