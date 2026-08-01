package com.itxindeshang.infrastructure.mp.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {
    @Bean // 2. 将拦截器注册为 Spring 的 Bean.
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 3. 添加分页插件，并指定数据库类型（这里以 MySQL 为例）
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());// 防全表更新插件
        // 如果配置多个插件, 切记分页最后添加
        // 指定 DbType 可以避免 MP 每次去探测数据库类型，提升性能
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
