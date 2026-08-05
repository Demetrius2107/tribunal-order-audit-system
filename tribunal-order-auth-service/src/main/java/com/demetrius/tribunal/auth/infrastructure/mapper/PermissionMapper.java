package com.demetrius.tribunal.auth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.auth.infrastructure.model.PermissionPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限点 Mapper。
 */
@Mapper
public interface PermissionMapper extends BaseMapper<PermissionPo> {

    /**
     * 按角色编码查询权限码列表（RBAC N:M 关联查询）。
     */
    @Select("""
            SELECT p.permission_code
            FROM t_role_permission rp
            JOIN t_permission p ON p.permission_code = rp.permission_code
            WHERE rp.role_code = #{roleCode}
            """)
    List<String> selectPermissionCodesByRole(@Param("roleCode") String roleCode);

    /**
     * 按角色编码查询完整权限点列表（RBAC N:M 关联查询）。
     */
    @Select("""
            SELECT p.id, p.permission_code, p.permission_name, p.module
            FROM t_role_permission rp
            JOIN t_permission p ON p.permission_code = rp.permission_code
            WHERE rp.role_code = #{roleCode}
            """)
    List<PermissionPo> selectByRoleCode(@Param("roleCode") String roleCode);
}