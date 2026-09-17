package com.itxindeshang.infrastructure.mq.consumer.order;

import com.itxindeshang.infrastructure.mq.constant.order.MqOrderConstant;
import com.itxindeshang.mapper.CouponUserMapper;
import com.itxindeshang.mapper.ProductMapper;
import com.itxindeshang.pojo.entity.OrderItem;
import com.itxindeshang.service.OrderItemService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.List;

@RocketMQMessageListener(
        topic = MqOrderConstant.ORDER_TOPIC,
        consumerGroup = MqOrderConstant.ORDER_PAY_SUCCESS_CONSUMER_GROUP,
        selectorExpression = MqOrderConstant.PAY_SUCCESS
)
@Component
@Slf4j
public class OrderPaySuccessConsumer implements RocketMQListener<String> {

    @Resource
    private CouponUserMapper couponUserMapper;

    @Override
    public void onMessage(String orderNo) {
        couponUserMapper.updatePaySuccess(orderNo);
    }
}
