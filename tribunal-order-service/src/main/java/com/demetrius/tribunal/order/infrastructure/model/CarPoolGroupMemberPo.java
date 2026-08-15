package com.demetrius.tribunal.order.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拼车组成员持久化对象（对应 t_car_pool_group_member 表，F-310）。
 */
@Data
@TableName("t_car_pool_group_member")
public class CarPoolGroupMemberPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 拼车组 ID */
    private String groupId;

    /** 成员订单编号 */
    private String orderNo;

    /** 加入时间 */
    private LocalDateTime joinTime;
}
