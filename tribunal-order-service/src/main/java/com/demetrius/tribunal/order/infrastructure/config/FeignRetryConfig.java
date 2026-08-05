package com.demetrius.tribunal.order.infrastructure.config;

import feign.RetryableException;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 客户端重试配置（N-207：跨服务调用失败重试）。
 *
 * <p>对 Feign 调用出现的网络异常（RetryableException）自动重试：
 * 最多重试 3 次，初始间隔 100ms，乘数 1.5（即 100ms → 150ms → 225ms）。
 * 业务异常（BizException）不重试，仅网络级异常重试。</p>
 */
@Configuration
public class FeignRetryConfig {

    @Bean
    public Retryer feignRetryer() {
        // period=100, maxPeriod=1000, maxAttempts=3
        return new Retryer.Default(100L, 1000L, 3);
    }
}