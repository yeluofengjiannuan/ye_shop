package com.itxindeshang.infrastructure.mq.consumer.product;

import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.infrastructure.es.service.ProductDocumentService;
import com.itxindeshang.infrastructure.mq.constant.product.MqProductConstant;
import com.itxindeshang.pojo.entity.Product;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;


@RocketMQMessageListener(
        topic = MqProductConstant.PRODUCT_TOPIC,
        consumerGroup = MqProductConstant.PRODUCT_INSERT_CONSUMER_GROUP,
        selectorExpression = MqProductConstant.INSERT
)
@Component
@Slf4j
public class ProductInsertConsumer implements RocketMQListener<Product> {
    @Resource
    private CopyMapper copyMapper;

    @Resource
    private  ProductDocumentService productDocumentService;

    @Override
    public void onMessage(Product product) {
        //查数据库
        if (product == null) {
            log.error("商品同步es查询不到商品 ");
            return;
        }
        ProductDocument productDocument= copyMapper.productToProductDocument(product);
        try {
            productDocumentService.save(productDocument);
        } catch (Exception e) {
            log.warn("商品es同步失败");
            throw new RuntimeException(e);
        }
    }
}
