package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.core.assist.ISqlRunner;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.context.BaseContext;
import com.itxindeshang.infrastructure.redis.connect.RedisConnector;
import com.itxindeshang.infrastructure.redis.connect.StringRedisConnector;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.infrastructure.redis.properties.RedisCacheTtlProperties;
import com.itxindeshang.mapper.CartMapper;
import com.itxindeshang.mapper.ProductMapper;
import com.itxindeshang.pojo.dto.CartProductDTO;
import com.itxindeshang.pojo.dto.CartProductSpecDTO;
import com.itxindeshang.pojo.dto.UpdateCartQuantityDTO;
import com.itxindeshang.pojo.entity.Cart;
import com.itxindeshang.pojo.entity.CartItem;
import com.itxindeshang.pojo.entity.Product;
import com.itxindeshang.pojo.entity.ProductSpec;
import com.itxindeshang.service.CartService;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static jodd.util.ThreadUtil.sleep;

@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    //TODO：购物车库存问题看后续是全在order模块处理吗

    @Resource
    private RedisCacheTtlProperties redisCacheTtlProperties;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private CartMapper cartMapper;

    @Resource
    private CopyMapper copyMapper;


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

    /**
     * 获取购物车列表
     * @return
     */
    @Override
    public Result getCartList() {
        //cartItem是展示的，联表查两个库，但还要查product判断是否上架
        String userId = BaseContext.getUserId();
        String cartKey = RedisKeyGenerator.cartKey(userId);

        List<Cart> dbCartList = lambdaQuery()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreateTime) // 按加购时间倒序
                .list();

        if (CollectionUtils.isEmpty(dbCartList)) {
            return Result.success(Collections.emptyList());
        }
        // ========== 第二步：批量联表查出商品详情 ==========
        Set<Long> specIds = dbCartList.stream()
                .map(Cart::getSpecId)
                .collect(Collectors.toSet());
        List<CartProductSpecDTO> specsWithProduct = cartMapper.selectSpecsWithProduct(specIds);
        Map<Long, CartProductSpecDTO> detailMap = specsWithProduct.stream()
                .collect(Collectors.toMap(CartProductSpecDTO::getSpecId, Function.identity()));
        // ========== 第三步：极速读取 Redis ==========
        Map<String, String> redisCartMap = StringRedisConnector.opsForHash().entries(cartKey);
        // ========== 第四步：内存拼装与状态判定 ==========
        //TODO：这里redis没有数据库有的就可以去回填，性能是没差多少就是了
        List<CartItem> cartItems = dbCartList.stream()
                // 【核心逻辑】：只保留在 detailMap 中能找到的商品（查不到的直接剔除）
                .filter(cart -> detailMap.containsKey(cart.getSpecId()))
                .map(cart -> {
                    CartProductSpecDTO detail = detailMap.get(cart.getSpecId());
                    // 1. 处理 Redis 数量降级
                    String hashKey = RedisKeyGenerator.cartHashKey(cart.getProductId(), cart.getSpecId());
                    Object redisQuantity = redisCartMap.get(hashKey);
                    int finalQuantity = (redisQuantity != null)
                            ? Integer.parseInt(redisQuantity.toString())
                            : cart.getQuantity();
                    // 2. 使用 MapStruct 组装基础数据
                    CartItem item = copyMapper.toCartItem(cart,detail);
                    item.setQuantity(finalQuantity); // 覆盖为最新数量
                    return item;
                })
                .collect(Collectors.toList());
        return Result.success(cartItems);
    }

    /**
     * 清空购物车
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result clearCart() {
        //TODO:后期看看有没有并发风险
        String userId = BaseContext.getUserId();
        String cartKey = RedisKeyGenerator.cartKey(userId);
        StringRedisConnector.delete(cartKey);
        boolean isRemoved = lambdaUpdate()
                .eq(Cart::getUserId, userId)
                .remove();
        if (!isRemoved) {
            throw new RuntimeException("清空购物车数据库操作失败，事务已回滚");
        }
        return Result.success();
    }
    /**
     * 修改购物车商品数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateCartQuantity(UpdateCartQuantityDTO updateCartQuantityDTO) {
        String userId = BaseContext.getUserId();
        Long cartId = updateCartQuantityDTO.getCartId();
        Integer newQuantity = updateCartQuantityDTO.getQuantity();
        // 1. 查库校验（防止修改不存在的商品，或校验库存）
        Cart cart = lambdaQuery()
                .eq(Cart::getId, cartId)
                .eq(Cart::getUserId, userId) // 【核心安全】：防止越权修改别人的购物车
                .one();
        if (cart == null) {
            return Result.error(MessageConstant.DATA_ERROR);
        }
        boolean updated = lambdaUpdate()
                .eq(Cart::getId, cartId)
                .set(Cart::getQuantity, newQuantity)
                .update();
        if (!updated) {
            throw new RuntimeException("修改购物车数量失败，事务已回滚");
        }
        String cartKey = RedisKeyGenerator.cartKey(userId);
        String cartHashKey = RedisKeyGenerator.cartHashKey(cart.getProductId(), cart.getSpecId());

        StringRedisConnector.opsForHash().put(cartKey, cartHashKey, String.valueOf(newQuantity));

        return Result.success();
    }
}
