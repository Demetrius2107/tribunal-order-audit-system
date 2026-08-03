package com.demetrius.tribunal.auth.interfaces.controller;

import com.demetrius.tribunal.auth.application.dto.AuthResult;
import com.demetrius.tribunal.auth.application.dto.LoginCommand;
import com.demetrius.tribunal.auth.application.service.AuthApplicationService;
import com.demetrius.tribunal.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 认证授权接口层（REST）。
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>Token 校验接口（供网关/过滤器调用）</li>
 *   <li>用户/角色/权限管理接口</li>
 * </ul>
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
     * 心跳接口（运维探活）。
     */
    @GetMapping("/heartbeat")
    public ApiResponse<String> heartbeat() {
        return ApiResponse.ok("UP");
    }
}
