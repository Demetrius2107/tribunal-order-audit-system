package com.demetrius.tribunal.common.redis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 分布式幂等守卫自动装配（M5）。
 *
 * <p>当 classpath 存在 StringRedisTemplate 时，自动创建 {@link RedisIdempotencyGuard}。
 * 无 Redis 环境时（StringRedisTemplate 不存在），降级到本地模式。</p>
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisIdempotencyGuard redisIdempotencyGuard(
            org.springframework.beans.factory.ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            // 无 Redis 环境降级到本地模式
            return new RedisIdempotencyGuard(null, "tribunal");
        }
        return new RedisIdempotencyGuard(redisTemplate, "tribunal");
    }
}
