package com.itxindeshang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itxindeshang.common.constant.MessageConstant;
import com.itxindeshang.common.exception.CouponException;
import com.itxindeshang.common.generator.SnowflakeIdGenerator;
import com.itxindeshang.common.mapstruct.CopyMapper;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.context.BaseContext;
import com.itxindeshang.infrastructure.mq.utils.MqProducerUtils;
import com.itxindeshang.infrastructure.redis.connect.RedisConnector;
import com.itxindeshang.infrastructure.redis.generator.RedisKeyGenerator;
import com.itxindeshang.mapper.CouponMapper;
import com.itxindeshang.mapper.CouponUserMapper;
import com.itxindeshang.pojo.UserInfo;
import com.itxindeshang.pojo.dto.CouponCreateDTO;
import com.itxindeshang.pojo.entity.Coupon;
import com.itxindeshang.pojo.entity.CouponReceiveMessage;
import com.itxindeshang.pojo.entity.CouponUser;
import com.itxindeshang.pojo.enums.CouponStatusEnum;
import com.itxindeshang.pojo.enums.CouponValidModeEnum;
import com.itxindeshang.pojo.vo.CouponUserVO;
import com.itxindeshang.service.CouponService;
import com.itxindeshang.service.CouponUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {
    //TODO:管理员主动分发给指定用户4的方法，增加user实体类的level字段
    //TODO：这里就是领取后n天过期的券是随时能领的，只有管理员主动下架，后续再优化这里的逻辑
    //TODO:pending和unablePending要处理
    @Resource
    private CopyMapper copyMapper;

    @Resource
    private CouponMapper couponMapper;

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private CouponUserMapper couponUserMapper;

    @Resource
    private MqProducerUtils mqProducerUtils;

    @Resource
    private CouponUserService couponUserService;

    /**
     * 管理员分发优惠券
     */
    @Override
    public Result<Long> saveCouponAdmin(CouponCreateDTO couponCreateDTO) {

        // 1. 条件校验
        validateCouponCreateDTO(couponCreateDTO);

        // 2. DTO → Entity 转换
        Coupon coupon = copyMapper.couponCreateDTOToCoupon(couponCreateDTO);

        // 3. 生成优惠券编号
        coupon.setCouponNo(snowflakeIdGenerator.generateCouponNo());

        // 4. 填充初始化数量字段
        coupon.setReceiveQty(0);
        coupon.setUsedQty(0);
        coupon.setStatus(CouponStatusEnum.DRAFT);

        // 5. 无门槛券强制门槛为0
        if (couponCreateDTO.getType() == 3) {
            coupon.setConditionAmount(BigDecimal.ZERO);
        }
        // 6. 入库
        couponMapper.insert(coupon);
        Long couponId = coupon.getId();

        return Result.success(couponId);
    }

    /**
     * 条件校验
     */
    private void validateCouponCreateDTO(CouponCreateDTO dto) {
        // 1. 分发时间非空校验
        if (dto.getReleaseTime() == null) {
            throw new CouponException(MessageConstant.RELEASE_TIME_REQUIRED);
        }
        // 2. 分发时间不能早于当前时间（防止发“过期”的券）
        if (dto.getReleaseTime().isBefore(LocalDateTime.now())) {
            throw new CouponException(MessageConstant.RELEASE_TIME_BEFORE_NOW);
        }
        // validMode = 1（固定时间）：validStart 和 validEnd 必填
        if (dto.getValidMode() == 1) {
            if (dto.getValidStart() == null || dto.getValidEnd() == null) {
                throw new CouponException(MessageConstant.VALID_MODE_FIXED_TIME_REQUIRED);
            }
            if (dto.getValidEnd().isBefore(dto.getValidStart())) {
                throw new CouponException(MessageConstant.VALID_END_BEFORE_START);
            }
        }

        // validMode = 2（领券后N天）：receiveValidDays 必填
        if (dto.getValidMode() == 2) {
            if (dto.getValidDays() == null || dto.getValidDays() <= 0) {
                throw new CouponException(MessageConstant.VALID_MODE_RECEIVE_DAYS_REQUIRED);
            }
        }

        // type = 1（满减）：conditionAmount 必填且大于0
        if (dto.getType() == 1) {
            if (dto.getConditionAmount() == null || dto.getConditionAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CouponException(MessageConstant.FULL_REDUCTION_CONDITION_REQUIRED);
            }
        }

        // type = 2（折扣）：discountAmount 应在 0~1 之间
        if (dto.getType() == 2) {
            if (dto.getDiscountAmount() == null

                    || dto.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0
                    || dto.getDiscountAmount().compareTo(BigDecimal.ONE) >= 0) {
                throw new CouponException(MessageConstant.DISCOUNT_RATE_INVALID);
            }
        }

        // type = 3（无门槛）：discountAmount 必须大于0
        if (dto.getType() == 3) {
            if (dto.getDiscountAmount() == null || dto.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CouponException(MessageConstant.NO_THRESHOLD_DISCOUNT_REQUIRED);
            }
        }
    }

    /**
     * 这里往下就是用户领取的逻辑，key修改lua hash槽{}确保集群情况下lua可以正常使用
     */
    private static final DefaultRedisScript<Long> COUPON_LUA_SCRIPT;

    static {
        COUPON_LUA_SCRIPT = new DefaultRedisScript<>();
        COUPON_LUA_SCRIPT.setLocation(new ClassPathResource("lua/coupon.lua"));
        COUPON_LUA_SCRIPT.setResultType(Long.class);
    }

    /**
     * 用户领取优惠券
     * @param quantity 前端拿到coupon的perUserQty字段
     * @param couponId 优惠券Id
     */
    @Override
    public Result<CouponUser> receiveCoupon(Long couponId, Integer quantity) {
        String userId = BaseContext.getUserId();
        UserInfo userInfo = BaseContext.getUserInfo();
        // 1. 查询券模板
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            return Result.error(MessageConstant.COUPON_NOT_FOUND);
        }
        //判断权限
        if (coupon.getUserLimitType() != 1) {
            if (!userInfo.getLevel().equals(coupon.getUserLimitType())) {
                return Result.error(MessageConstant.USER_LEVEL_ERROR);
            }
        }
        if (!quantity.equals(coupon.getPerUserQty())) {
            return Result.error(MessageConstant.COUPON_USER_RECEIVE_ERROR);
        }
        if (coupon.getStatus() != CouponStatusEnum.ON_SHELF) {
            return Result.error(MessageConstant.COUPON_NO_SHELF);
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidStart() != null && now.isBefore(coupon.getValidStart())) {
            return Result.error(MessageConstant.ACTIVITY_NOT_START);
        }
        if (coupon.getValidEnd() != null && now.isAfter(coupon.getValidEnd())) {
            return Result.error(MessageConstant.ACTIVITY_END);
        }
        Integer perUserQty = coupon.getPerUserQty();
        if (perUserQty == null || perUserQty <= 0) {
            return Result.error(MessageConstant.COUPON_ERROR);
        }
        // 2. 构建 Redis Key
        String stockKey = RedisKeyGenerator.couponStockKey(couponId);
        String receivedKey = RedisKeyGenerator.couponReceivedKey(couponId);
        String receiveQtyKey = RedisKeyGenerator.couponReceiveQtyKey(couponId);

        Long luaResult = null;
        try {
            luaResult = stringRedisTemplate.execute(
                    COUPON_LUA_SCRIPT,
                    Arrays.asList(stockKey, receivedKey, receiveQtyKey),
                    userId,
                    String.valueOf(perUserQty)
            );
        } catch (RedisSystemException e) {
            log.warn("Redis不可用，降级到DB领取, couponId={},userId={}", couponId, userId, e);
            return deductByDb(coupon, Long.valueOf(userId));
        } catch (Exception e) {
            log.error("Redis领券异常，降级到DB领取, couponId={}, userId={}", couponId, userId, e);
            return deductByDb(coupon, Long.valueOf(userId));
        }
        //增加 null 判断，防止拆箱 NPE
        if (luaResult == null) {
            log.error("Redis Lua脚本返回null，降级到DB领取, couponId={}, userId={}", couponId, userId);
            return deductByDb(coupon, Long.valueOf(userId));
        }
        //出现返回值是null是redis挂了，要怎么考虑吗，会抛异常吧？
        if (luaResult == -1L) {
            return Result.error(MessageConstant.DATA_ERROR);
        }

        if (luaResult == 0L) {
            return Result.error(MessageConstant.COUPON_STOCK_NULL);
        }

        if (luaResult == -2L) {
            return Result.error(MessageConstant.COUPON_HOLD);
        }

        // 3. Redis扣减成功，异步落库

        //这里redis消费者
        String pendingKey = RedisKeyGenerator.couponPendingKey(couponId);
        stringRedisTemplate.opsForValue().increment(pendingKey, 1);
        LocalDateTime expireTime;
        if (coupon.getValidMode() == 1) {
            expireTime = coupon.getValidEnd();
        } else {
            expireTime = LocalDateTime.now().plusDays(coupon.getValidDays());
        }
        try {
            mqProducerUtils.sendCouponReceiveMessage(couponId, Long.valueOf(userId), perUserQty);
        } catch (Exception e) {
            log.error("发送领券MQ消息失败, couponId={}, userId={}", couponId, userId, e);
            rollbackRedis(couponId, Long.valueOf(userId), perUserQty);
            return Result.error(MessageConstant.SYSTEM_BUSY);
        }
        // 4. 返回处理中的领券记录
        CouponUser couponUser = CouponUser.builder()
                .userId(Long.valueOf(userId))
                .couponId(couponId)
                .quantity(perUserQty)
                .unusedCount(perUserQty)
                .lockedCount(0)
                .usedCount(0)
                .expiredCount(0)
                .invalidatedCount(0)
                .refundedCount(0)
                .expireTime(expireTime)
                .createTime(LocalDateTime.now())
                .build();

        return Result.success(couponUser);
    }

    /**
     * 消费用户领取优惠券异步存库
     * @param message 消息体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncSave(CouponReceiveMessage message) {

        Long couponId = message.getCouponId();
        Long userId = message.getUserId();
        Integer quantity = message.getQuantity();

        // redis是否已经执行 pendingKey -1
        boolean redisOperated = false;
        // 是否需要补偿redis：只有发生异常要重试的时候才补偿
        boolean needCompensateRedis = false;
        String pendingKey = RedisKeyGenerator.couponPendingKey(couponId);

        try {
            // 1. DB 累加已领取数量0
            boolean isSuccess = lambdaUpdate()
                    .setSql("receive_qty = receive_qty + " + quantity)
                    .eq(Coupon::getId, couponId)
                    .update();
            if (!isSuccess) {
                throw new CouponException(MessageConstant.SYNC_SAVE_ERROR);
            }
            Coupon coupon = getById(couponId);
            LocalDateTime expireTime;
            if (coupon.getValidMode() == 1) {
                expireTime = coupon.getValidEnd();
            } else {
                expireTime = LocalDateTime.now().plusDays(coupon.getValidDays());
            }
            CouponUser couponUser = CouponUser.builder()
                    .userId(userId)
                    .couponId(couponId)
                    .quantity(quantity)
                    .unusedCount(quantity)
                    .lockedCount(0)
                    .usedCount(0)
                    .expiredCount(0)
                    .invalidatedCount(0)
                    .refundedCount(0)
                    .expireTime(expireTime) //补全过期时间
                    .build();
            // 2. 插入用户领取记录（唯一索引兜底幂等）
            try {
                couponUserMapper.insert(couponUser);
            } catch (DuplicateKeyException e) {
                log.warn("用户已领取过, couponId={}, userId={}, 跳过插入", couponId, userId);
            }
            //过期加入redis维护
            // 3. Redis pendingKey -1
            if (coupon.getValidMode().equals( CouponValidModeEnum.AFTER_RECEIVE.getCode())) {
                updateCouponUserRedisCache(couponUser);
            }
            stringRedisTemplate.opsForValue().increment(pendingKey, -1);
            redisOperated =true;
            // 全部执行成功，消费正常结束，needCompensateRedis保持false，finally不补偿
        } catch (Exception ex) {
            log.error("领券消费异常 couponId={},userId={}", couponId, userId, ex);
            // 标记：发生异常，要重试MQ，如果redis已经减过，需要补偿
            needCompensateRedis = true;
            // 抛出异常！触发Spring事务回滚DB，同时RocketMQ消息重试 RECONSUME_LATER
            throw ex;
        } finally {
            // 条件：发生异常需要重试 并且 redis已经执行过-1 →补偿 +1
            if (needCompensateRedis && redisOperated) {
                try {
                    stringRedisTemplate.opsForValue().increment(pendingKey, 1);
                    log.info("补偿redis pendingKey完成, key={}, quantity={}", pendingKey, 1);
                } catch (Exception compensateEx) {
                    stringRedisTemplate.opsForValue().increment(RedisKeyGenerator.couponUnablePendingKey(couponId), 1);
                }
            }
        }
    }

    /**
     * Redis不可用时，降级到数据库领取
     */
    private Result<CouponUser> deductByDb(Coupon coupon, Long userId) {

        Long couponId = coupon.getId();
        Integer perUserQty = coupon.getPerUserQty();

        String lockKey = RedisKeyGenerator.couponLock(couponId);
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                return Result.error(MessageConstant.SYSTEM_BUSY);
            }

            Long count = couponUserMapper.selectCount(
                    new LambdaQueryWrapper<CouponUser>()
                            .eq(CouponUser::getUserId, userId)
                            .eq(CouponUser::getCouponId, couponId)
            );

            if (count != null && count > 0) {
                return Result.error(MessageConstant.COUPON_HOLD);
            }
            //更新coupon表的stock
            int rows = couponMapper.deductStock(couponId, perUserQty);
            if (rows == 0) {
                return Result.error(MessageConstant.COUPON_STOCK_NULL);
            }

            CouponUser couponUser = CouponUser.builder()
                    .userId(userId)
                    .couponId(couponId)
                    .quantity(perUserQty)
                    .unusedCount(perUserQty)
                    .lockedCount(0)
                    .usedCount(0)
                    .expiredCount(0)
                    .invalidatedCount(0)
                    .refundedCount(0)
                    .expireTime(coupon.getValidEnd())
                    .createTime(LocalDateTime.now())
                    .build();

            couponUserMapper.insert(couponUser);

            return Result.success(couponUser);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("DB降级领取被中断, couponId={}, userId={}", couponId, userId, e);
            return Result.error(MessageConstant.SYSTEM_BUSY);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * MQ发送失败时回滚Redis
     */
    private void rollbackRedis(Long couponId, Long userId, Integer perUserQty) {
        String pendingKey = RedisKeyGenerator.couponPendingKey(couponId);
        try {
            String receivedKey = RedisKeyGenerator.couponReceivedKey(couponId);
            stringRedisTemplate.opsForValue().increment(receivedKey, perUserQty);
            stringRedisTemplate.opsForSet().remove(receivedKey, String.valueOf(userId));
            stringRedisTemplate.opsForValue().increment(pendingKey, -1);
        } catch (Exception e) {
            log.error("Redis回滚失败, couponId={}, userId={}", couponId, userId, e);
        }
    }

    // =============================================
    // 定时恢复对账
    // =============================================
    @Scheduled(fixedDelay = 30000)
    public void checkRedisAndRecover() {
        try {
            //探测是否存活
            stringRedisTemplate.opsForValue().get("health:check");
        } catch (Exception e) {
            log.warn("Redis不可用，跳过恢复");
            return;
        }

        List<Coupon> activeCoupons = couponMapper.selectActiveCoupons();
        activeCoupons.stream().filter(coupon -> coupon.getValidMode() == 2 || coupon.getValidEnd().isAfter(LocalDateTime.now()));
        for (Coupon coupon : activeCoupons) {
            try {
                recoverSingleCoupon(coupon.getId());
            } catch (Exception e) {
                log.error("恢复优惠券失败, couponId={}", coupon.getId(), e);
            }
        }
    }

    // =============================================
    // 恢复单个优惠券（对账修正）
    // =============================================
    private void recoverSingleCoupon(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            return;
        }
        String pendingKey = RedisKeyGenerator.couponPendingKey(couponId);
        String stockKey = RedisKeyGenerator.couponStockKey(couponId);
        String receivedKey = RedisKeyGenerator.couponReceivedKey(couponId);
        String receiveQtyKey = RedisKeyGenerator.couponReceiveQtyKey(couponId);

        // ===== 第一步：DB 已落库的 receive_qty =====
        int dbReceiveQty = coupon.getReceiveQty();

        // ===== 第二步：MQ 未消费的量（pending流水中 PENDING 状态的总量）=====
        String pendingStr = stringRedisTemplate.opsForValue().get(pendingKey);
        int mqQty = pendingStr != null ? Integer.parseInt(pendingStr) * coupon.getPerUserQty() : 0;

        // 3. 获取 Redis 中异常的坏账数量 (unablePendingKey)
        String unableStr = stringRedisTemplate.opsForValue().get(RedisKeyGenerator.couponUnablePendingKey(couponId));
        int unableQty = unableStr != null ? Integer.parseInt(unableStr) * coupon.getPerUserQty() : 0;

        // ===== 第三步：计算期望库存 =====
        int expectedReceiveQty = dbReceiveQty + mqQty + unableQty;

        // ===== 第四步：读 Redis 当前值，对比修正 =====
        String currentReceiveQtyStr = stringRedisTemplate.opsForValue().get(receiveQtyKey);
        int redisReceiveQty = currentReceiveQtyStr != null ? Integer.parseInt(currentReceiveQtyStr) : -1;
        if (redisReceiveQty != expectedReceiveQty) {
            log.warn("库存receiveQty偏差修正, couponId={}, redis当前={}, 期望={}, 偏差={}",
                    couponId, redisReceiveQty, expectedReceiveQty, redisReceiveQty - expectedReceiveQty);
            stringRedisTemplate.opsForValue().set(receiveQtyKey, String.valueOf(expectedReceiveQty));
        }
        // ===== 第五步：重建已领取用户集合
        List<CouponUser> receivedUsers = couponUserMapper.selectList(
                new LambdaQueryWrapper<CouponUser>()
                        .eq(CouponUser::getCouponId, couponId)
                        .select(CouponUser::getUserId)
        );
        Set<String> members = stringRedisTemplate.opsForSet().members(receivedKey);
        if (!CollectionUtils.isEmpty(members)) {
            String[] userIds = receivedUsers.stream()
                    .map(CouponUser::getUserId)
                    .map(String::valueOf)
                    .filter(receivedUser -> !members.contains(receivedUser))
                    .toArray(String[]::new);
            if (ArrayUtils.isNotEmpty(userIds)) {
                stringRedisTemplate.opsForSet().add(receivedKey, userIds);
            }
        } else {
            if (receivedUsers != null && !receivedUsers.isEmpty()) {
                // TODO: 数据量大时改为分批查询写入
                String[] userIds = receivedUsers.stream()
                        .map(CouponUser::getUserId)
                        .map(String::valueOf)
                        .toArray(String[]::new);
                if (ArrayUtils.isNotEmpty(userIds)) {
                    stringRedisTemplate.opsForSet().add(receivedKey, userIds);
                }
            }
        }
        // ===== 第六步：设置 TTL =====
        long ttl =0;
        if (coupon.getValidMode()==1) {
            ttl = calculateTtl(coupon);
        }
        if (ttl > 0) {
            stringRedisTemplate.expire(stockKey, ttl, TimeUnit.SECONDS);
            stringRedisTemplate.expire(receivedKey, ttl, TimeUnit.SECONDS);
        }
        stringRedisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(coupon.getTotalQty()));
        log.info("Redis恢复完成, couponId={}, dbReceiveQty={}, mqPendingQty={}, expectedReceiveQty={}",
                couponId, dbReceiveQty, mqQty, expectedReceiveQty);
    }

    // =============================================
    // 计算优惠券剩余有效时间（秒）
    // =============================================
    private long calculateTtl(Coupon coupon) {
        long endTimestamp = coupon.getValidEnd().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long now = System.currentTimeMillis();
        return Math.max((endTimestamp - now) / 1000, 0);
    }

    /**
     * 初始化更新购物券缓存
     */
    @Override
    public void updateCouponRedisCache() {
        List<Coupon> couponList = lambdaQuery().eq(Coupon::getStatus, 1).list();
        // 使用 Redis 管道 (Pipelined) 批量更新优惠券的基础信息缓存 (Hash结构)
        // 这样可以减少网络往返次数，提高写入性能,存coupon细节后续showList的时候可以使用
        RedisConnector.executePipelined(new SessionCallback<>() {
            @Override
            public <K, V> Object execute(@Nullable RedisOperations<K, V> operations) throws DataAccessException {
                for (Coupon coupon : couponList) {
                    String key = RedisKeyGenerator.couponDetail(coupon.getId());
                    //每个存hashkey
                    RedisConnector.setHashObject(key, coupon);//不设置ttl，根据需求下架
                }
                return null;
            }
        });

        //couponId
        List<Long> couponIdList = couponList.stream().map(Coupon::getId).toList();
        if (couponIdList.isEmpty()) {
            return;
        }
        // 查询所有相关的用户领券记录 (CouponUser)，并按 couponId 分组
        //固定时间结束的
        List<Coupon> couponFixedTimeList = couponList.stream()
                .filter(coupon -> Objects.equals(coupon.getValidMode(), CouponValidModeEnum.FIXED_TIME.getCode()))
                .toList();
        updateCouponFixedTimeListCache(couponFixedTimeList);
        //领取后n天结束的
        List<Coupon> couponAfterReceiveList = couponList.stream().filter(coupon -> Objects.equals(coupon.getValidMode(), CouponValidModeEnum.AFTER_RECEIVE.getCode()))
                .toList();
        updateAfterReceiveListCache(couponAfterReceiveList);
    }

    /**
     * 更新固定时间优惠券缓存
     * 维护 ZSet 存储优惠券id 监控状态变化 , 维护 Set存储用户 id
     * @param couponList 固定时间优惠券列表
     */
    private void updateCouponFixedTimeListCache(List<Coupon> couponList) {
        if (couponList == null || couponList.isEmpty()) {
            return;
        }
        for (Coupon coupon : couponList) {
            LocalDateTime validStart = coupon.getValidStart();
            LocalDateTime validEnd = coupon.getValidEnd();
            LocalDateTime now = LocalDateTime.now();
            Long couponId = coupon.getId();
            if (now.isBefore(validStart)) {
                long timestamp = validStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                String key = RedisKeyGenerator.couponFixedTimeUnBeginZSet();
                RedisConnector.opsForZSet().add(key, couponId, timestamp);
            }  else if (now.isAfter(validStart) && now.isBefore(validEnd)) {
                //FIXME：时间戳转换，地区现在是默认，实战可以主动指定时间戳地区
                long timestamp = validEnd.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                String key = RedisKeyGenerator.couponFixedTimeInProgressZSet();
                RedisConnector.opsForZSet().add(key, couponId, timestamp);
            }
        }
    }

    /**
     * 更新领劵后 N 天 优惠券缓存
     * 维护 ZSet (其中存储 couponUserId)监控状态变化 和 Set 存储用户 id
     *
     * @param couponList 领券后 N 天优惠券列表
     */
    private void updateAfterReceiveListCache(List<Coupon> couponList) {
        List<Long> couponIdList = couponList.stream().map(Coupon::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(couponIdList)) {
            return;
        }
        Map<Long, List<CouponUser>> couponUserMap = couponUserService.lambdaQuery()
                .in(CouponUser::getCouponId, couponIdList).list()
                .stream().collect(Collectors.groupingBy(CouponUser::getCouponId));

        RedisConnector.executePipelined(new SessionCallback<>() {
            @Override
            public <K, V> Object execute(@Nullable RedisOperations<K, V> operations) throws DataAccessException {
                for (Coupon coupon : couponList) {
                    List<CouponUser> couponUserList = couponUserMap.get(coupon.getId());
                    if (couponUserList == null) {
                        continue;
                    }
                    couponUserList.forEach(couponUser -> {
                        Long couponUserId = couponUser.getCouponId();
                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime end = couponUser.getExpireTime();
                        if (now.isBefore(end)) {
                            String key = RedisKeyGenerator.couponAfterReceiveTimeInProgressZSet();
                            long timestamp = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                            RedisConnector.opsForZSet().add(key, couponUserId, timestamp);
                        }
                    });
                }
                return null;
            }
        });
    }
    /**
     * 更新 N 天后过期的优惠券的 ZSet 缓存 其中存储couponUserId
     * @param couponUser 用户持有的优惠券
     */
    private void updateCouponUserRedisCache(CouponUser couponUser) {
        Long couponUserId = couponUser.getCouponId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = couponUser.getExpireTime();
        if (now.isBefore(end)) {
            String key = RedisKeyGenerator.couponAfterReceiveTimeInProgressZSet();
            long timestamp = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            RedisConnector.opsForZSet().add(key, couponUserId, timestamp);
        }
    }

    /**
     * 上架优惠券活动
     * @param couponId 优惠券id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> onShlef(Long couponId) {
        Coupon coupon = getById(couponId);
        if (coupon == null) {
            return Result.error(MessageConstant.COUPON_NOT_FOUND);
        }

        if (coupon.getValidEnd()!=null &&coupon.getValidEnd().isBefore(LocalDateTime.now())) {
            lambdaUpdate().set(Coupon::getStatus,3).eq(Coupon::getId,couponId).update();
            return Result.error(MessageConstant.ACTIVITY_EXPIRED);
        }
        if (CouponStatusEnum.VOIDED.equals(coupon.getStatus())) {
            return Result.error(MessageConstant.ACTIVITY_VOIDED);
        }
        //数据库操作：上架
        if (coupon.getValidEnd() == null || coupon.getValidEnd().isAfter(LocalDateTime.now())) {
            lambdaUpdate().set(Coupon::getStatus, 1).eq(Coupon::getId,couponId).update();
            if (CouponStatusEnum.OFF_SHELF.equals(coupon.getStatus())) {
                couponUserService.lambdaUpdate()
                        .set(CouponUser::getInvalidatedCount,0)
                        .setSql("unused_count = unused_count+invalidated_count ")
                        .eq(CouponUser::getCouponId,couponId)
                        .update();
            }

            //存couponDetail
            String couponDetailKey = RedisKeyGenerator.couponDetail(coupon.getId());
            RedisConnector.setHashObject(couponDetailKey,coupon);
            //写缓存方便用户领取
            if (coupon.getReleaseTime().isBefore(LocalDateTime.now())) {
                String stockKey = RedisKeyGenerator.couponStockKey(couponId);
                stringRedisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(coupon.getTotalQty()));
                String couponActivityKey = RedisKeyGenerator.couponActivity();
                RedisConnector.delete(couponActivityKey);
            } else {
                //存进这个zSet
                String couponActivityUnBeginZSetKey = RedisKeyGenerator.couponActivityUnBeginZSet();
                long timesTemp = coupon.getReleaseTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                RedisConnector.opsForZSet().add(couponActivityUnBeginZSetKey, couponId, timesTemp);
            }
            //redis存储过期时间
            coupon.setStatus(CouponStatusEnum.ON_SHELF);
            List<Coupon> couponNew = new ArrayList<>(1 );
            couponNew.add(coupon);
            if (CouponValidModeEnum.FIXED_TIME.getCode().equals(coupon.getValidMode())) {
                updateCouponFixedTimeListCache(couponNew);
            }
            return Result.success();
        }
        return Result.error(MessageConstant.ON_SHELF_ERROR);
    }

    /**
     * 优惠券活动下架
     * @param couponId 活动id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> offShelf(Long couponId) {
        Coupon coupon = getById(couponId);
        if (coupon == null) {
            return Result.error(MessageConstant.COUPON_NOT_FOUND);
        }
        if (!CouponStatusEnum.ON_SHELF.equals(coupon.getStatus())) {
            return Result.error(MessageConstant.ACTIVITY_OFF_SHELF);
        }
        if (coupon.getValidMode() == 2) {
            //领取后n天过期的优惠券下架直接作废
            lambdaUpdate().set(Coupon::getStatus, 4).eq(Coupon::getId, couponId).update();
            //删除couponDetail
            String couponDetail = RedisKeyGenerator.couponDetail(couponId);
            //正常下架就让redis跑完过期
            RedisConnector.delete(couponDetail);
        } else {
            //设为下架后续有机会重新上架
            lambdaUpdate().set(Coupon::getStatus,2).eq(Coupon::getId,couponId).update();
            //先设置为作废，在活动能开启的情况下设置为
            couponUserService.lambdaUpdate()
                    .set(CouponUser::getUnusedCount,0)
                    .setSql("invalidated_count = invalidated_count+unused_count ")
                    .eq(CouponUser::getCouponId,couponId)
                    .update();
        }
        //修改couponActivityRedis
        String couponActivityKey = RedisKeyGenerator.couponActivity();
        RedisConnector.delete(couponActivityKey);
        //清除相关缓存
        RedisConnector.delete(RedisKeyGenerator.couponDetail(couponId));
        RedisConnector.delete(RedisKeyGenerator.couponReceivedKey(couponId));
        RedisConnector.delete(RedisKeyGenerator.couponReceiveQtyKey(couponId));
        RedisConnector.delete(RedisKeyGenerator.couponStockKey(couponId));
        RedisConnector.delete(RedisKeyGenerator.couponPendingKey(couponId));
        RedisConnector.delete(RedisKeyGenerator.couponUnablePendingKey(couponId));
        return Result.success();
    }

    /**
     * 查看用户优惠券列表
     */
    @Override
    public Result<List<CouponUserVO>> showCouponUserList() {
        String userId = BaseContext.getUserId();
        //查缓存
        String couponUserListKey = RedisKeyGenerator.couponUserList(Long.valueOf(userId));
        List<CouponUserVO> couponUserList = (List<CouponUserVO>)RedisConnector.opsForValue().get(couponUserListKey);
        // 2. 缓存未命中，查库拼装
        if (CollectionUtils.isEmpty(couponUserList)) {
            //查库拼装
            List<CouponUser> list = couponUserService.lambdaQuery().eq(CouponUser::getUserId, userId).list();
            if (CollectionUtils.isEmpty(list)) {
                return Result.success();
            }
            list = list.stream().filter(couponUser -> couponUser.getUnusedCount() > 0 || couponUser.getLockedCount() > 0).toList();
            if (CollectionUtils.isEmpty(list)) {
                return Result.success();
            }
            List<Long> couponIds = list.stream().map(CouponUser::getCouponId).distinct().toList();
            List<Coupon> couponList = lambdaQuery().in(Coupon::getId, couponIds).list();
            Map<Long, Coupon> couponMap = couponList.stream().collect(Collectors.toMap(Coupon::getId, coupon -> coupon));
            couponUserList = list.stream().map(couponUser -> {
                Coupon coupon = couponMap.get(couponUser.getCouponId());
                return copyMapper.toCouponUserVO(coupon, couponUser);
            }).toList();
            RedisConnector.opsForValue().set(couponUserListKey,couponUserList);
        }
        return  Result.success(couponUserList);
    }

    /**
     * 查看couponActivity活动
     */
    @Override
    public Result<List<Coupon>> showCouponActivityList() {
        String couponActivityKey= RedisKeyGenerator.couponActivity();
        List<Coupon> coupons = (List<Coupon>)RedisConnector.opsForValue().get(couponActivityKey);
        //缓存穿透
        if (!CollectionUtils.isEmpty(coupons)) {
            //直接拿到，返回就行
            return Result.success(coupons);
        } else {
            //看门狗加分布式锁
            String couponActivityLockKey =RedisKeyGenerator.couponActivityLock();
            RLock lock = redissonClient.getLock(couponActivityLockKey);
            try {
                boolean isLocked = lock.tryLock(5, 30, TimeUnit.SECONDS);
                if (isLocked) {
                    //拿到锁再查一次
                    coupons = (List<Coupon>)RedisConnector.opsForValue().get(couponActivityKey);
                    if (!CollectionUtils.isEmpty(coupons)) {
                        return Result.success(coupons);
                    }
                    //查库
                    coupons = lambdaQuery().eq(Coupon::getStatus, CouponStatusEnum.ON_SHELF).le(Coupon::getReleaseTime,LocalDateTime.now()).list();
                    if (CollectionUtils.isEmpty(coupons)) {
                        // 数据库也没数据 -> 存空列表，有效期短一点（如 5 分钟）
                        RedisConnector.opsForValue().set(couponActivityKey, Collections.emptyList(), 5, TimeUnit.MINUTES);
                        return Result.success();
                    }
                    //缓存回填
                    RedisConnector.opsForValue().set(couponActivityKey,coupons);
                    // 有数据 -> 存数据，有效期长一点（如 30 分钟）
                    // 建议设置随机过期时间防止雪崩，例如 30分钟 + 随机0-10分钟
                    RedisConnector.opsForValue().set(couponActivityKey, coupons, 30, TimeUnit.MINUTES);
                    return Result.success(coupons);
                } else {
                    Thread.sleep(50);
                    //上述步骤
                    coupons = (List<Coupon>)RedisConnector.opsForValue().get(couponActivityKey);
                    if (!CollectionUtils.isEmpty(coupons)) {
                        return Result.success(coupons);
                    } else {
                        return Result.error(MessageConstant.DATA_ERROR);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(MessageConstant.LOCK_ERROR, e);
            }finally {
                // 【核心】：安全释放锁，无论中间发生了什么，这里一定会执行
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }
}
