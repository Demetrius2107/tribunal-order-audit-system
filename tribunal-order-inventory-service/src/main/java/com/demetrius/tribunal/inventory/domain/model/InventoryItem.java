package com.demetrius.tribunal.inventory.domain.model;

import java.math.BigDecimal;

/**
 * 库存物料聚合根（物料主数据 + 库存账）。
 *
 * <p>对应需求：F-501（库存查询）、F-502（库存预占/释放）、库存物料推送上游。</p>
 *
 * <p>职责：</p>
 * <ul>
 *   <li>物料主数据：SKU 编码/名称/单位</li>
 *   <li>库存账：总库存、已预占、可售量（available = total - reserved）</li>
 *   <li>预占/释放：下单预占、取消/签收释放（业务规则内聚在聚合内部）</li>
 * </ul>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>安全库存/预警线：低于阈值告警（库存管理细化）</li>
 *   <li>多仓库/多库位：库存维度扩展</li>
 *   <li>库存变动流水：每次预占/释放记录流水（审计 + 对账）</li>
 * </ul>
 */
public class InventoryItem {

    private final InventoryItemId id;

    private final String skuCode;

    private final String skuName;

    private final String unit;

    /** 总库存 */
    private BigDecimal totalQuantity;

    /** 已预占数量 */
    private BigDecimal reservedQuantity;

    public InventoryItem(InventoryItemId id, String skuCode, String skuName,
                         String unit, BigDecimal totalQuantity, BigDecimal reservedQuantity) {
        if (skuCode == null || skuCode.isBlank()) {
            throw new IllegalArgumentException("SKU编码不能为空");
        }
        if (totalQuantity == null || totalQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("总库存不能为负");
        }
        this.id = id;
        this.skuCode = skuCode;
        this.skuName = skuName;
        this.unit = unit;
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = reservedQuantity == null ? BigDecimal.ZERO : reservedQuantity;
    }

    /** 可售量 = 总库存 - 已预占 */
    public BigDecimal availableQuantity() {
        return totalQuantity.subtract(reservedQuantity);
    }

    /**
     * 预占库存（下单时调用）。
     *
     * @param quantity 预占数量
     * @throws IllegalStateException 可售量不足
     */
    public void reserve(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("预占数量必须大于0");
        }
        if (availableQuantity().compareTo(quantity) < 0) {
            throw new IllegalStateException(
                    "库存不足: 可售 " + availableQuantity() + ", 需预占 " + quantity);
        }
        this.reservedQuantity = reservedQuantity.add(quantity);
    }

    /**
     * 释放预占（取消/签收时调用）。
     *
     * @param quantity 释放数量
     */
    public void release(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("释放数量必须大于0");
        }
        if (reservedQuantity.compareTo(quantity) < 0) {
            throw new IllegalStateException("释放数量超过已预占数量");
        }
        this.reservedQuantity = reservedQuantity.subtract(quantity);
    }

    /**
     * 退货入库（售后退货完成时调用）。
     *
     * <p>退货入库只增加总库存，不影响预占数量。
     * 商品退回仓库后重新可售。</p>
     *
     * @param quantity 退货数量
     */
    public void returnStock(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("退货数量必须大于0");
        }
        this.totalQuantity = totalQuantity.add(quantity);
    }

    // ---------- getters ----------

    public InventoryItemId getId() {
        return id;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public String getSkuName() {
        return skuName;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public BigDecimal getReservedQuantity() {
        return reservedQuantity;
    }
}
