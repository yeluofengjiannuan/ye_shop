package com.itxindeshang.infrastructure.mq.consumer.order;

import com.itxindeshang.infrastructure.mq.constant.coupon.MqCouponConstant;
import com.itxindeshang.infrastructure.mq.constant.order.MqOrderConstant;
import com.itxindeshang.mapper.CouponUserMapper;
import com.itxindeshang.mapper.OrderItemMapper;
import com.itxindeshang.mapper.OrderMapper;
import com.itxindeshang.mapper.ProductMapper;
import com.itxindeshang.pojo.entity.OrderItem;
import com.itxindeshang.service.OrderItemService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.List;

@RocketMQMessageListener(
        topic = MqOrderConstant.ORDER_TOPIC,
        consumerGroup = MqOrderConstant.ORDER_CANCEL_CONSUMER_GROUP,
        selectorExpression = MqOrderConstant.CANCEL
)
@Component
@Slf4j
public class OrderCancelConsumer implements RocketMQListener<String> {

    @Resource
    private OrderItemService orderItemService;
    @Resource
    private ProductMapper productMapper;

    @Resource
    private CouponUserMapper couponUserMapper;

    @Override
    public void onMessage(String orderNo) {
        //TODO:抽出去加个事务，我真的要气笑了，一直加库存
        log.info("收到订单取消消息，开始恢复库存，orderNo: {}", orderNo);

        // 1. 查询订单下的所有商品
        List<OrderItem> items = orderItemService.lambdaQuery().eq(OrderItem::getOrderNo,orderNo).list();
        if (items == null || items.isEmpty()) {
            log.warn("订单商品为空，orderNo: {}", orderNo);
            return;
        }

        // 2. 逐个恢复库存
        for (OrderItem item : items) {
            productMapper.restoreProductAndSpecStock(
                    item.getProductId(),
                    item.getSpecId(),
                    item.getQuantity()
            );
        }
        //3.恢复coupon
            int count = couponUserMapper.updateCancel(orderNo);

        log.info("库存恢复完成，orderNo: {}", orderNo);
    }
}
