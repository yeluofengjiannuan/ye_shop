package com.itxindeshang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.OrderDTO;
import com.itxindeshang.pojo.entity.Order;
import com.itxindeshang.pojo.vo.OrderWithItemVO;
import jakarta.validation.constraints.NotBlank;

public interface OrderService extends IService<Order> {
    Result<OrderWithItemVO> insertOrder(OrderDTO orderDTO);


    Result<?> cancelOrder(@NotBlank String orderNo, String cancelReason);

    Result<?> paySuccess(String orderNo);

    Result<OrderWithItemVO> getOrderDesc(@NotBlank String orderNo);
}
