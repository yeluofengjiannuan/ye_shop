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
}

