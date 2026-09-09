package com.itxindeshang.controller.admin;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.CouponCreateDTO;
import com.itxindeshang.pojo.entity.Coupon;
import com.itxindeshang.pojo.entity.CouponUser;
import com.itxindeshang.pojo.vo.CouponUserVO;
import com.itxindeshang.service.CouponService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CouponController {
    @Resource
    private CouponService couponService;

    /**
     * 管理员分发优惠券
     */
    //TODO:好歹返回id
    @PostMapping("/admin/coupon/release")
    public Result<?> saveCouponAdmin(@RequestBody @Valid @NotNull CouponCreateDTO couponCreateDTO) {
        return couponService.saveCouponAdmin(couponCreateDTO);
    }

    /**
     * 用户领取优惠券
     */
    @PostMapping("/coupon/receive")
    public Result<CouponUser> receiveCoupon(@RequestParam("couponId") Long couponId, @RequestParam("quantity") Integer quantity) {
        return couponService.receiveCoupon(couponId, quantity);
    }

    /**
     * 优惠券上架
     */
    @PutMapping("/admin/coupon/onShelf")
    public Result<?> onShelfCoupon(Long couponId) {
        return couponService.onShlef(couponId);
    }

    /**
     * 优惠券下架
     */
    @PutMapping("/admin/coupon/offShelf")
    public Result<?> offShelfCoupon(Long couponId) {
        return couponService.offShelf(couponId);
    }

    /**
     * 查看用户持有优惠券
     */
    @GetMapping("/coupon/myList")
    public Result<List<CouponUserVO>> showCouponUserList() {
        return couponService.showCouponUserList();
    }

    /**
     * 查看所有优惠券活动
     */
    @GetMapping("/coupon/activity")
    public Result<List<Coupon>> showCouponActivity() {
        return couponService.showCouponActivityList();
    }
}
