package com.itxindeshang.common.exception;

import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常父类
 */
@Getter
public abstract class BusinessException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private  int code;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

}
