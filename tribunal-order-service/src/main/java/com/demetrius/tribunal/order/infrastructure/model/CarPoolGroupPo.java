package com.demetrius.tribunal.order.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拼车组持久化对象（对应 t_car_pool_group 表，F-310）。
 */
@Data
@TableName("t_car_pool_group")
public class CarPoolGroupPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 拼车组编号（业务唯一键） */
    private String groupNo;

    /** 状态：OPEN/CONFIRMED/CLOSED/CANCELLED（与 CarPoolGroupStatus.name() 对应） */
    private String status;

    /** 成员订单数 */
    private Integer memberCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
