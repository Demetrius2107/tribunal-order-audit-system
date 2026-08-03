package com.demetrius.tribunal.marketing.domain.model;

import java.math.BigDecimal;

/**
 * 押金规则聚合根。
 *
 * <p>对应需求：F-205（按包装类型配置的押金，可配置是否计入价格）。</p>
 */
public class DepositRule {

    private final String id;

    private final String skuCode;

    /** 押金类型编码（按包装类型配置） */
    private final String depositType;

    private final BigDecimal depositAmount;

    /** 是否计入价格 */
    private final boolean includedInPrice;

    public DepositRule(String id, String skuCode, String depositType,
                       BigDecimal depositAmount, boolean includedInPrice) {
        if (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("押金金额不能为负");
        }
        this.id = id;
        this.skuCode = skuCode;
        this.depositType = depositType;
        this.depositAmount = depositAmount;
        this.includedInPrice = includedInPrice;
    }

    public String getId() {
        return id;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public String getDepositType() {
        return depositType;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public boolean isIncludedInPrice() {
        return includedInPrice;
    }
}
