package com.demetrius.tribunal.common.dto.finance;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 分账执行请求（对应 PRD 4.3 POST /api/v1/settlement/split）。
 *
 * <p>分账比例之和必须 = 100%（FR-025），误差由平台方吸收。</p>
 */
@Data
public class SplitRequest {

    /** 结算单号 */
    private String settlementId;

    /** 分账总金额 */
    private BigDecimal totalAmount;

    /** 分账明细（各收款方） */
    private java.util.List<SplitItem> splits;

    /**
     * 单个分账方明细。
     */
    @Data
    public static class SplitItem {

        /** 收款方 ID（商家/平台/物流/分销商） */
        private String recipientId;

        /** 收款方类型：MERCHANT/PLATFORM/LOGISTICS/AGENT */
        private String recipientType;

        /** 分账金额 */
        private BigDecimal amount;

        /** 分账说明 */
        private String description;
    }
}
