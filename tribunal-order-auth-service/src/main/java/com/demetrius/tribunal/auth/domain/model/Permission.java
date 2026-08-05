package com.demetrius.tribunal.auth.domain.model;

/**
 * 权限点（RBAC 授权的最小单位，如 order:review 审单）。
 *
 * <p>对应需求：F-901（用户/角色/权限，RBAC）、N-402（授权）。</p>
 */
public class Permission {

    private final String id;

    /** 权限码（如 order:review / order:cancel） */
    private final String permissionCode;

    private final String permissionName;

    /** 所属模块（ORDER/CUSTOMER/...） */
    private final String module;

    public Permission(String id, String permissionCode, String permissionName, String module) {
        this.id = id;
        this.permissionCode = permissionCode;
        this.permissionName = permissionName;
        this.module = module;
    }

    public String getId() {
        return id;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public String getModule() {
        return module;
    }
}