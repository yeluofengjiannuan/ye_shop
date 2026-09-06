package com.itxindeshang.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.mapper.CouponMapper;
import com.itxindeshang.pojo.entity.Coupon;
import com.itxindeshang.pojo.entity.CouponReceiveMessage;
import com.itxindeshang.pojo.entity.CouponUser;
import com.itxindeshang.service.CouponService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CouponConfig {

    @Resource
    private CouponMapper couponMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Bean
    /**
     * 活动发布时预热库存到Redis TODO：这个应该也是维护一个zset定时任务
     */
    public void warmUp() {
        List<Coupon> coupons = couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>().eq(Coupon::getStatus,1)
                        .gt(Coupon::getValidEnd, LocalDateTime.now())
        );
        coupons.forEach(coupon -> {
            Long couponId = coupon.getId();
            String stockKey = RedisKeyGenerator.couponStockKey(couponId);
            String receivedKey = RedisKeyGenerator.couponReceivedKey(couponId);
            String receiveQtyKey =RedisKeyGenerator.couponReceiveQtyKey(couponId);

            Boolean absent = stringRedisTemplate.opsForValue()
                    .setIfAbsent(stockKey, String.valueOf(coupon.getTotalQty()));

            if (Boolean.TRUE.equals(absent)) {
                long ttl = calculateTtl(coupon);
                stringRedisTemplate.opsForValue().set(receiveQtyKey, String.valueOf(coupon.getReceiveQty()));
                stringRedisTemplate.expire(stockKey, ttl, TimeUnit.SECONDS);
                stringRedisTemplate.expire(receivedKey, ttl, TimeUnit.SECONDS);
                log.info("预热优惠券库存成功, couponId={}, stock={}", couponId, coupon.getTotalQty());
            }
        });
        }
    // =============================================
    // 计算优惠券剩余有效时间（秒）
    // =============================================
    private long calculateTtl(Coupon coupon) {
        long endTimestamp = coupon.getValidEnd().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long now = System.currentTimeMillis();
        return Math.max((endTimestamp - now) / 1000, 0);
    }

}
