package com.demetrius.tribunal.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * 网关限流 Key 解析（M5 RequestRateLimiter）。
 *
 * <p>按用户维度限流：优先取网关注入的 X-User-Id（JwtAuthGlobalFilter 写入），
 * 未登录请求回退到客户端 IP——保证每个用户/来源独立令牌桶，互不挤占。</p>
 */
@Configuration
public class RateLimitKeyResolverConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            // 未登录：按客户端 IP 限流
            String ip = exchange.getRequest().getRemoteAddress() == null
                    ? "unknown"
                    : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            return Mono.just("ip:" + ip);
        };
    }
}
