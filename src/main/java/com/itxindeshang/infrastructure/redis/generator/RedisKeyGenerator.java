package com.itxindeshang.infrastructure.redis.generator;

import com.itxindeshang.infrastructure.redis.constant.key.RedisConstant;

import java.time.LocalDate;

/**
 * Redis key 拼接器
 */
public class RedisKeyGenerator {


    /**
     * 用户登录信息
     * login:user: + userId
     * @param userId 用户id
     */
    public static String loginUser(long userId) {
        return RedisConstant.PREFIX_LOGIN + RedisConstant.USER + userId;
    }


    /**
     *刷新 Token
     * login:refresh:token + UUID
     */
    public static String loginRefreshToken(String UUID) {
        return RedisConstant.PREFIX_LOGIN + RedisConstant.REFRESH + RedisConstant.TOKEN + UUID;
    }

    /**
     *  productDetail
     *  product: + detail: + productId
     */
    public static String productDetail(Long productId) {
        return RedisConstant.PREFIX_PRODUCT+ RedisConstant.DETAIL + productId;
    }

    /**
     * productCollection
     *  product: +collection: + productId
     */
    public static String productCollection(Long productId) {
        return RedisConstant.PREFIX_PRODUCT + RedisConstant.COLLECTION +productId;
    }

    /**
     * productView
     * product: +view: + productId + today
     */
    public static String productView(Long productId,Long userId) {
        String date = LocalDate.now().toString();
        return RedisConstant.PREFIX_PRODUCT +RedisConstant.VIEW + date +":" +productId+":"+ userId;
    }

    /**
     * productViewViewCount
     * product +view+ count: +productId
     */
    public static String productViewCount(Long productId) {
        return RedisConstant.PREFIX_PRODUCT +RedisConstant.VIEW +RedisConstant.COUNT + productId;
    }
    //lock:product:detail
    public static String lockProductDetail(Long productId) {
        return RedisConstant.LOCK+ RedisConstant.PREFIX_PRODUCT + productId;
    }


    /**
     * cartKey
     * cart:+user: + userId
     * @param userId 用户id
     */
    public static String cartKey(String userId) {
        return RedisConstant.CART+RedisConstant.USER +userId;
    }

    /**
     * cartHashKey
     * product: + productId+, +spec: +specId
     * @param productId 商品id
     * @param specId 规格id
     */
    public static String cartHashKey(Long productId, Long specId) {
        return RedisConstant.PREFIX_PRODUCT+ productId + ","+RedisConstant.PREFIX_SPEC + specId;
    }

    /**
     * lockCart
     * lock: +cart: +userId+:+ specId
     * @param userId 用户id
     * @param specId 商品规格id
     */
    public static String lockCart(String userId, Long specId) {
        return RedisConstant.LOCK +RedisConstant.CART +userId +":"+ specId;
    }

    /**
     * couponStockKey
     * coupon: + stock: + "{" +couponId+ "}"
     * @param couponId 优惠券id
     */
    public static String couponStockKey(Long couponId) {
        return RedisConstant.PREFIX_COUPON + RedisConstant.STOCK + "{" + couponId + "}";
    }

    /**
     * couponStockKey
     * coupon: + received: + "{" +couponId+ "}"
     * @param couponId 优惠券id
     */
    public static String couponReceivedKey(Long couponId) {
        return RedisConstant.PREFIX_COUPON + RedisConstant.RECEIVED + "{" + couponId + "}";
    }

    /**
     * couponStockKey
     * coupon: + receiveQty: + "{" +couponId+ "}"
     * @param couponId 优惠券id
     */
    public static String couponReceiveQtyKey(Long couponId) {
        return RedisConstant.PREFIX_COUPON + RedisConstant.RECEIVE_QTY + "{" + couponId + "}";
    }

    /**
     * couponStockKey
     * coupon: + pending: + "{" +couponId+ "}"
     * @param couponId 优惠券id
     */
    public static String couponPendingKey(Long couponId) {
        return RedisConstant.PREFIX_COUPON + RedisConstant.PENDING + "{" + couponId + "}";
    }

    /**
     * couponStockKey
     * coupon: + unablePending: + "{" +couponId+ "}"
     * @param couponId 优惠券id
     */
    public static String couponUnablePendingKey(Long couponId) {
        return RedisConstant.PREFIX_COUPON + RedisConstant.UNABLE_PENDING + "{" + couponId + "}";
    }

    public static String couponLock(Long couponId) {
        return RedisConstant.PREFIX_COUPON+RedisConstant.LOCK+couponId;
    }

    /**
     * couponDetail
     * coupon: +  detail: +couponId
     * @param couponId 优惠券id
     */
    public static String couponDetail(Long couponId) {
        return RedisConstant.PREFIX_COUPON +RedisConstant.DETAIL +couponId;
    }

    /**
     * couponFixedTimeUnBeginZSet
     * coupon: + couponFixedTimeUnBegin
     */
    public static String couponFixedTimeUnBeginZSet() {
        return RedisConstant.PREFIX_COUPON + RedisConstant.COUPON_FIXED_TIME_UN_BEGIN;
    }

    /**
     * couponFixedTimeInProgressZSet
     * coupon: + couponFixedTimeInProgress
     */
    public static String couponFixedTimeInProgressZSet() {
        return RedisConstant.PREFIX_COUPON + RedisConstant.COUPON_FIXED_TIME_IN_PROGRESS;
    }

    /**
     * couponAfterReceiveTimeInProgressZSet
     * coupon: + couponAfterReceiveTimeInProgress
     */
    public static String couponAfterReceiveTimeInProgressZSet() {
        return RedisConstant.PREFIX_COUPON + RedisConstant.AFTER_RECEIVE_TIME_IN_PROGRESS;
    }

    /**
     * couponActivityUnBegin
     * coupon: + activityUnBegin
     */
    public static String couponActivityUnBeginZSet() {
        return RedisConstant.PREFIX_COUPON  +RedisConstant.ACTIVITY_UN_BEGIN;
    }

    /**
     * couponUserList
     * coupon:+user:+list:+{userId}
     */
    public static String couponUserList(Long userId) {
        return RedisConstant.PREFIX_COUPON + RedisConstant.USER + RedisConstant.LIST + "{" + userId + "}";
    }

    /**
     * couponActivityList
     * coupon:+activity+list
     */
    public static String couponActivity() {
        return RedisConstant.PREFIX_COUPON+RedisConstant.ACTIVITY;
    }

    /**
     * couponActivityLock
     * lock:+coupon:+activity
     */
    public static String couponActivityLock() {
        return RedisConstant.LOCK+RedisConstant.PREFIX_COUPON+RedisConstant.ACTIVITY;
    }

    /**
     * orderKey
     * order: + detail: + orderNo: + orderNo
     * @param orderNo 订单号
     */
    public static String orderKey(String orderNo) {
        return RedisConstant.PREFIX_ORDER + RedisConstant.DETAIL+RedisConstant.ORDERNO +orderNo;
    }
}
