package com.demetrius.tribunal.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 解析器（下游服务共享，无状态鉴权）。
 *
 * <p>auth-service 签发 Token 时把角色权限列表写入 claims；各业务服务只需配置相同的
 * {@code jwt.secret}，即可本地解析 Token 完成鉴权，无需每次调 auth-service 校验（大厂网关模式同理）。</p>
 *
 * <p>只负责<b>解析与校验</b>，不负责签发（签发在 auth-service 的 JwtProvider）。</p>
 */
public final class JwtTokenParser {

    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLE_CODE = "roleCode";
    public static final String CLAIM_PERMISSIONS = "permissions";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String TYPE_ACCESS = "access";

    private final SecretKey secretKey;

    public JwtTokenParser(String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解析 Access Token；无效/过期/类型非 access 时返回 null（不抛异常，由拦截器统一转 401）。
     */
    public Claims parseAccessToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
            // 防止 refresh token 冒充 access token 调业务接口
            if (!TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
                return null;
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 从 Claims 取权限码列表（无则空列表）。
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissions(Claims claims) {
        Object perms = claims.get(CLAIM_PERMISSIONS);
        if (perms instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    /**
     * 转换为 UserContext 需要的当前用户信息。
     */
    public UserContext.CurrentUser toCurrentUser(Claims claims) {
        return new UserContext.CurrentUser(
                claims.getSubject(),
                claims.get(CLAIM_USERNAME, String.class),
                claims.get(CLAIM_ROLE_CODE, String.class),
                getPermissions(claims));
    }
}