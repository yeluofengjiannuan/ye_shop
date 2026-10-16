package com.itxindeshang.common.result;

import com.itxindeshang.common.exception.BusinessException;
import lombok.Getter;

/**
 * 统一返回码枚举（唯一！不要再建别的错误码枚举）
 *
 * 【字段说明】
 *   message → 给前端看的友好提示（用户能看懂）
 *   detail  → 给后端日志看的技术细节（排查问题用，默认空字符串）
 *
 * 【code 范围规划】
 *   200     成功（固定，不要改！）
 *   1xxx    参数校验（前端传参错了）
 *   2xxx    认证/授权（登录/权限）
 *   3xxx    商品/分类（业务规则错误）
 *   4xxx    购物车（业务规则错误）
 *   5xxx    优惠券/活动（业务规则错误 + 运营后台参数校验）
 *   6xxx    订单（业务规则错误）
 *   8xxx    数据库通用（技术层错误，给前端友好提示，detail 里保留技术信息）
 *   9xxx    系统兜底（技术依赖故障，给前端"稍后重试"，detail 里留技术信息）
 */
@Getter
public enum ResultCode {

    // ==================== 成功（固定 200） ====================
    SUCCESS(200, "操作成功", ""),
    ERROR(500, "系统异常",""),//先保留后续处理完可以删
    // ==================== 1xxx 参数校验 ====================
    /** 通用参数不合法（GlobalExceptionHandler 里参数校验异常会覆盖 message，所以这个主要给手动抛的用） */
    PARAM_INVALID(1001, "参数不合法，请检查后重试", ""),
    /** 缺少必填参数（Controller 层缺少 @RequestParam 触发） */
    MISSING_REQUIRED_PARAM(1002, "缺少必填参数，请检查后重试", ""),

    // ==================== 2xxx 认证/授权 ====================
    NO_TOKEN(2001, "用户未登录，请先登录", ""),
    /** 给前端友好提示，detail 里可以加 "token过期时间=xxx"（抛异常时用 exception(customMessage, customDetail) 覆盖） */
    ACCESS_TOKEN_EXPIRED(2002, "登录已过期，请重新登录", "accessToken 过期"),
    REFRESH_TOKEN_EXPIRED(2003, "登录已过期，请重新登录", "refreshToken 过期"),
    TOKEN_SIGNATURE_ERROR(2004, "登录状态异常，请重新登录", "token 签名错误/被篡改"),
    LOGIN_ERROR(2005, "用户名或密码错误", ""),
    ACCOUNT_NOT_FOUND(2006, "账号不存在，请检查后重试", ""),
    USER_ALREADY_EXISTS(2007, "用户名已存在，请更换用户名", ""),
    USER_DOES_NOT_EXIST(2008, "用户不存在，请检查后重试", ""),
    ACCOUNT_LOCKED(2009, "账号已被锁定，请联系管理员", ""),
    PERMISSION_DENIED(2010, "权限不足，无法访问该资源", ""),

    // ==================== 3xxx 商品/分类 ====================
    PRODUCT_NOT_FOUND(3001, "抱歉，您查看的商品不存在", "productId 不存在"),
    PRODUCT_CATEGORY_INVALID(3002, "商品不能存放在一级分类下，请选择子分类", ""),
    PRODUCT_IMAGE_OR_SPEC_EMPTY(3003, "商品图片或规格不能为空，请检查后重试", ""),
    CATEGORY_NOT_FOUND(3004, "未找到对应分类，请检查后重试", "categoryId 不存在"),
    /** 分布式锁失败：前端友好，detail 里保留技术信息 */
    LOCK_ERROR(3005, "当前操作人数较多，请稍后重试", "分布式锁获取失败"),
    PRODUCT_SPEC_NOT_MATCH(3006, "商品规格与商品不匹配，请检查后重试", ""),
    CATEGORY_PARENTID_FIXED(3007, "数据更新失败，分类父类ID不可修改", ""),
    HOT_PRODUCT_LIMIT_EXCEEDED(3008, "热门商品缓存数量超限，请联系管理员", ""),

    // ==================== 4xxx 购物车 ====================
    CART_CLEAR_DB_FAILED(4001, "清空购物车失败，请稍后重试", "数据库删除失败"),
    CART_UPDATE_QTY_FAILED(4002, "修改数量失败，请稍后重试", "数据库更新失败"),
    CART_DELETE_BATCH_FAILED(4003, "批量删除失败，请稍后重试", "数据库批量删除失败"),

    // ==================== 5xxx 优惠券/活动 ====================
    // --- 通用业务错误 ---
    COUPON_NOT_FOUND(5001, "优惠券不存在，请检查后重试", "couponId 不存在"),
    COUPON_NOT_AVAILABLE(5002, "该优惠券当前不可用，请稍后再试", ""),
    COUPON_EXPIRED(5003, "优惠券已过期，无法使用", ""),
    COUPON_ALREADY_USED(5004, "您已使用过该优惠券", ""),
    /** 库存不足：明确告诉用户是优惠券的库存 */
    COUPON_STOCK_INSUFFICIENT(5005, "优惠券剩余库存不足", ""),
    /** 已领取：明确告诉用户是领取上限，不是锁定 */
    COUPON_ALREADY_RECEIVED(5006, "您已领取过该优惠券", ""),
    COUPON_STATUS_ERROR(5007, "优惠券状态异常，请稍后再试", "状态锁定失败"),
    COUPON_RECEIVE_LIMIT_EXCEEDED(5008, "您已达到该优惠券领取上限", ""),
    COUPON_NOT_ON_SHELF(5009, "优惠券未上架，无法领取", ""),
    // --- 活动状态 ---
    ACTIVITY_NOT_STARTED(5010, "活动尚未开始，请耐心等待", ""),
    ACTIVITY_ENDED(5011, "活动已结束，无法参与", ""),
    COUPON_CONFIG_ERROR(5012, "优惠券配置异常，请联系管理员", ""),
    RELEASE_TIME_INVALID(5013, "优惠券发放时间设置错误，请检查后重试", ""),
    RELEASE_TIME_BEFORE_VALIDITY(5014, "发放时间必须早于有效期开始时间", ""),
    USER_LEVEL_NOT_MATCH(5015, "您的用户等级不满足领取条件", ""),
    ACTIVITY_OFF_SHELF(5016, "活动已下架，无法参与", ""),
    ACTIVITY_ALREADY_ON_SHELF(5017, "活动已上架，无需重复操作", ""),
    ACTIVITY_VOIDED(5018, "活动已作废，无法参与", ""),
    ON_SHELF_FAILED(5019, "上架失败，请稍后重试", ""),
    ASYNC_SAVE_FAILED(5020, "数据保存中，请稍后刷新查看", "异步存库失败等待重试"),
    // --- 运营后台：优惠券创建参数校验（给运营人员看的，message 可以更专业） ---
    COUPON_FIXED_TIME_REQUIRED(5101, "固定时间模式下，开始和结束时间不能为空", ""),
    COUPON_TIME_RANGE_INVALID(5102, "结束时间不能早于开始时间", ""),
    COUPON_RECEIVE_DAYS_INVALID(5103, "领券后有效天数必须大于 0", ""),
    COUPON_FULL_THRESHOLD_INVALID(5104, "满减券的门槛金额必须大于 0", ""),
    COUPON_DISCOUNT_RATE_INVALID(5105, "折扣率必须在 0 到 1 之间（如 0.8 表示 8 折）", ""),
    COUPON_NO_THRESHOLD_AMOUNT_INVALID(5106, "无门槛券的优惠金额必须大于 0", ""),
    COUPON_SPECIFIED_GOODS_EMPTY(5107, "指定商品时，商品列表不能为空", ""),
    COUPON_GOODS_NOT_EXIST(5108, "部分商品 ID 不存在，请检查后重试", ""),
    COUPON_SPECIFIED_CATEGORY_EMPTY(5109, "指定分类时，分类列表不能为空", ""),
    COUPON_CATEGORY_NOT_EXIST(5110, "部分分类 ID 不存在，请检查后重试", ""),

    // ==================== 6xxx 订单 ====================
    ORDER_NOT_EXIST(6001, "订单不存在，请检查后重试", "orderNo 不存在"),
    ORDER_STATUS_INVALID(6002, "当前订单状态不支持该操作（仅待支付订单可取消）", ""),
    ORDER_ALREADY_COMPLETED(6003, "订单已完成，无法重复操作", ""),
    STOCK_RESTORE_FAILED(6004, "库存恢复失败，请联系管理员", "数据库/Redis 恢复失败"),
    COUPON_RESTORE_FAILED(6005, "优惠券恢复失败，请联系管理员", "数据库/Redis 恢复失败"),
    COUPON_CONFIRM_FAILED(6006, "优惠券核销失败，请联系管理员", "数据库核销失败"),
    SALE_COUNT_UPDATE_FAILED(6007, "销量更新失败，请联系管理员", "数据库更新失败"),

    // ==================== 8xxx 数据库通用 ====================
    /** 通用数据库保存失败：给前端友好提示，detail 里保留技术信息 */
    DB_SAVE_FAILED(8001, "数据保存失败，请稍后重试", "INSERT/UPDATE 影响行数=0"),
    /** 带子节点的分类不能删：明确告诉用户原因 */
    DB_DELETE_FAILED(8002, "数据删除失败：该分类下有子分类，请先删除子分类后再试", ""),
    DATA_ALREADY_EXISTS(8003, "数据已存在，请检查后重试", "唯一约束冲突/DuplicateKeyException"),

    // ==================== 9xxx 系统兜底 ====================
    /** 系统级技术故障：前端只看到"稍后重试"，detail 里保留具体异常（ES挂了？Redis挂了？） */
    SYSTEM_ERROR(9001, "服务器异常，请稍后重试", "系统内部异常"),
    SYSTEM_BUSY(9002, "系统繁忙，请稍后重试", "请求过多/外部依赖超时");

    private final int code;
    /** 给前端看的友好提示（用户能看懂，不暴露技术细节） */
    private final String message;
    /** 给后端日志看的技术细节（排查问题用，默认空字符串） */
    private final String detail;

    ResultCode(int code, String message, String detail) {
        this.code = code;
        this.message = message;
        this.detail = detail;
    }

    // ==================== 异常工厂方法（重载，满足不同场景） ====================

    /**
     * 用枚举自带的 message + detail 创建异常（最常用）
     */
    public BusinessException exception() {
        return new BusinessException(this.code, this.message, this.detail);
    }

    /**
     * 覆盖 message（保留 code 和 detail）
     * 用在：业务场景需要更具体的用户提示（比如 ProductServiceImpl 里可以：throw ResultCode.PRODUCT_NOT_FOUND.exception("商品ID=123 不存在") —— 前端看到具体ID，日志里有技术细节）
     */
    public BusinessException exception(String customMessage) {
        return new BusinessException(this.code, customMessage, this.detail);
    }

    /**
     * 同时覆盖 message 和 detail（最灵活）
     * 用在：MQ Consumer / ES 操作等技术场景 —— 前端看到友好提示，日志里有详细排查信息
     */
    public BusinessException exception(String customMessage, String customDetail) {
        return new BusinessException(this.code, customMessage, customDetail);
    }
}