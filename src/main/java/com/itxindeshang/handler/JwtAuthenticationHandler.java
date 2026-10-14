package com.itxindeshang.handler;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.common.result.ResultCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.SignatureException;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.UnauthenticatedException;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * JWT 认证异常处理器
 * 使用 Spring 的 @RestControllerAdvice 统一处理 Filter 抛出的异常
 */
@Slf4j
@Order(1)
@RestControllerAdvice
public class JwtAuthenticationHandler {

    /**
     * Shiro 抛出的"未认证"异常（Filter 层检查到没登录）
     */
    @ExceptionHandler(UnauthenticatedException.class)
    public Result<Object> handleUnauthenticatedException(UnauthenticatedException e) {
        // 🔑 日志：WARN 级别（正常业务失败） + 完整堆栈（e 对象直接传） + ResultCode.detail
        log.warn("认证失败: code={}, detail={}",
                ResultCode.NO_TOKEN.getCode(),
                ResultCode.NO_TOKEN.getDetail(),
                e);
        // 🔑 一行搞定：HTTP 401 + 业务 code=2001 + 友好 message
        return Result.error(HttpResponseStatus.UNAUTHORIZED.code(),ResultCode.NO_TOKEN.getCode(), ResultCode.NO_TOKEN.getMessage());
    }

    /**
     * JWT Token 过期（jjwt 库抛出）
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public Result<Object> handleExpiredJwtException(ExpiredJwtException e) {
        // 日志里可以额外打"过期时间"方便排查
        log.warn("Token 过期: code={}, detail={}, expiredAt={}",
                ResultCode.ACCESS_TOKEN_EXPIRED.getCode(),
                ResultCode.ACCESS_TOKEN_EXPIRED.getDetail(),
                e.getClaims() != null ? e.getClaims().getExpiration() : "unknown",
                e);
        return Result.error(HttpResponseStatus.UNAUTHORIZED.code(),ResultCode.ACCESS_TOKEN_EXPIRED.getCode(), ResultCode.ACCESS_TOKEN_EXPIRED.getMessage());
    }

    /**
     * Token 签名错误 / 格式错误 / 解码失败
     * （把三种 jjwt 异常合并处理，对前端来说都是"token 无效"）
     */
    @ExceptionHandler({SignatureException.class, MalformedJwtException.class, DecodingException.class})
    public Result<Object> handleJwtException(Exception e) {
        // 这些异常可能是有人恶意篡改 token（SignatureException），可以打 ERROR 方便关注
        log.error("Token 非法: code={}, detail={}, errorType={}",
                ResultCode.TOKEN_SIGNATURE_ERROR.getCode(),
                ResultCode.TOKEN_SIGNATURE_ERROR.getDetail(),
                e.getClass().getSimpleName(),
                e);
        return Result.error(HttpResponseStatus.UNAUTHORIZED.code(),ResultCode.TOKEN_SIGNATURE_ERROR.getCode(), ResultCode.TOKEN_SIGNATURE_ERROR.getMessage());
    }
}