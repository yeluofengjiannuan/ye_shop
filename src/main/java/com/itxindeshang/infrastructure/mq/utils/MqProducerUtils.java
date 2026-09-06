package com.itxindeshang.infrastructure.mq.utils;

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


    public void sendCouponReceiveMessage(Long couponId, Long userId, Integer quantity) {
        CouponReceiveMessage message = new CouponReceiveMessage(couponId, userId, quantity);
        rocketMQTemplate.convertAndSend(TOPIC, message);
    }

}
