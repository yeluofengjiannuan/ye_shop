package com.itxindeshang.controller.admin;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.ProductDTO;
import com.itxindeshang.service.ProductService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/product")
public class ProductController {
    @Resource
    private ProductService productService;

    @PostMapping("/add")
    public Result addProduct(@RequestBody @Validated ProductDTO productDTO) {
        return productService.addProduct(productDTO);
    }
}
