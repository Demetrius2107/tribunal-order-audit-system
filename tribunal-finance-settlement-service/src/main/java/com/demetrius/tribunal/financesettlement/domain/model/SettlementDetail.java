package com.demetrius.tribunal.financesettlement.domain.model;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 结算明细实体（对应 settlement_detail 表，PRD 5.1）。
 *
 * <p>明细项类型：GOODS/SHIPPING/DISCOUNT/TAX/PLATFORM_FEE/PAYMENT_FEE（PRD 2.1.2 FR-004）。</p>
 */
@Getter
public class SettlementDetail {

    private final String id;

    private final String settlementId;

    /** 明细项类型 */
    private final String itemType;

    private final String skuId;

    private final String skuName;

    private final Integer quantity;

    private final BigDecimal unitPrice;

    /** 原始金额 */
    private final BigDecimal originalAmount;

    /** 实际金额（优惠分摊后，FR-005 精确到分） */
    private final BigDecimal actualAmount;

    private final String description;

    public SettlementDetail(String id, String settlementId, String itemType, String skuId, String skuName,
                            Integer quantity, BigDecimal unitPrice, BigDecimal originalAmount,
                            BigDecimal actualAmount, String description) {
        this.id = id;
        this.settlementId = settlementId;
        this.itemType = itemType;
        this.skuId = skuId;
        this.skuName = skuName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.originalAmount = originalAmount;
        this.actualAmount = actualAmount;
        this.description = description;
    }
}
