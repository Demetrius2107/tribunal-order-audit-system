package com.demetrius.tribunal.financesettlement.domain.repository;

import com.demetrius.tribunal.financesettlement.domain.model.SettlementDetail;

import java.util.List;

/**
 * 结算明细仓储接口。
 */
public interface SettlementDetailRepository {

    List<SettlementDetail> findBySettlementId(String settlementId);

    void save(SettlementDetail detail);
}
