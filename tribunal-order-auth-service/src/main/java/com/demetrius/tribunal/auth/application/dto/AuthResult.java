package com.demetrius.tribunal.auth.application.dto;

/**
 * 登录出参（Token 信息）。
 */
public record AuthResult(
        String token,
        String userId,
        String username,
        String roleCode) {
}
