package com.demetrius.tribunal.common.trace;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * TraceId 自动配置。
 *
 * <p>Web 服务自动注册 {@link TraceIdFilter}，优先级最高（HIGHEST_PRECEDENCE + 1），
 * 确保在鉴权/业务过滤器之前生成 traceId。</p>
 *
 * <p>同时导入 {@link TraceIdFeignAutoConfiguration}，实现跨服务 traceId 透传。</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication
public class TraceIdAutoConfiguration {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.setName("traceIdFilter");
        return registration;
    }
}
