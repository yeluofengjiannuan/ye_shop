package com.itxindeshang.infrastructure.es.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class EsIndexInitializerRunner implements CommandLineRunner {
    private final EsIndexInitializerService esIndexInitializerService;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始执行 ES 索引初始化...");
        esIndexInitializerService.initProductIndex();
        log.info("ES 索引初始化完成");
    }
}
