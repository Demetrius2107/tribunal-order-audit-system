package com.demetrius.tribunal.auth.application.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 令牌提供者（签发 + 校验，支持 Access/Refresh 双 Token）。
 *
 * <p>对应需求：N-401（认证）。</p>
 *
 * <ul>
 *   <li>Access Token：短时效（默认 2h），携带权限 claims，用于业务接口鉴权</li>
 *   <li>Refresh Token：长时效（默认 7 天），仅用于换取新的 Access Token，不含权限</li>
 * </ul>
 *
 * <p>使用 HMAC-SHA256 对称签名，密钥从配置读取（jwt.secret），
 * 生产环境应使用 256 位以上随机密钥并妥善保管。</p>
 */
@Component
public class JwtProvider {

    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLE_CODE = "roleCode";
    public static final String CLAIM_PERMISSIONS = "permissions";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";

    /** Token 类型：access（业务接口） */
    public static final String TYPE_ACCESS = "access";
    /** Token 类型：refresh（仅用于刷新） */
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;

    /** Access Token 过期时间（毫秒），默认 2 小时 */
    private final long accessExpirationMs;

    /** Refresh Token 过期时间（毫秒），默认 7 天 */
    private final long refreshExpirationMs;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.access-expiration-ms:7200000}") long accessExpirationMs,
                       @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * 签发 Access Token（携带权限列表，下游服务无状态鉴权）。
     *
     * @param userId      用户 ID
     * @param username    用户名
     * @param roleCode    角色编码
     * @param permissions 该角色拥有的权限码列表（RBAC）
     * @return JWT 字符串
     */
    public String generateAccessToken(String userId, String username, String roleCode, List<String> permissions) {
        return generate(userId, username, roleCode, permissions, TYPE_ACCESS, accessExpirationMs);
    }

    /**
     * 签发 Refresh Token（不含权限，仅标识身份；轮换由 RefreshTokenStore 管理）。
     */
    public String generateRefreshToken(String userId, String username, String roleCode) {
        return generate(userId, username, roleCode, List.of(), TYPE_REFRESH, refreshExpirationMs);
    }

    private String generate(String userId, String username, String roleCode,
                            List<String> permissions, String tokenType, long expirationMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE_CODE, roleCode)
                .claim(CLAIM_PERMISSIONS, permissions)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 校验并解析 Token。
     *
     * @param token JWT 字符串
     * @return Claims（包含 userId=subject, username, roleCode, permissions, tokenType）
     * @throws JwtException  Token 无效或已过期
     * @throws IllegalArgumentException token 为空
     */
    public Claims validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token 不能为空");
        }
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();
    }

    /**
     * 校验 Token 且必须是指定类型（access 或 refresh），防止 refresh token 冒充 access token 调业务接口。
     */
    public Claims validateTokenOfType(String token, String expectedType) {
        Claims claims = validateToken(token);
        String type = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!expectedType.equals(type)) {
            throw new JwtException("Token 类型不匹配: expected=" + expectedType + ", actual=" + type);
        }
        return claims;
    }

    /**
     * 从 Claims 中取权限码列表（可能为空）。
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
     * 检查 Token 是否已过期。
     */
    public boolean isExpired(String token) {
        try {
            validateToken(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}