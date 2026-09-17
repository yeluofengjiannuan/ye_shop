package com.itxindeshang.controller.admin;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.OrderDTO;
import com.itxindeshang.pojo.vo.OrderWithItemVO;
import com.itxindeshang.service.OrderService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public Result<OrderWithItemVO> insertOrder(@RequestBody @Validated OrderDTO orderDTO) {
        return orderService.insertOrder(orderDTO);
    }

    /**
     * 取消订单
     */
    @PutMapping("/cancel")
    public Result<?> cancelOrder(@RequestParam @NotBlank String orderNo, String cancelReason) {
        return orderService.cancelOrder(orderNo, cancelReason);
    }

    /**
     * 支付成功
     */
    @PutMapping("/pay/success")
    public Result<?> paySuccess(@NotBlank String orderNo) {
        return orderService.paySuccess(orderNo);
    }
    /**
     * 查看订单详情
     */
    @GetMapping("/detail")
    public Result<OrderWithItemVO> getOrderDesc(@RequestParam @NotBlank String orderNo) {
        return orderService.getOrderDesc(orderNo);
    }


}
