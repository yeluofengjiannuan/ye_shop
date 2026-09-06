-- ============================================
-- 优惠券领取 Lua 脚本 (极简版：仅判重 + 库存校验)
-- ============================================

-- KEYS[1]：优惠券总库存 key (固定不变)
-- 例如：coupon:stock:1001

-- KEYS[2]：已领取用户集合 key (用于判重)
-- 例如：coupon:received:1001

-- KEYS[3]：已领取库存数量 key (动态变化，需与DB同步)
-- 例如：coupon:receiveQty:1001

-- ARGV[1]：当前领取用户 id
-- ARGV[2]：本次需要扣减的库存数量 (perUserQty)


-- 1. 获取总库存
local stock = tonumber(redis.call('get', KEYS[1]))
if stock == nil then
    return -1 -- 库存 key 不存在
end

-- 2. 获取已领取库存数量
local receiveQty = tonumber(redis.call('get', KEYS[3])) or 0

-- 3. 核心判断：总库存 >= 已领取 + 本次需要扣减的
if stock < (receiveQty + tonumber(ARGV[2])) then
    return 0 -- 库存不足
end

-- 4. 判断当前用户是否已经领过这张券
if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then
    return -2 -- 用户已领取
end

-- 5. 校验通过，把当前用户加入已领取集合
redis.call('sadd', KEYS[2], ARGV[1])

-- 6. 预扣库存：把已领取数量加上本次扣减的数量
redis.call('incrby', KEYS[3], ARGV[2])

-- 返回 1 表示校验通过，可以发 MQ 了
return 1