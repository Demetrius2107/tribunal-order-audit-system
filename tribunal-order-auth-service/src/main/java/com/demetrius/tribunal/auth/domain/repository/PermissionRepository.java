package com.demetrius.tribunal.auth.domain.repository;

import com.demetrius.tribunal.auth.domain.model.Permission;

import java.util.List;

/**
 * 权限仓储接口（RBAC）。
 */
public interface PermissionRepository {

    /**
     * 查询指定角色拥有的全部权限码（跨 t_role_permission + t_permission）。
     */
    List<Permission> findByRoleCode(String roleCode);

    /**
     * 查询全部权限点（权限管理用）。
     */
    List<Permission> findAll();
}