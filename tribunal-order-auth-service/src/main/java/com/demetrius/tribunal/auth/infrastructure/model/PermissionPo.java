package com.demetrius.tribunal.auth.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 权限点持久化对象（对应 t_permission 表）。
 */
@Data
@TableName("t_permission")
public class PermissionPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String permissionCode;

    private String permissionName;

    private String module;
}