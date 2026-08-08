package com.demetrius.tribunal.marketing.domain.model;

import java.math.BigDecimal;

/**
 * 押金规则聚合根（M4 押金引擎）。
 *
 * <p>对应需求 F-205：按包装类型配置押金，决定是否额外计入应付金额。</p>
 *
 * <p>字段语义：</p>
 * <ul>
 *   <li>{@code skuCode} + {@code packagingType} 组合定位一条押金规则</li>
 *   <li>{@code unitDeposit} 每个销售单位的押金</li>
 *   <li>{@code includedInPrice} = true 时押金已含在售价中（不再额外加收）；
 *       = false 时押金额外加收到应付金额</li>
 * </ul>
 */
public class DepositRule {

    private final String id;
    private final String skuCode;
    private final PackagingType packagingType;
    private final BigDecimal unitDeposit;
    private final boolean includedInPrice;
    private final boolean active;

    public DepositRule(String id, String skuCode, PackagingType packagingType,
                       BigDecimal unitDeposit, boolean includedInPrice, boolean active) {
        if (skuCode == null || skuCode.isBlank()) {
            throw new IllegalArgumentException("SKU 编码不能为空");
        }
        if (packagingType == null) {
            throw new IllegalArgumentException("包装类型不能为空");
        }
        if (unitDeposit == null || unitDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("单件押金不能为负");
        }
        this.id = id;
        this.skuCode = skuCode;
        this.packagingType = packagingType;
        this.unitDeposit = unitDeposit;
        this.includedInPrice = includedInPrice;
        this.active = active;
    }

    public String getId() { return id; }
    public String getSkuCode() { return skuCode; }
    public PackagingType getPackagingType() { return packagingType; }
    public BigDecimal getUnitDeposit() { return unitDeposit; }
    public boolean isIncludedInPrice() { return includedInPrice; }
    public boolean isActive() { return active; }
}
