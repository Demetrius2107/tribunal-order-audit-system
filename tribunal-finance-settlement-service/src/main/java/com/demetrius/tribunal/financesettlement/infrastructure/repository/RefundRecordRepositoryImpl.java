package com.demetrius.tribunal.financesettlement.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.financesettlement.domain.model.RefundRecord;
import com.demetrius.tribunal.financesettlement.domain.repository.RefundRecordRepository;
import com.demetrius.tribunal.financesettlement.infrastructure.mapper.RefundRecordMapper;
import com.demetrius.tribunal.financesettlement.infrastructure.model.RefundRecordPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 退款记录仓储实现（infrastructure 层）。
 */
@Repository
public class RefundRecordRepositoryImpl implements RefundRecordRepository {

    private final RefundRecordMapper refundRecordMapper;

    public RefundRecordRepositoryImpl(RefundRecordMapper refundRecordMapper) {
        this.refundRecordMapper = refundRecordMapper;
    }

    @Override
    public Optional<RefundRecord> findByRefundId(String refundId) {
        RefundRecordPo po = refundRecordMapper.selectOne(
                new LambdaQueryWrapper<RefundRecordPo>()
                        .eq(RefundRecordPo::getRefundId, refundId));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public void save(RefundRecord record) {
        RefundRecordPo po = new RefundRecordPo();
        po.setId(record.getId());
        po.setRefundId(record.getRefundId());
        po.setSettlementId(record.getSettlementId());
        po.setOriginalOrderId(record.getOriginalOrderId());
        po.setRefundType(record.getRefundType());
        po.setRefundAmount(record.getRefundAmount());
        po.setReason(record.getReason());
        po.setReasonCode(record.getReasonCode());
        po.setStatus(record.getStatus());
        po.setApproverId(record.getApproverId());
        po.setChannelTransactionId(record.getChannelTransactionId());
        refundRecordMapper.insert(po);
    }

    private RefundRecord toDomain(RefundRecordPo po) {
        return new RefundRecord(
                po.getId(), po.getRefundId(), po.getSettlementId(), po.getOriginalOrderId(),
                po.getRefundType(), po.getRefundAmount(), po.getReason(), po.getReasonCode(),
                po.getStatus(), po.getApproverId(), po.getChannelTransactionId());
    }
}
