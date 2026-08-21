package com.itxindeshang.infrastructure.redis.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "redis.cache.ttl")
@Data
public class RedisCacheTtlProperties {

    private long productDetailTtl;

    private long productViewTtl;

}
