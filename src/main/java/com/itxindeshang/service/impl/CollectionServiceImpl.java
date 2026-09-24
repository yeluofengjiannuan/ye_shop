package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.context.BaseContext;
import com.itxindeshang.infrastructure.redis.connect.RedisConnector;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.mapper.CollectionMapper;
import com.itxindeshang.pojo.entity.ProductCollection;
import com.itxindeshang.service.CollectionService;
import org.springframework.stereotype.Service;

@Service
public class CollectionServiceImpl  extends ServiceImpl<CollectionMapper, ProductCollection> implements CollectionService {
    /**
     * 用户收藏商品
     * @param productId
     * @return
     */
    @Override
    public Result<?> addCollection(Long productId) {
        String collectionKey = RedisKeyGenerator.productCollection(productId);
        String userId = BaseContext.getUserId();
        ProductCollection productCollection = ProductCollection.builder().productId(productId).userId(Long.valueOf(userId)).build();
        boolean isSuccess = save(productCollection);
        if (!isSuccess) {
            return Result.error(MessageConstant.TOM_CAT_ERROR);
        }
        RedisConnector.delete(collectionKey);
        return Result.success();
    }
}
