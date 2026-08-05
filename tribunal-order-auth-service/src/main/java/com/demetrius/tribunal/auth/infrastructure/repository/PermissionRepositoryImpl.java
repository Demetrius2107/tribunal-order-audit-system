package com.demetrius.tribunal.auth.infrastructure.repository;

import com.demetrius.tribunal.auth.domain.model.Permission;
import com.demetrius.tribunal.auth.domain.repository.PermissionRepository;
import com.demetrius.tribunal.auth.infrastructure.mapper.PermissionMapper;
import com.demetrius.tribunal.auth.infrastructure.model.PermissionPo;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 权限仓储实现（MyBatis-Plus）。
 */
@Repository
public class PermissionRepositoryImpl implements PermissionRepository {

    private final PermissionMapper permissionMapper;

    public PermissionRepositoryImpl(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    @Override
    public List<Permission> findByRoleCode(String roleCode) {
        return permissionMapper.selectByRoleCode(roleCode).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Permission> findAll() {
        return permissionMapper.selectList(null).stream()
                .map(this::toDomain)
                .toList();
    }

    private Permission toDomain(PermissionPo po) {
        return new Permission(po.getId(), po.getPermissionCode(), po.getPermissionName(), po.getModule());
    }
}