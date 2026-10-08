package com.itxindeshang.infrastructure.mq.consumer.product;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.infrastructure.es.service.ProductDocumentService;
import com.itxindeshang.infrastructure.mq.constant.product.MqProductConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.List;


@RocketMQMessageListener(
        topic = MqProductConstant.PRODUCT_TOPIC,
        consumerGroup = MqProductConstant.SYNC_SAVE_CONSUMER_GROUP,
        selectorExpression = MqProductConstant.SYNC_SAVE
)
@Component
@Slf4j
public class ProductSyncSaveESConsumer implements RocketMQListener<List<ProductDocument>> {

    @Resource
    private  ProductDocumentService productDocumentService;

    @Override
    public void onMessage(List<ProductDocument> productDocumentList) {
        if (CollectionUtils.isEmpty(productDocumentList)){
            return;
        }
        productDocumentService.batchSaveProductDocument(productDocumentList);
    }
}
