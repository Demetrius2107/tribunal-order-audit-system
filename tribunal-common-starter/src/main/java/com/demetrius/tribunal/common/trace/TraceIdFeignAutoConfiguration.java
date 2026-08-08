package com.demetrius.tribunal.common.trace;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TraceId Feign 拦截器自动配置。
 *
 * <p>当消费方引入 openfeign 时自动生效，将当前 MDC 中的 traceId
 * 通过 {@code X-Trace-Id} 请求头传递给下游服务。</p>
 */
@Configuration
public class TraceIdFeignAutoConfiguration {

    @Bean
    public RequestInterceptor traceIdFeignInterceptor() {
        return template -> {
            String traceId = MDC.get(TraceIdFilter.MDC_TRACE_ID);
            if (traceId != null) {
                template.header(TraceIdFilter.TRACE_ID_HEADER, traceId);
            }
        };
    }
}
