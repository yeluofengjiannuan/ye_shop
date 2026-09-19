package com.itxindeshang.infrastructure.mq.consumer.product;

import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.infrastructure.es.service.ProductDocumentService;
import com.itxindeshang.infrastructure.mq.constant.product.MqProductConstant;
import com.itxindeshang.pojo.entity.Product;
import com.itxindeshang.pojo.entity.ProductSyncMessage;
import com.itxindeshang.pojo.enums.CommonStatus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;


@RocketMQMessageListener(
        topic = MqProductConstant.PRODUCT_TOPIC,
        consumerGroup = MqProductConstant.PRODUCT_UPDATE_STATUS_CONSUMER_GROUP,
        selectorExpression = MqProductConstant.UPDATE_STATUS
)
@Component
@Slf4j
public class ProductUpdateStatusConsumer implements RocketMQListener<ProductSyncMessage> {

    @Resource
    private  ProductDocumentService productDocumentService;

    @Override
    public void onMessage(ProductSyncMessage message) {
        //查数据库
        if (message == null) {
            log.error("商品状态更新参数丢失");
            return;
        }
        try {
            Long productId = message.getProductId();
            CommonStatus status = message.getStatus();
            productDocumentService.updateStatus(productId,status);
        } catch (Exception e) {
            log.warn("商品es同步失败");
            throw new RuntimeException(e);
        }
    }
}
