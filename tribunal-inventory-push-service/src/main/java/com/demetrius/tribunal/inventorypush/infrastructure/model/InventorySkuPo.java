package com.demetrius.tribunal.inventorypush.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 库存主数据持久化对象（对应 inventory_sku 表，PRD 5.1）。
 */
@Data
@TableName("inventory_sku")
public class InventorySkuPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 标准化 SKU 编码 */
    private String skuId;

    /** 仓库编码 */
    private String warehouseId;

    /** 货主编码 */
    private String ownerId;

    /** 总库存 */
    private Integer totalQty;

    /** 可用库存 */
    private Integer availableQty;

    /** 锁定库存 */
    private Integer lockedQty;

    /** 在途库存 */
    private Integer inTransitQty;

    /** 预留库存 */
    private Integer reservedQty;

    /** 乐观锁版本号 */
    private Long version;
}
