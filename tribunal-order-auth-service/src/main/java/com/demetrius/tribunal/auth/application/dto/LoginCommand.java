package com.demetrius.tribunal.auth.application.dto;

/**
 * 登录入参。
 */
public record LoginCommand(
        String username,
        String password) {
}
