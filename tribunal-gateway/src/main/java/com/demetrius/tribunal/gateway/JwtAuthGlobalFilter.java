package com.demetrius.tribunal.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关全局 JWT 鉴权过滤器（M5）。
 *
 * <p>前置鉴权：网关统一解析 JWT，将 userId/username/role 注入下游请求头，
 * 后端服务无需重复解析 Token（信任网关注入的内部头）。</p>
 *
 * <p>白名单路径（不需鉴权）：/api/auth/login, /api/auth/register, /actuator/**</p>
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGlobalFilter.class);

    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/actuator");

    @Value("${jwt.secret:tribunal-order-auth-secret-key-2026-0123456789abcdef}")
    private String jwtSecret;

    @Value("${auth.internal-token:tribunal-internal-token-2026}")
    private String internalToken;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单放行
        if (WHITE_LIST.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // 内部服务间互信 Token（Feign → Gateway → Service 场景）
        String internal = request.getHeaders().getFirst("X-Internal-Token");
        if (internalToken.equals(internal)) {
            return chain.filter(exchange);
        }

        // JWT 鉴权
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("未携带 Token，拒绝: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 注入下游请求头（后端服务从 header 读取，无需再解析 JWT）
            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id", String.valueOf(claims.get("userId")))
                    .header("X-Username", String.valueOf(claims.get("username")))
                    .header("X-User-Role", String.valueOf(claims.getOrDefault("role", "USER")))
                    .header("X-Internal-Token", internalToken)
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception ex) {
            log.warn("JWT 解析失败: {}", ex.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        // 最高优先级（先于路由过滤器执行）
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
