package com.demetrius.tribunal.financesettlement.domain.repository;

import com.demetrius.tribunal.financesettlement.domain.model.SplitRecord;

import java.util.List;

/**
 * 分账记录仓储接口。
 */
public interface SplitRecordRepository {

    List<SplitRecord> findBySettlementId(String settlementId);

    void save(SplitRecord record);
}
