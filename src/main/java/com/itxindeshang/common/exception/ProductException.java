package com.itxindeshang.common.exception;

/**
 * 商品模块业务异常
 */
public class ProductException extends BusinessException {
    public ProductException(String message) {
        super(message);
    }
    public ProductException(int code, String message) {
        super(code, message);
    }
}
