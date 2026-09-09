package com.itxindeshang.job.delay;

import com.itxindeshang.infrastructure.mq.constant.coupon.MqCouponConstant;
import com.itxindeshang.infrastructure.redis.connect.RedisConnector;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.pojo.entity.Coupon;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class CouponUseStatusChangeListener {

    // 注入自定义的线程池，专门用于跑这几个轮询任务，避免阻塞主业务线程
    @Qualifier("couponStatusChangeListenerExecutor")
    @Resource
    private Executor threadPool;

    // Redisson 客户端，用于获取分布式锁，保证集群环境下只有一个节点在执行轮询
    @Resource
    private RedissonClient redissonClient;


    @Resource
    private RocketMQTemplate rocketMQTemplate;


    private volatile boolean running = true;


    /**
     * 项目启动自动开启监听线程
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startListen() {
        //TODO：写个线程允许查看存在的活动但未开始领取的，开启时间就让其可以领取
        threadPool.execute(() -> loopListen(RedisKeyGenerator.couponFixedTimeUnBeginZSet()));
        threadPool.execute(() -> loopListen(RedisKeyGenerator.couponFixedTimeInProgressZSet()));
        threadPool.execute(() -> loopListen(RedisKeyGenerator.couponAfterReceiveTimeInProgressZSet()));
        threadPool.execute(()->loopListen(RedisKeyGenerator.couponActivityUnBeginZSet()));
        running = true;
        log.info("优惠券过期监听任务 running = {}", running);
    }

    /**
     * 核心阻塞轮询
     */
    private void loopListen(String zSetKey) {
        while (running) {
            RLock lock = null;
            try {
                // 1. 获取分布式锁，保证集群环境下同一个 ZSet 只有一个线程在跑
                // tryLock(0, 5, ...) 表示尝试获取锁，不等待，锁的自动释放时间为 5 秒
                lock = redissonClient.getLock(getLockKey(zSetKey));
                boolean lockOk = lock.tryLock(0, 5, TimeUnit.SECONDS);
                if (!lockOk) {
                    // 没抢到锁，说明其他节点在处理，休眠 1 秒后继续尝试
                    sleep(1000);
                    continue;
                }

                long now = System.currentTimeMillis();
                //查询最早一条已过期任务
                Set<Object> taskSet = RedisConnector.opsForZSet()
                        .rangeByScore(zSetKey, 0, now, 0, 1);
                if (Objects.isNull(taskSet) || taskSet.isEmpty()) {
                    sleep(50);//好像是线程罪魁祸首，我的mq来不及处理数据的元凶
//                    sleep(1000);
                    continue;
                }
                String taskId = taskSet.toArray()[0].toString();
                // 4. 执行具体的业务过期逻辑（移动 Redis 状态 + 发送 MQ 消息）
                doExpireBiz(taskId, zSetKey);
                // 5. 处理完成后，从 ZSet 中移除该任务
                RedisConnector.opsForZSet().remove(zSetKey, taskId);

            } catch (InterruptedException e) {
                log.warn("监听线程被中断，即将退出");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("延迟任务监听异常", e);
                // 发生异常休眠 200ms，避免死循环疯狂报错
                sleep(200);
            } finally {
                unlock(lock);
            }
        }
    }

    /**
     * 业务过期逻辑
     * 移动优惠券缓存的id set
     * 使用 rocketMQ 通知
     */
    private void doExpireBiz(String taskId, String zSetKey) {
        log.info("执行过期任务 taskId:{}", taskId);
        Long couponId = Long.valueOf(taskId);
        Long couponUserId = Long.valueOf(taskId);
        //固定时间过期优惠券  未开始->未用
        if (zSetKey.equals(RedisKeyGenerator.couponFixedTimeUnBeginZSet())) {
            String couponDetailListKey = RedisKeyGenerator.couponDetail(couponId);
            Coupon coupon = RedisConnector.getHashObject(couponDetailListKey, Coupon.class);
            if (coupon == null) {
                String key = RedisKeyGenerator.couponFixedTimeUnBeginZSet();
                RedisConnector.opsForZSet().remove(key,couponId);
                return;
            }
            String key = RedisKeyGenerator.couponFixedTimeUnBeginZSet();
            RedisConnector.opsForZSet().remove(key,couponId);
            //这时候优惠券允许使用
            String zsetKey =RedisKeyGenerator.couponFixedTimeInProgressZSet();
            long endTimestamp = coupon.getValidEnd().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            RedisConnector.opsForZSet().add(zsetKey, couponId, endTimestamp);
            return;
        }

        //固定时间优惠券 未用->已过期
        if (zSetKey.equals(RedisKeyGenerator.couponFixedTimeInProgressZSet())) {
            //删除，处理库
            String key = RedisKeyGenerator.couponFixedTimeInProgressZSet();
            RedisConnector.opsForZSet().remove(key,couponId);
            //重建活动列表
            RedisConnector.delete(RedisKeyGenerator.couponActivity());
            //删除detail
            RedisConnector.delete(RedisKeyGenerator.couponDetail(couponId));
            //删除receiveQty
            RedisConnector.delete(RedisKeyGenerator.couponReceiveQtyKey(couponId));
            //删除received
            RedisConnector.delete(RedisKeyGenerator.couponReceivedKey(couponId));
            //删除相关pendingKey
            RedisConnector.delete(RedisKeyGenerator.couponPendingKey(couponId));
            RedisConnector.delete(RedisKeyGenerator.couponUnablePendingKey(couponId));
            rocketMQTemplate.convertAndSend(MqCouponConstant.TOPIC_COUPON+":"+MqCouponConstant.TAG_FIXED_TIME,couponId);
            return;
        }
        // 领券后 N 天优惠券 未用->过期
        if (zSetKey.equals(RedisKeyGenerator.couponAfterReceiveTimeInProgressZSet())) {
            zSetKey = RedisKeyGenerator.couponAfterReceiveTimeInProgressZSet();
            RedisConnector.opsForZSet().remove(zSetKey,couponUserId);
            rocketMQTemplate.convertAndSend(MqCouponConstant.TOPIC_COUPON+":"+MqCouponConstant.TAG_AFTER_RECEIVE,couponUserId);
            return;
        }
        //活动未发放->活动上线
        //FIXME：这里的key名称可以修改为zSet后缀的
        if (zSetKey.equals(RedisKeyGenerator.couponActivityUnBeginZSet())) {
            //拿到coupon类型
            String couponDetailListKey = RedisKeyGenerator.couponDetail(couponId);
            Coupon coupon = RedisConnector.getHashObject(couponDetailListKey, Coupon.class);
            if (coupon == null) {
                String key = RedisKeyGenerator.couponFixedTimeUnBeginZSet();
                //FIXME：我也不知道什么情况会==null
                RedisConnector.opsForZSet().remove(key,couponId);
                return;
            }
            //上线活动预热,清空couponActivity后续查询会自己填充的
            String couponActivity = RedisKeyGenerator.couponActivity();
            RedisConnector.delete(couponActivity);
            //清除zSet存的couponId
            String couponActivityUnBeginZSetKey = RedisKeyGenerator.couponActivityUnBeginZSet();
            RedisConnector.opsForZSet().remove(couponActivityUnBeginZSetKey, couponId);
            //预热缓存库存
            String stockKey = RedisKeyGenerator.couponStockKey(couponId);
            RedisConnector.opsForValue().set(stockKey, coupon.getTotalQty());
        }
    }

    /**
     * 服务关闭
     */
    @PreDestroy
    public void stopListen() {
        running = false;
        log.info("Redis 延迟任务监听已停止");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * @param key 传入 ZSet 的 key
     * @return 锁对象字符串
     */
    private String getLockKey(String key) {
        return "lock:" + key;
    }

}
