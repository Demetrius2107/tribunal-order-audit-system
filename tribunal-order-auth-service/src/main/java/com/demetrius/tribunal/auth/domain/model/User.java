package com.demetrius.tribunal.auth.domain.model;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 用户聚合根。
 *
 * <p>对应需求：F-901（用户/角色/权限，RBAC）、N-401（认证）。</p>
 *
 * <p>密码使用 BCrypt 加密存储，登录时用 BCrypt 校验。</p>
 */
public class User {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final String id;

    private final String username;

    /** 密码密文（BCrypt 加密存储） */
    private String password;

    private final String roleCode;

    /**
     * 创建新用户：密码自动 BCrypt 加密。
     */
    public User(String id, String username, String rawPassword, String roleCode) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        this.id = id;
        this.username = username;
        this.password = ENCODER.encode(rawPassword);
        this.roleCode = roleCode;
    }

    /**
     * 校验密码（BCrypt 比对）。
     */
    public boolean verifyPassword(String rawPassword) {
        return password != null && ENCODER.matches(rawPassword, password);
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRoleCode() {
        return roleCode;
    }
}
