package com.demetrius.tribunal.common.auth;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;

/**
 * Feign 内部调用鉴权拦截器（服务间调用自动附加内部 Token）。
 *
 * <p>场景：order-service 通过 Feign 调用 billing/inventory/customer 等服务的业务接口时，
 * 这些接口已被 {@link AuthInterceptor} 保护；若不带凭据会被 401 拦截。
 * 大厂做法：内部服务间用「内部调用令牌」互相认证（代替用户 JWT）。</p>
 *
 * <p>实现：所有 Feign 请求统一附加 {@code X-Internal-Token} 请求头，值来自配置
 * {@code auth.internal-token}；下游 AuthInterceptor 校验该头等于本服务配置值时直接放行。</p>
 */
public class FeignInternalTokenInterceptor implements RequestInterceptor {

    public static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";

    private final String internalToken;

    public FeignInternalTokenInterceptor(@Value("${auth.internal-token:}") String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    public void apply(RequestTemplate template) {
        if (internalToken != null && !internalToken.isBlank()) {
            template.header(HEADER_INTERNAL_TOKEN, internalToken);
        }
    }
}