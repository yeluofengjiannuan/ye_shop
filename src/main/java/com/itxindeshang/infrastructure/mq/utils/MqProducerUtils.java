package com.itxindeshang.infrastructure.mq.utils;

import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.infrastructure.mq.constant.order.MqOrderConstant;
import com.itxindeshang.infrastructure.mq.constant.product.MqProductConstant;
import com.itxindeshang.pojo.entity.CouponReceiveMessage;
import com.itxindeshang.pojo.entity.Product;
import com.itxindeshang.pojo.entity.ProductSyncMessage;
import com.itxindeshang.pojo.enums.CommonStatus;
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

    public void sendProductInsertData(Product product) {
       rocketMQTemplate.convertAndSend(MqProductConstant.PRODUCT_TOPIC + ":" + MqProductConstant.INSERT, product);
    }

    public void sendProductUpdateStatus(Long productId, CommonStatus status) {
        ProductSyncMessage productSyncMessage = ProductSyncMessage.builder().productId(productId).status(status).build();
        rocketMQTemplate.convertAndSend(MqProductConstant.PRODUCT_TOPIC + ":" + MqProductConstant.UPDATE_STATUS, productSyncMessage);
    }
}