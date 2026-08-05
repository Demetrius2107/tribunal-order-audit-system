package com.demetrius.tribunal.auth.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RefreshTokenStore 单元测试：签发/校验/轮换/吊销。
 */
class RefreshTokenStoreTest {

    private RefreshTokenStore store;

    @BeforeEach
    void setUp() {
        store = new RefreshTokenStore(604800000L);
    }

    @Test
    void 签发后可校验取回身份() {
        String token = store.issue("u1", "dealer01", "DEALER");

        assertEquals("u1", store.getUserId(token));
        assertEquals("dealer01", store.getUsername(token));
        assertEquals("DEALER", store.getRoleCode(token));
    }

    @Test
    void 轮换后旧token作废() {
        String oldToken = store.issue("u1", "dealer01", "DEALER");
        String newToken = store.issue("u1", "dealer01", "DEALER"); // 轮换

        assertNull(store.getUserId(oldToken), "轮换后旧 refresh token 必须作废（防重放）");
        assertEquals("u1", store.getUserId(newToken));
    }

    @Test
    void 吊销后不可用() {
        String token = store.issue("u1", "dealer01", "DEALER");
        store.revoke(token);
        assertNull(store.getUserId(token));
    }

    @Test
    void 不同用户互不影响且可踢下线() {
        String u1Token = store.issue("u1", "dealer01", "DEALER");
        store.issue("u2", "sales01", "SALES");

        store.revokeByUser("u1");
        assertNull(store.getUserId(u1Token));
        assertEquals(1, store.activeCount());
    }

    @Test
    void 空token校验返回null() {
        assertNull(store.getUserId(null));
        assertNull(store.getUserId(""));
    }
}
