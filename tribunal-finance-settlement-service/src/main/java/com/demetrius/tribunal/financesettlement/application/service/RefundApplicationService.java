package com.demetrius.tribunal.financesettlement.application.service;

import com.demetrius.tribunal.common.dto.finance.RefundApplyRequest;
import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.financesettlement.domain.model.RefundRecord;
import com.demetrius.tribunal.financesettlement.domain.model.SettlementOrder;
import com.demetrius.tribunal.financesettlement.domain.repository.RefundRecordRepository;
import com.demetrius.tribunal.financesettlement.domain.repository.SettlementOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 退款应用服务（PRD 2.4 退款/冲正层）。
 *
 * <p>逆向流程：退款申请 → 资格校验 → 人工审核（大额） → 分账回退 → 渠道退款（PRD 6.3）。</p>
 *
 * <p>基建说明：当前实现退款资格与金额校验骨架，分账资金回退、渠道退款调用留待后续填充。</p>
 */
@Service
public class RefundApplicationService {

    /** 大额退款人工审核阈值（分），默认 5000 元（PRD 2.4.3 FR-046，待确认事项建议值） */
    private static final BigDecimal LARGE_REFUND_THRESHOLD = new BigDecimal("500000");

    private final RefundRecordRepository refundRecordRepository;
    private final SettlementOrderRepository settlementOrderRepository;

    public RefundApplicationService(RefundRecordRepository refundRecordRepository,
                                    SettlementOrderRepository settlementOrderRepository) {
        this.refundRecordRepository = refundRecordRepository;
        this.settlementOrderRepository = settlementOrderRepository;
    }

    /**
     * 退款申请（PRD 4.4 POST /api/v1/refund/apply）。
     */
    @Transactional
    public void apply(RefundApplyRequest request) {
        if (refundRecordRepository.findByRefundId(request.getRefundId()).isPresent()) {
            throw new BizException("FIN-001", "退款单已存在: " + request.getRefundId());
        }
        SettlementOrder order = settlementOrderRepository
                .findBySettlementId(request.getOriginalSettlementId())
                .orElseThrow(() -> new BizException("FIN-001", "原结算单不存在: " + request.getOriginalSettlementId()));

        // 退款金额校验：退款金额 ≤ 实付金额（FIN-006；部分退款场景下还需扣减已退金额）
        BigDecimal refundAmount = request.getItems().stream()
                .map(RefundApplyRequest.RefundItem::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (refundAmount.compareTo(order.getNetAmount()) > 0) {
            throw new BizException("FIN-006", "退款金额超过可退金额");
        }

        // 大额退款人工审核（FR-046）：超过阈值进入 PENDING 待审核，否则直接 APPROVED
        String status = refundAmount.compareTo(LARGE_REFUND_THRESHOLD) >= 0 ? "PENDING" : "APPROVED";

        RefundRecord record = new RefundRecord(
                UUID.randomUUID().toString().replace("-", ""),
                request.getRefundId(), request.getOriginalSettlementId(), order.getOrderId(),
                request.getRefundType() == null ? "FULL" : request.getRefundType(),
                refundAmount, request.getReason(), request.getReasonCode(),
                status, null, null);
        refundRecordRepository.save(record);
    }
}
