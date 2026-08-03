package com.demetrius.tribunal.financesettlement.domain.repository;

import com.demetrius.tribunal.financesettlement.domain.model.RefundRecord;

import java.util.Optional;

/**
 * 退款记录仓储接口。
 */
public interface RefundRecordRepository {

    Optional<RefundRecord> findByRefundId(String refundId);

    void save(RefundRecord record);
}
