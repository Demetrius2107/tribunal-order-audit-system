package com.demetrius.tribunal.erp.domain.repository;

import com.demetrius.tribunal.erp.domain.model.ErpOrder;
import com.demetrius.tribunal.erp.domain.model.ErpOrderId;

import java.util.Optional;

/**
 * ERP 履约订单仓储接口（domain 定义，infrastructure 实现）。
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>补充按 sourceOrderNo 查询（回传/对账用）</li>
 *   <li>补充履约列表分页查询</li>
 * </ul>
 */
public interface ErpOrderRepository {

    void save(ErpOrder order);

    Optional<ErpOrder> findById(ErpOrderId id);

    Optional<ErpOrder> findBySourceOrderNo(String sourceOrderNo);

    void delete(ErpOrderId id);
}
