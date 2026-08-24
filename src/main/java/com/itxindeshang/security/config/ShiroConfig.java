package com.itxindeshang.security.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.itxindeshang.properties.JWTProperties;
import com.itxindeshang.security.filter.JWTFilter;
import com.itxindeshang.security.realm.CustomRealm;
import jakarta.servlet.Filter;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {
    // 1. 自定义Realm（认证+授权核心）
    @Bean
    public CustomRealm customRealm() {
        return new CustomRealm();
    }
    // 2. 彻底禁用Session（前后端分离必需）
    @Bean
    public DefaultWebSessionManager sessionManager() {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        sessionManager.setSessionIdCookieEnabled(false); // 关闭Session Cookie
        sessionManager.setSessionIdUrlRewritingEnabled(false); // 关闭 URL 重写
        sessionManager.setGlobalSessionTimeout(-1); // 禁用 Session 超时
        return sessionManager;
    }
    // 3. Shiro核心管理器（禁用Session存储）
    @Bean
    public SecurityManager securityManager(CustomRealm customRealm, DefaultWebSessionManager sessionManager) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(customRealm);
        securityManager.setSessionManager(sessionManager);

        // 强制禁用Session存储（避免依赖Session导致认证混乱）
        DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        DefaultSessionStorageEvaluator sessionStorageEvaluator = new DefaultSessionStorageEvaluator();
        sessionStorageEvaluator.setSessionStorageEnabled(false);
        subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator);
        securityManager.setSubjectDAO(subjectDAO);
        return securityManager;
    }
    // 4. 统一返回格式的ObjectMapper
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
//        objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);快要弃用了
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
    // 5. 自定义JwtFilter
    @Bean
    public JWTFilter jwtFilter(JWTProperties jwtProperties, @Lazy HandlerExceptionResolver handlerExceptionResolver) {
        JWTFilter jwtFilter = new JWTFilter();
        configureJwtFilter(jwtFilter, jwtProperties, handlerExceptionResolver);
        return jwtFilter;
    }

    private void configureJwtFilter(JWTFilter filter, JWTProperties jwtProperties, HandlerExceptionResolver handlerExceptionResolver) {
        filter.setJwtProperties(jwtProperties);
        filter.setHandlerExceptionResolver(handlerExceptionResolver);
        filter.setTokenWhitelistCache(Caffeine.newBuilder()
                .expireAfterWrite(java.time.Duration.ofMinutes(10))
                .maximumSize(1000)
                .build());
    }
    /**
     * 禁用 Spring Boot 自动注册 JwtFilter 为全局过滤器
     * 避免 UnavailableSecurityManagerException
     */
    @Bean
    public FilterRegistrationBean<JWTFilter> registration(@Qualifier("jwtFilter") JWTFilter filter) {
        FilterRegistrationBean<JWTFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
    // 6. 核心：Shiro过滤器工厂（无任何javax依赖）
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager,
                                                         @Qualifier("jwtFilter") JWTFilter jwtFilter
                                                         /*,@Qualifier("optionalJwtFilter") OptionalJwtFilter optionalJwtFilter */){
        ShiroFilterFactoryBean factoryBean = new ShiroFilterFactoryBean();
        factoryBean.setSecurityManager(securityManager);

        // 注册JwtFilter（Jakarta的Filter，Shiro 1.12完全兼容）
        Map<String, Filter> filters = new LinkedHashMap<>();
        filters.put("jwt", jwtFilter);
        /*filters.put("optionalJwt", optionalJwtFilter);*/
        factoryBean.setFilters(filters);

        // 拦截规则（顺序：自上而下，公开接口在前）
        Map<String, String> filterChainDefinitionMap = getStringStringMap();

        factoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        return factoryBean;
    }

    private static @NonNull Map<String, String> getStringStringMap() {
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
        // 公开接口（无需认证）

         //TODO：这里是我的项目后续自己安排的
        filterChainDefinitionMap.put("/api/login/**", "anon");
        //TODO：测试完记得弄回来
        filterChainDefinitionMap.put("/**", "jwt");
        return filterChainDefinitionMap;
    }
    // 7. 启用Shiro注解支持（@RequiresRoles等）
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }
    // 8. AOP自动代理（确保注解生效）
    @Bean
    @DependsOn("lifecycleBeanPostProcessor")
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator creator = new DefaultAdvisorAutoProxyCreator();
        creator.setProxyTargetClass(true); // 强制 cglib代理
        return creator;
    }

    // 9. Shiro生命周期处理器

    /**
     * JWTFilter 的生命周期和初始化顺序，必须由 Shiro 主导，而非 Spring Boot”。确保了组件在正确的时机被正确初始化
     * @return
     */
    @Bean
    public static org.apache.shiro.spring.LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new org.apache.shiro.spring.LifecycleBeanPostProcessor();
    }
}
