package com.itxindeshang.common.constant;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MessageConstant {
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String LOGIN_ERROR = "用户名或密码错误";
    public static final String REFRESH_TOKEN_EXPIRED_ERROR = "登录已过期,请重新登录";
    public static final String USER_NAME_EXISTS = "用户名已存在";
    public static final String USER_NOT_LOGIN = "用户登录异常,请重新登录";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String TOKEN_INVALID = "JWT 令牌解析失败(accessToken签名错误,篡改)";
    public static final String SQL_MESSAGE_SAVE_ERROR = "数据保存失败，请稍后重试";
    public static final String SQL_MESSAGE_DELETE_ERROR = "数据删除失败，不得删除带子节点的分类，请重试";
    public static final String NO_ACCESS_TOKEN = "用户未登录";
    public static final String TOKEN_EXPIRED = "JWT 令牌解析失败(accessToken过期)";
    public static final String PERMISSION_DENIED = "权限不足";
    public static final String SYSTEM_ERROR = "服务器异常";
    public static final String SQL_MESSAGE_UPDATE_PARENTID_ERROR = "数据更新失败，不得修改父类ID";
    public static final String PRODUCT_CATEGORY_INVALID = "商品不能存放在一级分类下，请选择子分类";
    public static final String CATEGORY_NOT_FOUND = "未找到对应分类";
    public static final String TOM_CAT_ERROR = "系统繁忙，请稍后重试";
    public static final String DATA_ERROR ="数据异常，请重试";
    public static final String LOCK_ERROR ="获取商品详情分布式锁异常";
    public static final String PRODUCT_IMAGE_OR_SPEC_EMPTY = "图片或商品规格不得为空";
    public static final String PRODUCT_NOT_FOUND = "未找到改商品";
    public static final String SYSTEM_BUSY = "系统繁忙";
    //======================购物车==========================================
    public static final String CART_CLEAR_DB_FAILED = "清空购物车失败，请重试";
    public static final String CART_UPDATE_QTY_FAILED = "修改数量失败，请重试";
    public static final String CART_DELETE_BATCH_FAILED = "批量删除失败，请重试";
    //=========================优惠券========================================
    public static final String VALID_MODE_FIXED_TIME_REQUIRED = "固定时间模式下，开始时间和结束时间不能为空";
    public static final String VALID_END_BEFORE_START = "结束时间不能早于开始时间";
    public static final String VALID_MODE_RECEIVE_DAYS_REQUIRED = "领券后有效天数必须大于0";
    public static final String FULL_REDUCTION_CONDITION_REQUIRED = "满减券的门槛金额必须大于0";
    public static final String DISCOUNT_RATE_INVALID = "折扣率必须在0到1之间";
    public static final String NO_THRESHOLD_DISCOUNT_REQUIRED = "无门槛券的优惠金额必须大于0";
    public static final String COUPON_NOT_FOUND = "优惠券不存在";
    public static final String COUPON_NOT_AVAILABLE = "该优惠券不可用";
    public static final String COUPON_EXPIRED = "优惠券已过期";
    public static final String COUPON_ALREADY_USED = "优惠券已被使用";
    public static final String COUPON_USER_RECEIVE_ERROR="用户获取优惠券数量异常,请重试";
    public static final String COUPON_NO_SHELF = "优惠券未上架";
    public static final String ACTIVITY_NOT_START ="活动尚未开始";
    public static final String ACTIVITY_END ="活动已结束";
    public static final String COUPON_ERROR = "优惠券配置异常";
    public static final String COUPON_STOCK_NULL = "库存不足";
    public static final String COUPON_HOLD = "优惠券已领取";
    public static final String SYNC_SAVE_ERROR = "异步存库失败等待重试";
    public static final String RELEASE_TIME_REQUIRED = "分发时间不能早于当前时间";
    public static final String RELEASE_TIME_BEFORE_NOW = "分发时间必须早于有效期开始时间";
    public static final String USER_LEVEL_ERROR = "用户等级不匹配无法领取";
    public static final String ACTIVITY_OFF_SHELF = "活动已下架";
    public static final String ON_SHELF_ERROR = "上架失败，请稍后重试";
    public static final String ACTIVITY_EXPIRED = "活动已过期";
    public static final String ACTIVITY_ON_SHELF = "活动已上架";
    public static final String ACTIVITY_VOIDED = "活动已作废";
    public static final String PRODUCT_SPEC_NOT_MATCH_PRODUCT = "商品规格与商品不匹配";
    public static final String COUPON_STATUS_ERROR = "优惠券状态异常，锁定失败";
    public static final String ORDER_NOT_EXIST = "订单不存在";
    public static final String ORDER_NOT_CANCEL = "订单不处于可以取消状态";
    public static final Object ORDER_PROCESSED = "订单已处理";
}
