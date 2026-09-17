package com.itxindeshang.service;

import com.itxindeshang.pojo.dto.OrderDTO;
import com.itxindeshang.pojo.entity.Address;
import com.itxindeshang.pojo.entity.Coupon;
import com.itxindeshang.pojo.entity.CouponUser;
import com.itxindeshang.pojo.entity.Order;
import com.itxindeshang.pojo.vo.ProductSpecVO;
import lombok.Data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
public class OrderContext {
    private OrderDTO orderDTO;
    private Address address;
    private CouponUser couponUser;
    private Coupon coupon;
    private Map<Long, ProductSpecVO> specMap;
    private Order order; // 最终生成的订单实体
    /**
     * 符合优惠券使用范围的商品ID集合
     * 用于后续优惠计算和分摊
     */
    private Set<Long> applicableProductIds = new HashSet<>();


    private boolean success = true;
    private String msg;

    public void setFail(String msg) {
        this.success = false;
        this.msg = msg;
    }
}
