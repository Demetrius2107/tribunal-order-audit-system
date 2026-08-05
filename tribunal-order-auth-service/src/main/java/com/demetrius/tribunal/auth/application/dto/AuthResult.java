package com.demetrius.tribunal.auth.application.dto;

import java.util.List;

/**
 * 登录/刷新出参（Access + Refresh 双 Token）。
 */
public record AuthResult(
        /** Access Token（业务接口鉴权用，短时效） */
        String token,
        /** Refresh Token（换取新 Access Token 用，长时效） */
        String refreshToken,
        String userId,
        String username,
        String roleCode,
        /** 该角色拥有的权限码列表（RBAC） */
        List<String> permissions) {
}