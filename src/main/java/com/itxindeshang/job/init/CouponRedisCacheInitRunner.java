package com.itxindeshang.job.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itxindeshang.infrastructure.redis.connect.StringRedisConnector;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.mapper.CouponMapper;
import com.itxindeshang.pojo.entity.Coupon;
import com.itxindeshang.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 初始化优惠卷缓存
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CouponRedisCacheInitRunner implements ApplicationRunner {

    private final CouponService couponService;

    private final CouponMapper couponMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("初始化 优惠券 redis缓存...");
        couponService.updateCouponRedisCache();
//        warmUp();
        log.info("初始化 优惠券 redis缓存成功...");
    }

     // 活动发布时预热库存到Redis TODO：这个应该也是维护一个zset定时任务
   /* public void warmUp() {
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
    }*/
}
