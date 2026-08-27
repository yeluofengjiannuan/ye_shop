package com.itxindeshang.controller.user;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.CartProductDTO;
import com.itxindeshang.pojo.entity.Cart;
import com.itxindeshang.service.CartService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 *  购物车管理
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {
    @Resource
    private CartService cartService;

    /**
     * 添加商品到购物车
     */
    @PostMapping("/add")
    public Result<?> addCart(@RequestBody CartProductDTO cartProductDTO) {
        return cartService.addCart(cartProductDTO);
    }
}
