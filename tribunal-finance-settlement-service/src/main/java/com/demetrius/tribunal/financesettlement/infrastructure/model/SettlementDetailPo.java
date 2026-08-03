package com.demetrius.tribunal.financesettlement.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算明细表持久化对象（对应 settlement_detail 表，PRD 5.1）。
 */
@Data
@TableName("settlement_detail")
public class SettlementDetailPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String settlementId;

    /** 明细项类型：GOODS/SHIPPING/DISCOUNT/TAX/PLATFORM_FEE/PAYMENT_FEE */
    private String itemType;

    private String skuId;

    private String skuName;

    private Integer quantity;

    private BigDecimal unitPrice;

    /** 原始金额 */
    private BigDecimal originalAmount;

    /** 实际金额（分摊后） */
    private BigDecimal actualAmount;

    private String description;
}
