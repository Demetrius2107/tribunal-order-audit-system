package com.demetrius.tribunal.order.domain.model;

import java.math.BigDecimal;

/**
 * 空包装回收明细（业务文档九节：returnable_packaging）。
 *
 * <p>经销商退回空包装物，订单可包含回收明细；回收参与订单金额/押金计算。</p>
 */
public class ReturnablePackaging {

    /**
     * 包装类型编码
     */
    private final String packagingType;

    /**
     * 包装类型名称
     */
    private final String packagingName;

    /**
     * 回收数量
     */
    private final BigDecimal quantity;

    /**
     * 单个包装押金
     */
    private final BigDecimal unitDeposit;

    /**
     * 押金合计 = 数量 × 单价押金
     */
    private final BigDecimal depositAmount;

    public ReturnablePackaging(String packagingType, String packagingName,
                               BigDecimal quantity, BigDecimal unitDeposit) {
        if (packagingType == null || packagingType.isBlank()) {
            throw new IllegalArgumentException("包装类型不能为空");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("回收数量必须大于0");
        }
        if (unitDeposit == null || unitDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("押金单价不能为负");
        }
        this.packagingType = packagingType;
        this.packagingName = packagingName;
        this.quantity = quantity;
        this.unitDeposit = unitDeposit;
        this.depositAmount = quantity.multiply(unitDeposit);
    }

    public String getPackagingType() {
        return packagingType;
    }

    public String getPackagingName() {
        return packagingName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitDeposit() {
        return unitDeposit;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }
}
