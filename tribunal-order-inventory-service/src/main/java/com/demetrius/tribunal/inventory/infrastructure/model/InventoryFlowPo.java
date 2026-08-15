package com.demetrius.tribunal.inventory.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存变动流水持久化对象（对应 t_inventory_flow 表，审计 + 对账）。
 */
@Data
@TableName("t_inventory_flow")
public class InventoryFlowPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String skuCode;

    /** 变动类型（IN/OUT/RESERVE/RELEASE） */
    private String changeType;

    /** 变动数量 */
    private BigDecimal quantity;

    /** 来源单号（订单号等，可为空） */
    private String sourceNo;

    /** 变动时间 */
    private LocalDateTime createTime;
}
