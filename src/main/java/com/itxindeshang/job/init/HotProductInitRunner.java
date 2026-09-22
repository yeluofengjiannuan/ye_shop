package com.itxindeshang.job.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itxindeshang.common.constant.BucketConstant;
import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.infrastructure.es.service.ProductDocumentService;
import com.itxindeshang.infrastructure.redis.config.RedisBucketTtlProperties;
import com.itxindeshang.infrastructure.redis.connect.RedisConnector;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.infrastructure.redis.properties.RedisCacheCountProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * 初始化热门商品缓存
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HotProductInitRunner implements ApplicationRunner {

    private final ProductDocumentService productDocumentService;

    private final RedisCacheCountProperties redisCacheCountProperties;

    private final RedissonClient redissonClient;

    private static final String hotProductKey = RedisKeyGenerator.hotProductKey();

    private static final String hotProductIdListKey =RedisKeyGenerator.idListKey();

    private  final RedisBucketTtlProperties redisBucketTtlProperties;

    //从 es 初始化热门商品缓存到 redis
    @Override
//    @SuppressWarnings("unchecked")
    public void run(ApplicationArguments args) throws Exception {
        log.info("初始化热门商品缓存... 初始化数量{}", redisCacheCountProperties.getHotProductCacheSize());
        //es查询
        List<ProductDocument> productDocuments = productDocumentService.searchLimitHotProductDocument(redisCacheCountProperties.getHotProductCacheSize());
        ArrayList<Long> hotProductIdList = new ArrayList<>(productDocuments.size());
        productDocuments.forEach(doc -> hotProductIdList.add(doc.getId()));
        //批量写入
        RedisConnector.executePipelined(new SessionCallback<>() {
            @Override
            public <K, V> Object execute(@Nullable RedisOperations<K, V> operations) throws DataAccessException {
                if (Objects.isNull(operations)){
                    log.error("热门商品缓存初始化失败...");
                    return null;
                }
                for (ProductDocument productDocument : productDocuments) {
                    Long hotProductId = productDocument.getId();
                    String hashKey = RedisKeyGenerator.documentIdKey(hotProductId);
                    operations.opsForHash().put((K) hotProductKey, hashKey, productDocument);
                }
                operations.opsForValue().set((K) hotProductIdListKey, (V) hotProductIdList);
                return null;
            }
        });
        log.info("初始化热门商品 redis缓存完成...");
        log.info("初始化热门商品idList redis缓存完成...");
        log.info("开始初始化热门商品 bucket...");
        RBucket<BucketConstant.BucketSign> hotProductBucket = redissonClient.getBucket(RedisKeyGenerator.updateBucketKey(hotProductKey));
        RBucket<BucketConstant.BucketSign> hotIdListProductBucket = redissonClient.getBucket(RedisKeyGenerator.updateBucketKey(hotProductIdListKey));
        hotProductBucket.set(new BucketConstant.BucketSign(BucketConstant.BucketThreadType.READ_THREAD,null)
                        , Duration.ofSeconds(redisBucketTtlProperties.getHotProductReadBucketTtl())
        );
        hotIdListProductBucket.set(new BucketConstant.BucketSign(BucketConstant.BucketThreadType.READ_THREAD,null)
                        , Duration.ofSeconds(redisBucketTtlProperties.getHotProductReadBucketTtl())
        );
        log.info("初始化热门商品 bucket完成...");
    }
}
