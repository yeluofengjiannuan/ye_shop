package com.itxindeshang.common.exception;

public class OrderException extends BusinessException {
    public OrderException(String message) {
        super(message);
    }

    public OrderException(int code, String message) {
        super(code, message);
    }

}
