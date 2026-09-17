package com.itxindeshang.job.delay;

import com.itxindeshang.infrastructure.mq.utils.MqProducerUtils;
import com.itxindeshang.infrastructure.redis.connect.RedisConnector;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.infrastructure.redis.properties.RedisCacheTtlProperties;
import com.itxindeshang.mapper.OrderMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CancelUnpaidOrderDelayJob implements DisposableBean {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RedisCacheTtlProperties redisCacheTtlProperties;

    @Qualifier("cancelUnpaidOrderExecutor")
    @Resource
    private Executor threadPool;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private MqProducerUtils mqProducerUtils;


    private RBlockingQueue<String> blockingQueue;
    
    private RDelayedQueue<String> delayedQueue;

    private static final String BLOCKING_QUEUE_NAME = "cancelUnpaidOrderBlockingQueue";
    private static final String ORDER_CANCEL_REASON = "订单超时未支付，自动取消";

    private Thread consumerThread; // 把线程提出来作为成员变量


    @PostConstruct
    private void init()  {
        RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(BLOCKING_QUEUE_NAME);
        RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        this.blockingQueue = blockingQueue;
        this.delayedQueue = delayedQueue;
        startConsumer();
    }

    public void setUnpaidOrderNoToDelayQueue(String orderNO) {
        log.info("order:{},添加延迟任务成功", orderNO);
        delayedQueue.remove(orderNO);

        long orderTtl = redisCacheTtlProperties.getOrderTtl();
        //订单取消时间
        delayedQueue.offer(orderNO, orderTtl, TimeUnit.SECONDS);
    }

    public void setPaidOrderNoToCancelDelayQueue(String orderNo) {
        delayedQueue.remove(orderNo);
    }


    //TODO：后续再看这个线程池要怎么办
    private void startConsumer() {
        consumerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String orderNo = blockingQueue.take();
                    log.info("从延迟队列取到消息: {}", orderNo);
                    // 调用公共取消方法
                    cancelOrder(orderNo, ORDER_CANCEL_REASON);
                    //发异步消息恢复库存
                } catch (InterruptedException e) {
                    log.warn("处理订单支付超时消费者线程被中断，停止运行");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("处理订单支付超时任务时发生异常: ", e);
                }
            }
        });
        consumerThread.setName("cancelUnpaidOrder");
        consumerThread.setDaemon(true); // 设为守护线程，避免阻止JVM退出
        consumerThread.start(); // ← 关键！启动线程
        log.info("订单超时取消消费者线程已启动");
    }
    /**
     * 公共取消方法：超时取消、用户主动取消都走这里
     */
    public void cancelOrder(String orderNo, String reason) {
        // 1. 先移除延迟队列任务（防止重复触发）
        delayedQueue.remove(orderNo);
        boolean isSuccess = orderMapper.cancelOrderCommon(orderNo, reason);
        if (!isSuccess) {
//            log.warn("订单取消失败，orderNo: {}，reason: {}", orderNo, reason);
            log.info("订单 {} 无需取消（可能已支付或已取消），跳过", orderNo);
            return;
        }
        // 删除 Redis 缓存
        RedisConnector.delete(RedisKeyGenerator.orderKey(orderNo));
        // 发送 MQ 消息，触发库存恢复等后续逻辑
        mqProducerUtils.sendOrderCancelMessage(orderNo);
        log.info("订单取消成功，orderNo: {}, reason: {}", orderNo, reason);
    }

    //实现优雅停机
    @Override
    public void destroy() throws Exception {
        if (consumerThread != null) {
            consumerThread.interrupt();
            try {
                consumerThread.join(5000); // 最多等5秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.info("订单超时取消消费者线程已安全停止");
        }
        // 销毁延迟队列，释放Redis资源
        if (consumerThread != null) {
            consumerThread.interrupt(); // 项目关闭时，主动中断线程
            log.info("订单超时取消消费者线程已安全停止");
        }
    }
}