package com.itxindeshang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.itxindeshang.mapper")
@EnableScheduling//启动定时任务
@EnableTransactionManagement
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync//异步任务
public class YeShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(YeShopApplication.class, args);
    }

}
