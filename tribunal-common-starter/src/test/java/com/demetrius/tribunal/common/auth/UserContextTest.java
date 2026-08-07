package com.demetrius.tribunal.common.auth;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserContext 单元测试：上下文写入/读取/权限判断/清理（防线程池串号）。
 */
class UserContextTest {

    @Test
    void 写入后可读取用户信息() {
        UserContext.set(new UserContext.CurrentUser("u1", "dealer01", "DEALER", List.of("order:create")));

        assertEquals("u1", UserContext.getRequiredUserId());
        assertEquals("dealer01", UserContext.get().username());
        assertTrue(UserContext.hasPermission("order:create"));
        assertFalse(UserContext.hasPermission("order:review"));
    }

    @Test
    void 未登录时getRequiredUserId抛异常() {
        UserContext.clear();
        assertThrows(IllegalStateException.class, UserContext::getRequiredUserId);
        assertNull(UserContext.getUserId());
    }

    @Test
    void 清理后上下文消失() {
        UserContext.set(new UserContext.CurrentUser("u1", "dealer01", "DEALER", List.of()));
        UserContext.clear();

        assertNull(UserContext.getUserId());
        assertFalse(UserContext.hasPermission("order:create"));
    }
}
