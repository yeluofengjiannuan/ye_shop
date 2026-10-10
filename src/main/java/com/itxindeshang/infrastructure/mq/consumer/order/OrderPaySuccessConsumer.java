package com.itxindeshang.infrastructure.mq.consumer.order;

import com.itxindeshang.infrastructure.mq.constant.order.MqOrderConstant;
import com.itxindeshang.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;


@RocketMQMessageListener(
        topic = MqOrderConstant.ORDER_TOPIC,
        consumerGroup = MqOrderConstant.ORDER_PAY_SUCCESS_CONSUMER_GROUP,
        selectorExpression = MqOrderConstant.PAY_SUCCESS
)
@Component
@Slf4j
public class OrderPaySuccessConsumer implements RocketMQListener<String> {



    @Resource
    private OrderService orderService;

    @Override
    public void onMessage(String orderNo) {
        orderService.syncPaySuccess(orderNo);

    }
}
