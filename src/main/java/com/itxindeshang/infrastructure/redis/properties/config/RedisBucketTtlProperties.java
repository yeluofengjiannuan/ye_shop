package com.itxindeshang.infrastructure.redis.properties.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "redis.bucket.ttl")
@Data
public class RedisBucketTtlProperties {

    private long hotProductReadBucketTtl;

    private long hotProductWriteBucketTtl;
}
