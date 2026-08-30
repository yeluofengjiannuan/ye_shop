package com.itxindeshang.controller.user;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.CartProductDTO;
import com.itxindeshang.pojo.dto.UpdateCartQuantityDTO;
import com.itxindeshang.pojo.entity.Cart;
import com.itxindeshang.service.CartService;
import io.grpc.internal.ClientStream;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


/**
 * 购物车管理
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

    /**
     * 获取购物车列表
     */
    @GetMapping("/list")
    public Result getCartList() {
        return cartService.getCartList();
    }

    /**
     * 清空购物车
     */
    @DeleteMapping("/clear")
    public Result clearCart() {
        return cartService.clearCart();
    }

    /**
     * 修改购物车商品数量
     */
    @PutMapping("/updateQuantity")
    public Result updateQuantity(UpdateCartQuantityDTO updateCartQuantityDTO) {
        return cartService.updateCartQuantity(updateCartQuantityDTO);
    }
}
