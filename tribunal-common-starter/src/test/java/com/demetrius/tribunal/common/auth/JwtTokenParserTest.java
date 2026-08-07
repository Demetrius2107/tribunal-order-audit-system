package com.demetrius.tribunal.common.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtTokenParser 单元测试：解析/类型隔离/权限提取。
 *
 * <p>Token 由 auth-service 的 JwtProvider 签发（测试内用同 secret 复刻签发逻辑）。</p>
 */
class JwtTokenParserTest {

    private static final String SECRET = "tribunal-order-auth-secret-key-2026-0123456789abcdef";

    private JwtTokenParser parser;

    @BeforeEach
    void setUp() {
        parser = new JwtTokenParser(SECRET);
    }

    /** 复刻 auth-service 签发逻辑（access token） */
    private String issueAccess(String userId, List<String> permissions) {
        return io.jsonwebtoken.Jwts.builder()
                .subject(userId)
                .claim("username", "dealer01")
                .claim("roleCode", "DEALER")
                .claim("permissions", permissions)
                .claim("tokenType", "access")
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 3600000))
                .signWith(javax.crypto.SecretKey.class.cast(
                        io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .compact();
    }

    /** 复刻 refresh token（tokenType=refresh） */
    private String issueRefresh(String userId) {
        return io.jsonwebtoken.Jwts.builder()
                .subject(userId)
                .claim("tokenType", "refresh")
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 3600000))
                .signWith(javax.crypto.SecretKey.class.cast(
                        io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .compact();
    }

    @Test
    void 解析合法accessToken返回Claims() {
        String token = issueAccess("u1", List.of("order:create", "order:view"));

        Claims claims = parser.parseAccessToken(token);
        assertNotNull(claims);
        assertEquals("u1", claims.getSubject());
        assertEquals(List.of("order:create", "order:view"), parser.getPermissions(claims));
    }

    @Test
    void refreshToken不能冒充accessToken() {
        String refresh = issueRefresh("u1");
        assertNull(parser.parseAccessToken(refresh), "refresh token 调业务接口必须拒绝");
    }

    @Test
    void 无效token返回null不抛异常() {
        assertNull(parser.parseAccessToken("garbage-token"));
        assertNull(parser.parseAccessToken(null));
        assertNull(parser.parseAccessToken(""));
    }

    @Test
    void toCurrentUser转换权限() {
        String token = issueAccess("u1", List.of("order:review"));
        UserContext.CurrentUser user = parser.toCurrentUser(parser.parseAccessToken(token));

        assertEquals("u1", user.userId());
        assertEquals("dealer01", user.username());
        assertEquals("DEALER", user.roleCode());
        assertEquals(List.of("order:review"), user.permissions());
    }
}
