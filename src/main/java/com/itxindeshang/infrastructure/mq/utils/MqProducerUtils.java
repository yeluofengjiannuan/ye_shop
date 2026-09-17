package com.itxindeshang.infrastructure.mq.utils;

import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.infrastructure.mq.constant.order.MqOrderConstant;
import com.itxindeshang.pojo.entity.CouponReceiveMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MqProducerUtils {
    @Autowired
    private RocketMQTemplate rocketMQTemplate;


    private static final String TOPIC = "coupon-receive-topic";

    private static final String ORDER_CANCEL_TOPIC = "order-cancel-topic";


    public void sendCouponReceiveMessage(Long couponId, Long userId, Integer quantity) {
        CouponReceiveMessage message = new CouponReceiveMessage(couponId, userId, quantity);
        rocketMQTemplate.convertAndSend(TOPIC, message);
    }

    public void sendOrderCancelMessage(String orderNo) {
        rocketMQTemplate.convertAndSend(MqOrderConstant.ORDER_TOPIC+":"+MqOrderConstant.CANCEL,orderNo);
    }

    public void sendOrderPaySuccess(String orderNo) {
        rocketMQTemplate.convertAndSend(MqOrderConstant.ORDER_TOPIC+":"+MqOrderConstant.PAY_SUCCESS,orderNo);
    }
}
