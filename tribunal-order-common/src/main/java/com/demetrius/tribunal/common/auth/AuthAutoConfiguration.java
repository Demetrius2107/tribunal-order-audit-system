package com.demetrius.tribunal.common.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 鉴权自动配置（各业务服务引入 order-common 后自动生效）。
 *
 * <p>注册 {@link AuthInterceptor} 到所有 HTTP 请求；白名单路径默认放行：
 * {@code /api/auth/&#42;&#42;}（登录/注册/刷新/登出等）、任意路径下的 heartbeat 探活接口，
 * 可通过配置 {@code auth.white-list} 追加（逗号分隔）。</p>
 *
 * <p>同时注册 {@link FeignInternalTokenInterceptor}：服务间 Feign 调用自动附加
 * {@code X-Internal-Token} 头，下游服务校验一致后放行（内部服务互信，见 auth.internal-token）。</p>
 *
 * <p>关闭方式：{@code auth.enabled=false}。</p>
 */
@AutoConfiguration(after = WebMvcAutoConfiguration.class)
@ConditionalOnProperty(name = "auth.enabled", havingValue = "true", matchIfMissing = true)
public class AuthAutoConfiguration implements WebMvcConfigurer {

    private final JwtTokenParser tokenParser;

    /** 内部调用令牌（服务间 Feign 互信凭据） */
    private final String internalToken;

    /** 额外白名单路径（逗号分隔），默认放行认证接口与心跳 */
    private final String[] extraWhiteList;

    public AuthAutoConfiguration(
            @Value("${jwt.secret}") String secret,
            @Value("${auth.internal-token:}") String internalToken,
            @Value("${auth.white-list:}") String whiteList) {
        this.tokenParser = new JwtTokenParser(secret);
        this.internalToken = internalToken;
        this.extraWhiteList = whiteList == null || whiteList.isBlank()
                ? new String[0]
                : whiteList.split(",");
    }

    @Bean
    public JwtTokenParser jwtTokenParser() {
        return tokenParser;
    }

    @Bean
    public FeignInternalTokenInterceptor feignInternalTokenInterceptor() {
        return new FeignInternalTokenInterceptor(internalToken);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(tokenParser, internalToken))
                .addPathPatterns("/**")
                .excludePathPatterns("/api/auth/**", "/**/heartbeat")
                .excludePathPatterns(extraWhiteList);
    }
}