package com.itxindeshang.security.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.itxindeshang.common.constant.JWTTokenClaimsConstant;
import com.itxindeshang.context.BaseContext;
import com.itxindeshang.pojo.entity.SysUser;
import com.itxindeshang.properties.JWTProperties;
import com.itxindeshang.security.token.JWTToken;
import com.itxindeshang.util.JWTUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.DecodingException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Setter
public class JWTFilter extends AuthenticatingFilter {
    private static final Logger log = LoggerFactory.getLogger(JWTFilter.class);

    // 仅用setter注入（ShiroConfig中配置，避免@Resource冲突）
    protected JWTProperties jwtProperties;
    protected Cache<String, Claims> tokenWhitelistCache;
    protected HandlerExceptionResolver handlerExceptionResolver;

    /**
     * (未使用)
     * 核心：解析Token，封装JwtToken（确保userId非空）
     * 被调用时机：Subject.login() 方法内部调用
     * 注意：
     * 1. 此时 request 中应该已经包含 Token
     * 2. 如果 Token 格式错误/为空，直接返回 null -> login 失败
     * 3. 如果 Token 解析成功，返回 JwtToken 对象（包含 userId, token, claims）
     */
    @Override
    protected AuthenticationToken createToken(ServletRequest servletRequest, ServletResponse servletResponse) throws Exception {
        HttpServletRequest httpRequest = WebUtils.toHttp(servletRequest);
        String token = getRequestToken(httpRequest);

        // 检查白名单缓存
        if (tokenWhitelistCache != null) {
            Claims cachedClaims = tokenWhitelistCache.getIfPresent(token);
            if (cachedClaims != null) {
                log.debug("从缓存中获取到 Token 信息");
                return new JWTToken(cachedClaims.get(JWTTokenClaimsConstant.SYS_USER_ID).toString(), token, cachedClaims);
            }
        }

        // 解析Token获取userId（失败则视为无效Token）
        try {
            Claims claims = JWTUtils.parseJWT(jwtProperties.getUserSecretKey(), token);
            String userId = claims.get(JWTTokenClaimsConstant.SYS_USER_ID).toString();

            // 加入白名单缓存
            if (tokenWhitelistCache != null) {
                tokenWhitelistCache.put(token, claims);
            }

            log.debug("解析Token成功，userId：{}", userId);
            return new JWTToken(userId, token, claims);
        } catch (DecodingException e) {
            log.error("Token 解码失败（包含非法字符）：{}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.error("Token 校验失败：{}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Token 解析异常", e);
            return null;
        }
    }

    /**
     * 1
     * 预处理：跨域OPTIONS请求放行
     * 流程：
     * 1. 转换 request/response 为 HttpServletRequest/HttpServletResponse
     * 2. 设置跨域响应头（允许 Origin, Methods, Headers, Credentials）
     * 3. 如果是 OPTIONS 请求，直接返回 200 OK，不继续执行后续 Filter
     * 4. 如果是其他请求，继续执行 super.preHandle() -> 进入 isAccessAllowed()
     */
    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws  Exception {
        HttpServletRequest httpRequest = WebUtils.toHttp(request);

        // 如果是 OPTIONS 请求，直接放行，不进行后续拦截逻辑
        // 由于配置了 Ordered.HIGHEST_PRECEDENCE 的 CorsFilter，
        // 这里的 OPTIONS 处理实际上是作为双重保障。
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            return true;
        }
        return super.preHandle(request, response);
    }
    //认证流程
    @Override
    protected boolean onAccessDenied(ServletRequest servletRequest, ServletResponse servletResponse) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            // 尝试解析 Token
            // 1. 从请求头提取 Token
            String tokenStr = getRequestToken(request);
            if (tokenStr == null) {
                throw new UnauthenticatedException("MessageConstant.NO_ACCESS_TOKEN");
            }
            // 2. 解析 JWT（验证签名、过期时间等）
            Claims claims = JWTUtils.parseJWT(jwtProperties.getUserSecretKey(), tokenStr);
            String userId = claims.get(JWTTokenClaimsConstant.SYS_USER_ID).toString();

            // 构建 Token,这里三个参数全是解析好的
            JWTToken token = new JWTToken(userId, tokenStr, claims);

            // 提交给 Realm 进行认证
            Subject subject = getSubject(request, response);
            subject.login(token);

            // 认证成功后，手动将 UserInfo 放入 ThreadLocal
            SysUser user = (SysUser) subject.getPrincipal();
            if (user != null) {
                BaseContext.setUserInfo(user.getUserInfo());
            }

            return true; // 认证通过，放行
        } catch (Exception e) {
//            handlerExceptionResolver.resolveException(request, response, null, e);
            return false;
        }
    }
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        if ("OPTIONS".equalsIgnoreCase(((HttpServletRequest) request).getMethod())) {
            return true;
        }
        return false;
    }
    protected String getRequestToken(HttpServletRequest request) {
        return request.getHeader(jwtProperties.getUserTokenName());

    }
    /**
     * 请求结束后的清理工作
     * 作用：
     * 1. 无论请求成功还是失败，只要 Filter 执行过，最后都会执行此方法
     * 2. 关键任务：清理 ThreadLocal 中的 UserInfo，防止内存泄漏
     */
    @Override
    public void afterCompletion(ServletRequest request, ServletResponse response, Exception exception) throws Exception {
        BaseContext.removeUserInfo();
        super.afterCompletion(request, response, exception);
    }
}
