package com.itxindeshang.infrastructure.mq.consumer.coupon;

import com.itxindeshang.infrastructure.mq.constant.coupon.MqCouponConstant;
import com.itxindeshang.infrastructure.redis.connect.RedisConnector;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.pojo.entity.CouponUser;
import com.itxindeshang.service.CouponUserService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;


@Component
@RocketMQMessageListener(consumerGroup = MqCouponConstant.CONSUMER_GROUP_COUPON_UPDATE_STATUS
        , topic = MqCouponConstant.TOPIC_COUPON
        , selectorExpression = MqCouponConstant.TAG_AFTER_RECEIVE
        , consumeMode = ConsumeMode.CONCURRENTLY)
@RequiredArgsConstructor
public class CouponAfterReceiveStatusConsumer implements RocketMQListener<Long> {

    private final CouponUserService couponUserService;
    /**
     * 批量消费 N 天后优惠券 更改状态
     */
    @Override
    public void onMessage(Long couponUserId) {
        couponUserService.lambdaUpdate()
                .set(CouponUser:: getUnusedCount,0)
                .setSql("expired_count = expired_count + unused_count")
                .eq(CouponUser::getId,couponUserId)
                .update();
    }
}
