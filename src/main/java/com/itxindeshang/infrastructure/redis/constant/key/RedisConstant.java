package com.itxindeshang.infrastructure.redis.constant.key;

/**
 * 规范:一级key: PREFIX_... = "...:"
 *     二级key: ... ="...:"
 *     ......
 *     变量key拼接在 RedisKeyGenerator
 */
public class RedisConstant {
    public static final String PREFIX_LOGIN = "login:";
    public static final String USER = "user:";
    public static final String REFRESH = "refresh:";
    public static final String TOKEN = "token:";
    public static final String PREFIX_PRODUCT = "product:";
    public static final String DETAIL = "detail";
    public static final String COLLECTION ="collection:";
    public static final String VIEW = "view:";
    public static final String COUNT = "count:";
    public static final String LOCK ="lock:";
    public static final String CART = "cart:";
    public static final String PREFIX_SPEC = "spec:";
    public static final String PREFIX_COUPON = "coupon:";
    public static final String STOCK = "stock:";
    public static final String RECEIVE_QTY = "receiveQty:";
    public static final String RECEIVED = "received:";
    public static final String PENDING ="pending:";
    public static final String UNABLE_PENDING="unablePending:";
    public static final String COUPON_FIXED_TIME_UN_BEGIN ="couponFixedTimeUnBegin" ;
    public static final String USE_STATUS = "useStatus:";
    public static final String ID_LIST="idList";
    public static final String COUPON_FIXED_TIME_IN_PROGRESS = "couponFixedTimeInProgress";
    public static final String AFTER_RECEIVE_TIME_IN_PROGRESS = "afterReceiveTimeInProgress";
    public static final String ACTIVITY_UN_BEGIN = "activityUnBegin";
    public static final String LIST = "list:";
    public static final String ACTIVITY = "activity";
    public static final String PREFIX_ORDER ="order:";
    public static final String ORDERNO = "orderNo:";
}

