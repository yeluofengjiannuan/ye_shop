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
}
