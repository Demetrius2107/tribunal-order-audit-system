package com.demetrius.tribunal.auth.interfaces.controller;

import com.demetrius.tribunal.auth.application.dto.AuthResult;
import com.demetrius.tribunal.auth.application.dto.LoginCommand;
import com.demetrius.tribunal.auth.application.dto.TokenValidationResult;
import com.demetrius.tribunal.auth.application.service.AuthApplicationService;
import com.demetrius.tribunal.auth.domain.model.Permission;
import com.demetrius.tribunal.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证授权接口层（REST）。
 *
 * <p>对应需求：F-901（用户/角色/权限）、N-401（认证）。</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    /**
     * 登录：POST /api/auth/login
     */
    @PostMapping("/login")
    public ApiResponse<AuthResult> login(@Valid @RequestBody LoginCommand command) {
        return ApiResponse.ok(authApplicationService.login(command));
    }

    /**
     * 注册：POST /api/auth/register
     */
    @PostMapping("/register")
    public ApiResponse<AuthResult> register(@RequestParam String username,
                                            @RequestParam String password,
                                            @RequestParam(defaultValue = "DEALER") String roleCode) {
        return ApiResponse.ok(authApplicationService.register(username, password, roleCode));
    }

    /**
     * Token 校验：GET /api/auth/validate?token=xxx
     * <p>供网关/过滤器/RBAC 鉴权调用，返回 userId/username/roleCode/permissions。</p>
     */
    @GetMapping("/validate")
    public ApiResponse<TokenValidationResult> validateToken(@RequestParam String token) {
        return ApiResponse.ok(authApplicationService.validateToken(token));
    }

    /**
     * 刷新 Token：POST /api/auth/refresh?refreshToken=xxx
     * <p>Refresh Token 轮换：签发新双 Token，旧 refresh 立即作废（防重放）。</p>
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResult> refresh(@RequestParam String refreshToken) {
        return ApiResponse.ok(authApplicationService.refresh(refreshToken));
    }

    /**
     * 登出：POST /api/auth/logout?refreshToken=xxx
     * <p>吊销该用户全部 Refresh Token（Access Token 靠过期自然失效）。</p>
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestParam String refreshToken) {
        authApplicationService.logout(refreshToken);
        return ApiResponse.ok(null);
    }

    /**
     * 按角色查询权限点：GET /api/auth/permissions?roleCode=DEALER
     */
    @GetMapping("/permissions")
    public ApiResponse<List<Permission>> getPermissionsByRole(@RequestParam String roleCode) {
        return ApiResponse.ok(authApplicationService.getPermissionsByRole(roleCode));
    }

    /**
     * 心跳接口（运维探活）。
     */
    @GetMapping("/heartbeat")
    public ApiResponse<String> heartbeat() {
        return ApiResponse.ok("UP");
    }
}
