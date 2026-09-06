package com.itxindeshang.infrastructure.mq.consumer.coupon;

import com.itxindeshang.pojo.entity.CouponReceiveMessage;
import com.itxindeshang.service.CouponService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RocketMQMessageListener(
        topic = "coupon-receive-topic",
        consumerGroup = "coupon-receive-group"
)
public class CouponConsumer implements RocketMQListener<CouponReceiveMessage> {

    @Resource
    private CouponService couponService;

    @Override
    public void onMessage(CouponReceiveMessage message) {
        couponService.syncSave(message);
    }
}