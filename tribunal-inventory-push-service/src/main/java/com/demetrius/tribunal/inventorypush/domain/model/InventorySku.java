package com.demetrius.tribunal.inventorypush.domain.model;

import lombok.Getter;

/**
 * 库存主数据领域实体（对应 inventory_sku 表，PRD 5.1）。
 *
 * <p>区分总/可用/锁定/在途/预留多维度库存（PRD 2.3.1），版本号用于乐观锁与顺序控制。</p>
 */
@Getter
public class InventorySku {

    private final String id;

    /** 标准化 SKU 编码 */
    private final String skuId;

    /** 仓库编码 */
    private final String warehouseId;

    /** 货主编码 */
    private final String ownerId;

    private int totalQty;

    private int availableQty;

    private int lockedQty;

    private int inTransitQty;

    private int reservedQty;

    /** 乐观锁版本号（大版本覆盖小版本） */
    private long version;

    public InventorySku(String id, String skuId, String warehouseId, String ownerId,
                        int totalQty, int availableQty, int lockedQty,
                        int inTransitQty, int reservedQty, long version) {
        this.id = id;
        this.skuId = skuId;
        this.warehouseId = warehouseId;
        this.ownerId = ownerId;
        this.totalQty = totalQty;
        this.availableQty = availableQty;
        this.lockedQty = lockedQty;
        this.inTransitQty = inTransitQty;
        this.reservedQty = reservedQty;
        this.version = version;
    }

    /** 以推送快照覆盖各维度库存（可用库存为负时由应用层拦截为异常数据，此处仅赋值） */
    public void applySnapshot(int totalQty, int availableQty, int lockedQty,
                              int inTransitQty, int reservedQty, long newVersion) {
        this.totalQty = totalQty;
        this.availableQty = availableQty;
        this.lockedQty = lockedQty;
        this.inTransitQty = inTransitQty;
        this.reservedQty = reservedQty;
        this.version = newVersion;
    }
}
