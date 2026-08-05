package com.demetrius.tribunal.auth.application.service;

import com.demetrius.tribunal.auth.application.dto.AuthResult;
import com.demetrius.tribunal.auth.application.dto.LoginCommand;
import com.demetrius.tribunal.auth.domain.model.Permission;
import com.demetrius.tribunal.auth.domain.model.User;
import com.demetrius.tribunal.auth.domain.repository.PermissionRepository;
import com.demetrius.tribunal.auth.domain.repository.UserRepository;
import com.demetrius.tribunal.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthApplicationService 单元测试：登录/失败锁定/刷新/权限查询。
 */
@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PermissionRepository permissionRepository;

    private AuthApplicationService service;

    @BeforeEach
    void setUp() {
        JwtProvider jwtProvider = new JwtProvider(
                "tribunal-order-auth-secret-key-2026-0123456789abcdef", 7200000, 604800000);
        service = new AuthApplicationService(userRepository, permissionRepository,
                jwtProvider, new LoginFailGuard(3, 15), new RefreshTokenStore(604800000L));
    }

    @Test
    void 登录成功签发双Token并携带权限() {
        User user = new User("u1", "dealer01", "raw-pass", "DEALER");
        when(userRepository.findByUsername("dealer01")).thenReturn(Optional.of(user));
        when(permissionRepository.findByRoleCode("DEALER"))
                .thenReturn(List.of(new Permission("p1", "order:create", "下单", "ORDER")));

        AuthResult result = service.login(new LoginCommand("dealer01", "raw-pass"));

        assertNotNull(result.token());
        assertNotNull(result.refreshToken());
        assertEquals("u1", result.userId());
        assertEquals("DEALER", result.roleCode());
        assertEquals(List.of("order:create"), result.permissions());
    }

    @Test
    void 密码错误抛业务异常() {
        User user = new User("u1", "dealer01", "raw-pass", "DEALER");
        when(userRepository.findByUsername("dealer01")).thenReturn(Optional.of(user));

        assertThrows(BizException.class,
                () -> service.login(new LoginCommand("dealer01", "wrong-pass")));
    }

    @Test
    void 连续失败达阈值后锁定() {
        User user = new User("u1", "dealer01", "raw-pass", "DEALER");
        when(userRepository.findByUsername("dealer01")).thenReturn(Optional.of(user));

        // 前 3 次错误密码：每次登录都会抛业务异常（密码错误）
        assertThrows(BizException.class,
                () -> service.login(new LoginCommand("dealer01", "wrong")));   // 1
        assertThrows(BizException.class,
                () -> service.login(new LoginCommand("dealer01", "wrong")));   // 2
        assertThrows(BizException.class,
                () -> service.login(new LoginCommand("dealer01", "wrong")));   // 3 → 触发锁定

        // 第 4 次即使密码正确也被锁定拒绝
        BizException ex = assertThrows(BizException.class,
                () -> service.login(new LoginCommand("dealer01", "raw-pass")));
        assertTrue(ex.getMessage().contains("锁定"));
    }

    @Test
    void 刷新token轮换签发新双Token() {
        User user = new User("u1", "dealer01", "raw-pass", "DEALER");
        when(userRepository.findByUsername("dealer01")).thenReturn(Optional.of(user));
        when(permissionRepository.findByRoleCode("DEALER")).thenReturn(List.of());
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        String refresh = service.login(new LoginCommand("dealer01", "raw-pass")).refreshToken();
        AuthResult refreshed = service.refresh(refresh);

        assertNotNull(refreshed.token());
        assertNotNull(refreshed.refreshToken());
        assertNotEquals(refresh, refreshed.refreshToken(), "轮换后 refresh token 必须变更");
    }

    @Test
    void 无效refreshToken抛异常() {
        assertThrows(BizException.class, () -> service.refresh("invalid-refresh-token"));
    }

    @Test
    void 按角色查询权限() {
        when(permissionRepository.findByRoleCode("SALES"))
                .thenReturn(List.of(new Permission("p1", "order:review", "审单", "ORDER")));

        List<Permission> perms = service.getPermissionsByRole("SALES");
        assertEquals(1, perms.size());
        assertEquals("order:review", perms.get(0).getPermissionCode());
    }
}
