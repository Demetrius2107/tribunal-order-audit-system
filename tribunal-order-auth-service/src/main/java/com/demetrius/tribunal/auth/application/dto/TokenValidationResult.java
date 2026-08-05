package com.demetrius.tribunal.auth.application.dto;

import java.util.List;

/**
 * Token 校验结果 DTO（供网关/过滤器/RBAC 鉴权使用）。
 */
public record TokenValidationResult(
        boolean valid,
        boolean expired,
        String userId,
        String username,
        String roleCode,
        List<String> permissions) {
}