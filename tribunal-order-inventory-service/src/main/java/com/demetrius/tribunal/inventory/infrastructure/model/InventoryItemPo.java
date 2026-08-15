package com.demetrius.tribunal.inventory.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存物料持久化对象（对应 t_inventory_item 表）。
 */
@Data
@TableName("t_inventory_item")
public class InventoryItemPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String skuCode;

    private String skuName;

    private String unit;

    /** 总库存 */
    private BigDecimal totalQuantity;

    /** 已预占 */
    private BigDecimal reservedQuantity;

    /** 乐观锁版本号（并发预占/释放防超卖） */
    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;
}
