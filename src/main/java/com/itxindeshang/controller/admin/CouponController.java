package com.itxindeshang.controller.admin;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.CouponCreateDTO;
import com.itxindeshang.service.CouponService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CouponController {
    @Resource
    private CouponService couponService;

    @PostMapping("/admin/coupon/release")
    public Result<?> saveCouponAdmin(@RequestBody @Valid @NotNull CouponCreateDTO couponCreateDTO) {
        return couponService.saveCouponAdmin(couponCreateDTO);
    }
}
