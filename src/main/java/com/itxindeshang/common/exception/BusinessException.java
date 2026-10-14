package com.itxindeshang.common.exception;

import lombok.Getter;
import java.io.Serial;

/**
 * 业务异常（普通类，所有业务异常都用这个！空壳子类可以删了）
 */
@Getter
public class BusinessException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;
    /** 给前端看的友好提示 */
    private final String message;
    /** 给后端日志看的技术细节（排查问题用） */
    private final String detail;

    // 3 种构造器（和 ResultCode 的 exception() 方法对应）
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.detail = "";
    }

    public BusinessException(int code, String message, String detail) {
        super(message);
        this.code = code;
        this.message = message;
        this.detail = detail;
    }

    // 保留带 cause 的构造器（用于技术异常，比如 ES 挂了的原始异常）
    public BusinessException(int code, String message, String detail, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
        this.detail = detail;
    }

    //FIXME：老代码/子类都靠它，后续需要优化掉的
    public BusinessException(String message) {
        super(message);
        this.code = 500;           // 默认用系统错误码（或你想用别的也行）
        this.message = message;
        this.detail = "";
    }
}