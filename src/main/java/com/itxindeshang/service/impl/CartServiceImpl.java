package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.context.BaseContext;
import com.itxindeshang.infrastructure.redis.connect.RedisConnector;
import com.itxindeshang.infrastructure.redis.connect.StringRedisConnector;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.infrastructure.redis.properties.RedisCacheTtlProperties;
import com.itxindeshang.mapper.CartMapper;
import com.itxindeshang.mapper.ProductMapper;
import com.itxindeshang.pojo.dto.CartProductDTO;
import com.itxindeshang.pojo.entity.Cart;
import com.itxindeshang.service.CartService;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static jodd.util.ThreadUtil.sleep;

@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    @Resource
    private RedisCacheTtlProperties redisCacheTtlProperties;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private CartMapper cartMapper;


    /**
     * 添加商品到购物车
     * @param cartProductDTO
     * @return
     */
    @Override
    public Result<?> addCart(CartProductDTO cartProductDTO) {
        //TODO:异步到库，结算时强制save到库
        String userId = BaseContext.getUserId();
        Long productId = cartProductDTO.getProductId();
        Long specId = cartProductDTO.getSpecId();
        Integer quantity = cartProductDTO.getQuantity();
        String cartKey = RedisKeyGenerator.cartKey(userId);
        String cartHashKey = RedisKeyGenerator.cartHashKey(productId, specId);
        // 1. 第一道防线：无锁极速检查缓存（如果缓存里有，先暂存数量，但不直接返回，因为还要累加）
        Object cacheQuantity = StringRedisConnector.opsForHash().get(cartKey, cartHashKey);
        if (cacheQuantity != null) {
            StringRedisConnector.opsForHash().increment(cartKey, cartHashKey, quantity);
            lambdaUpdate()
                    .eq(Cart::getSpecId, specId)
                    .eq(Cart::getUserId, userId)
                    .setSql("quantity = quantity + " + quantity)
                    .update();
            return Result.success();
        }
        // 2. 第二道防线：缓存未命中（或需要回源校验），使用 Redisson 防击穿
        String lockKey = RedisKeyGenerator.lockCart(userId, specId);
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean isLocked = lock.tryLock(5, -1, TimeUnit.SECONDS);

            if (isLocked) {
                try {
                    // 【双重检查】拿到锁后，再次检查缓存，防止其他线程刚刚把数据写进去
                    Object doubleCheckQuantity = StringRedisConnector.opsForHash().get(cartKey, cartHashKey);
                    if (doubleCheckQuantity != null) {
                        StringRedisConnector.opsForHash().increment(cartKey, cartHashKey, quantity);
                        return Result.success();
                    } else {
                        return handleCacheMissAndSync(userId, productId, specId, quantity, cartKey, cartHashKey);
                    }
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                // 没抢到锁，短暂休眠后重试查缓存
                Thread.sleep(50);
                // 【双重检查】拿到锁后，再次检查缓存，防止其他线程刚刚把数据写进去
                Object doubleCheckQuantity = StringRedisConnector.opsForHash().get(cartKey, cartHashKey);
                if (doubleCheckQuantity != null) {
                    StringRedisConnector.opsForHash().increment(cartKey, cartHashKey,quantity);
                    return Result.success();
                } else {
                    return Result.error(MessageConstant.SYSTEM_BUSY);
                }

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(MessageConstant.LOCK_ERROR, e);
        }
        //异步存库
    }

    /**
     * 处理缓存未命中时的查库与同步逻辑
     * @param userId 用户id
     * @param productId 商品id
     * @param specId 商品规格id
     * @param quantity 购物车加购数量
     */
    private Result<?> handleCacheMissAndSync(String userId, Long productId, Long specId,
                                        Integer quantity, String cartKey, String cartHashKey) {
        Integer productStatus = cartMapper.checkProductStatus(productId, specId);
        if (productStatus == null || productStatus != 1) {
            return Result.error("商品不存在或已下架");
        }
        // 第一次加购时，这里会返回 null，我们把它当作 0 处理，完美兼容！
        Integer dbQuantity = cartMapper.getCartQuentityOnly(userId, productId, specId);
        int currentQuantity = (dbQuantity != null) ? dbQuantity : 0;

        // 将 DB 旧数据与前端增量合并，原子写入 Redis
        StringRedisConnector.opsForHash().put(cartKey, cartHashKey,String.valueOf(currentQuantity + quantity));
        StringRedisConnector.expire(cartKey, redisCacheTtlProperties.getCartTtl(), TimeUnit.SECONDS);
        //TODO: 后续异步存库
        boolean update = lambdaUpdate()
                .eq(Cart::getSpecId, specId)
                .eq(Cart::getUserId, userId)
                .setSql("quantity = quantity + " + quantity)
                .update();
        if (!update) {
            Cart cart = Cart.builder()
                    .userId(Long.valueOf(userId))
                    .specId(specId)
                    .quantity(quantity)
                    .productId(productId)
                    .checked(0)//TODO:常量设置
                    .build();
            save(cart);
        }
        return Result.success();
    }
}
