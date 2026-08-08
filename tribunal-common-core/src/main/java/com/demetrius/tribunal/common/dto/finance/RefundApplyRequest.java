package com.demetrius.tribunal.common.dto.finance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 退款申请请求（对应 PRD 4.4 POST /api/v1/refund/apply）。
 *
 * <p>退款金额必须 ≤ 实付金额 - 已退金额（FR-006/FIN-006）。</p>
 */
@Data
public class RefundApplyRequest {

    /** 原结算单号 */
    @NotBlank(message = "originalSettlementId 不能为空")
    private String originalSettlementId;

    /** 退款单号，如 REF_20260803_001 */
    @NotBlank(message = "refundId 不能为空")
    private String refundId;

    /** 退款类型：FULL 全额 / PARTIAL 部分 */
    private String refundType;

    /** 退款明细（部分退款时按 SKU） */
    private List<RefundItem> items;

    /** 退款原因 */
    private String reason;

    /** 原因码：USER_CANCEL/USER_RETURN/PRICE_DIFF/SYSTEM_ERROR */
    private String reasonCode;

    /**
     * 单个 SKU 退款明细。
     */
    @Data
    public static class RefundItem {

        private String skuId;

        private Integer quantity;

        /** 退款金额（精确到分） */
        private BigDecimal refundAmount;
    }
}
