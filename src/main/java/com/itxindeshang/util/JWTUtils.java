package com.itxindeshang.util;

import com.itxindeshang.common.constant.MessageConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

@Component
public class JWTUtils {
    private static final String JWT_PREFIX = "Bearer ";

    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        // 生成 JWT的时间
        long expMillis = System.currentTimeMillis() + ttlMillis;//底层时间精准，传输才用LocalDateTime
        Date exp = new Date(expMillis);

        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                //这个是：存放在JWT中的数据，可以自定义
                .claims(claims)
                //设置过期时间
                .expiration(exp)
                //设置签名
                .signWith(key)
                //创建JWT
                .compact();
    }

    /**
     * Token 解密
     *
     * @param secretKey jwt秘钥 此秘钥一定要保留好在服务端, 不能暴露出去, 否则sign就可以被伪造, 如果对接多个客户端建议改造成多个
     * @param token     加密后的 token
     * @return
     */
    public static Claims parseJWT(String secretKey, String token) {
        if (Objects.isNull(token) || !token.startsWith(JWT_PREFIX)) {
            throw new SignatureException(MessageConstant.TOKEN_INVALID);
        }
        String cleanToken = token.substring(JWT_PREFIX.length()).replaceAll("\\s", "");// 去掉 Bearer和空格
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(cleanToken)
                .getPayload();
    }
}
