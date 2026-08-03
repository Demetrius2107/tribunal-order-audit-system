package com.demetrius.tribunal.auth.domain.model;

/**
 * 用户聚合根。
 *
 * <p>对应需求：F-901（用户/角色/权限，RBAC）、N-401（认证）。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>密码加密存储（BCrypt），登录时校验</li>
 *   <li>Token 签发（JWT：过期/刷新），对照 N-401</li>
 *   <li>角色-权限关联与接口级鉴权（RBAC）</li>
 * </ul>
 */
public class User {

    private final String id;

    private final String username;

    /** 密码密文（TODO：BCrypt 加密存储） */
    private String password;

    private final String roleCode;

    public User(String id, String username, String password, String roleCode) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        this.id = id;
        this.username = username;
        this.password = password;
        this.roleCode = roleCode;
    }

    /**
     * 校验密码（骨架：明文比对；TODO：BCrypt matches）。
     */
    public boolean verifyPassword(String rawPassword) {
        return password != null && password.equals(rawPassword);
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
