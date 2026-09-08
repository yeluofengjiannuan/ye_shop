package com.itxindeshang.infrastructure.mq.consumer.coupon;


import com.itxindeshang.infrastructure.mq.constant.coupon.MqCouponConstant;
import com.itxindeshang.pojo.entity.CouponUser;
import com.itxindeshang.service.CouponService;
import com.itxindeshang.service.CouponUserService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RocketMQMessageListener(topic = MqCouponConstant.TOPIC_COUPON
        , consumerGroup = MqCouponConstant.CONSUMER_GROUP_COUPON_UPDATE_STATUS
        , selectorExpression = MqCouponConstant.TAG_FIXED_TIME )
@RequiredArgsConstructor
public class CouponFixedTimeStatusConsumer implements RocketMQListener<Long> {

    private final CouponUserService couponUserService;

    @Override
    public void onMessage(Long couponId) {
        //FIXME:这里真的不是消息丢失吗
        if (Objects.isNull(couponId)) {
            return;
        }
            couponUserService.lambdaUpdate().set(CouponUser::getUnusedCount,0)
                    .setSql("expired_count = expired_count + unused_count")
                    .eq(CouponUser::getCouponId,couponId)
                    .update();

    }
}

