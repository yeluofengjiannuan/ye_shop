package com.itxindeshang.handler;

import com.itxindeshang.common.exception.BusinessException;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.common.result.ResultCode;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 注意：Shiro 权限异常由 ShiroExceptionHandler 单独处理（@Order(1) 优先级更高）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常：业务规则错误，HTTP 200 + 业务 code（比如商品不存在 code=3001）
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        if (!e.getDetail().isEmpty()) {
            log.warn("业务异常: code={}, message={}, detail={}", e.getCode(), e.getMessage(), e.getDetail());
        } else {
            log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        }
        //  显式 HTTP 200 + 业务 code
        return Result.error(HttpResponseStatus.OK.code(), e.getCode(), e.getMessage());
    }

    // 参数校验异常：HTTP 400（Bad Request），告诉浏览器请求格式不对
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<?> handleValidationException(Exception e) {
        String msg;
        if (e instanceof MethodArgumentNotValidException ex) {
            msg = ex.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
        } else {
            msg = ((BindException) e).getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
        }
        log.warn("参数校验失败: {}", msg);
        // 显式 HTTP 400 + 业务 code 1001
        return Result.error(HttpResponseStatus.BAD_REQUEST.code(), ResultCode.PARAM_INVALID.getCode(), msg);
    }

    // 约束校验异常：HTTP 400（Bad Request），告诉浏览器请求格式不对
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.error(HttpResponseStatus.BAD_REQUEST.code(), ResultCode.PARAM_INVALID.getCode(), msg);
    }

    // Spring Boot 3.2+ 统一校验异常：HTTP 400（Bad Request），告诉浏览器请求格式不对
    @ExceptionHandler(HandlerMethodValidationException.class)
    public Result<?> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        String msg = e.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(error -> {
                    if (error.getDefaultMessage() != null) {
                        return error.getDefaultMessage();
                    }
                    return "参数校验失败";
                })
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        // 🔑 显式 HTTP 400 + 业务 code 1001
        return Result.error(HttpResponseStatus.BAD_REQUEST.code(), ResultCode.PARAM_INVALID.getCode(), msg);
    }

    // 缺少必填请求参数：HTTP 400（Bad Request），告诉浏览器请求格式不对
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.error(HttpResponseStatus.BAD_REQUEST.code(), ResultCode.PARAM_INVALID.getCode(),
                "缺少必填参数: " + e.getParameterName());
    }

    // 兜底：系统异常 HTTP 500
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("未捕获异常", e);
        return Result.error(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMessage());
    }
}