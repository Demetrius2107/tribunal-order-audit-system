package com.demetrius.tribunal.common.dto.inventory;

import lombok.Data;

/**
 * 库存快照（各维度库存数量，对应 PRD 2.3.1 多维度库存管理）。
 */
@Data
public class InventorySnapshot {

    /** 总库存（物理在库总量） */
    private Integer totalQty;

    /** 可用库存 = 总库存 - 锁定 - 预留 */
    private Integer availableQty;

    /** 锁定库存（已审单未发货占用） */
    private Integer lockedQty;

    /** 在途库存 */
    private Integer inTransitQty;

    /** 预留库存 */
    private Integer reservedQty;
}
