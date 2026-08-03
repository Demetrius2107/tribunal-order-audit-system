package com.demetrius.tribunal.financesettlement.domain.repository;

import com.demetrius.tribunal.financesettlement.domain.model.SettlementOrder;

import java.util.Optional;

/**
 * 结算单仓储接口（domain 定义，infrastructure 实现）。
 */
public interface SettlementOrderRepository {

    Optional<SettlementOrder> findBySettlementId(String settlementId);

    Optional<SettlementOrder> findByOrderId(String orderId);

    void save(SettlementOrder order);
}
