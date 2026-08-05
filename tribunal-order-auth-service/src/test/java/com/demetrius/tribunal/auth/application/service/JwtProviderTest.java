package com.demetrius.tribunal.auth.application.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtProvider 单元测试：双 Token 签发/校验/类型隔离。
 */
class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider("tribunal-order-auth-secret-key-2026-0123456789abcdef", 7200000, 604800000);
    }

    @Test
    void generateAccessToken_携带权限claims() {
        String token = jwtProvider.generateAccessToken("u1", "dealer01", "DEALER", List.of("order:create", "order:view"));

        Claims claims = jwtProvider.validateToken(token);
        assertEquals("u1", claims.getSubject());
        assertEquals("dealer01", claims.get(JwtProvider.CLAIM_USERNAME));
        assertEquals("DEALER", claims.get(JwtProvider.CLAIM_ROLE_CODE));
        assertEquals(List.of("order:create", "order:view"), jwtProvider.getPermissions(claims));
        assertEquals(JwtProvider.TYPE_ACCESS, claims.get(JwtProvider.CLAIM_TOKEN_TYPE));
    }

    @Test
    void generateRefreshToken_类型为refresh且无权限() {
        String token = jwtProvider.generateRefreshToken("u1", "dealer01", "DEALER");

        Claims claims = jwtProvider.validateToken(token);
        assertEquals(JwtProvider.TYPE_REFRESH, claims.get(JwtProvider.CLAIM_TOKEN_TYPE));
        assertTrue(jwtProvider.getPermissions(claims).isEmpty());
    }

    @Test
    void validateTokenOfType_类型不匹配抛异常() {
        String refresh = jwtProvider.generateRefreshToken("u1", "dealer01", "DEALER");
        // refresh token 冒充 access token 调业务接口 → 必须拒绝
        assertThrows(JwtException.class,
                () -> jwtProvider.validateTokenOfType(refresh, JwtProvider.TYPE_ACCESS));
    }

    @Test
    void validateToken_无效token抛异常() {
        assertThrows(JwtException.class, () -> jwtProvider.validateToken("not-a-jwt"));
    }

    @Test
    void isExpired_未过期返回false() {
        String token = jwtProvider.generateAccessToken("u1", "dealer01", "DEALER", List.of());
        assertFalse(jwtProvider.isExpired(token));
    }
}
