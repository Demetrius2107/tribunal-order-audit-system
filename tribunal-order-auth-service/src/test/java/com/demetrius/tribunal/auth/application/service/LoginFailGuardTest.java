package com.demetrius.tribunal.auth.application.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginFailGuard 单元测试：失败计数/锁定阈值/重置。
 */
class LoginFailGuardTest {

    private LoginFailGuard newGuard(int maxFailures, long lockMinutes) {
        return new LoginFailGuard(maxFailures, lockMinutes);
    }

    @Test
    void 连续失败达到阈值后锁定() {
        LoginFailGuard guard = newGuard(3, 15);

        assertFalse(guard.isLocked("user1"));
        assertFalse(guard.recordFailure("user1")); // 第 1 次
        assertFalse(guard.recordFailure("user1")); // 第 2 次
        assertTrue(guard.recordFailure("user1"));  // 第 3 次 → 达到阈值
        assertTrue(guard.isLocked("user1"));
    }

    @Test
    void 未达阈值不锁定() {
        LoginFailGuard guard = newGuard(5, 15);

        assertFalse(guard.recordFailure("user1"));
        assertFalse(guard.recordFailure("user1"));
        assertFalse(guard.isLocked("user1"));
        assertEquals(3, guard.remainingAttempts("user1"));
    }

    @Test
    void 成功后重置计数() {
        LoginFailGuard guard = newGuard(3, 15);

        guard.recordFailure("user1");
        guard.recordFailure("user1");
        guard.reset("user1");
        assertFalse(guard.isLocked("user1"));
        assertFalse(guard.recordFailure("user1")); // 重置后重新计数
    }

    @Test
    void 不同用户名互不影响() {
        LoginFailGuard guard = newGuard(2, 15);

        guard.recordFailure("userA");
        assertTrue(guard.recordFailure("userA"));
        assertTrue(guard.isLocked("userA"));
        assertFalse(guard.isLocked("userB"));
    }
}
