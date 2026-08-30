package com.itxindeshang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.CartProductDTO;
import com.itxindeshang.pojo.entity.Cart;

public interface CartService extends IService<Cart> {
    Result<?> addCart(CartProductDTO cartProductDTO);

    Result getCartList();

    Result clearCart();
}
