package com.demetrius.tribunal.auth.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户持久化对象（对应 t_user 表）。
 */
@Data
@TableName("t_user")
public class UserPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String username;

    private String password;

    /** 角色编码（RBAC） */
    private String roleCode;
}
