package com.demetrius.tribunal.billing.domain.repository;

import com.demetrius.tribunal.billing.domain.model.FinanceBill;
import com.demetrius.tribunal.billing.domain.model.BillId;

import java.util.Optional;

/**
 * 金融账单订单仓储接口（domain 定义，infrastructure 实现）。
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>补充按 sourceOrderNo 查询（回传/对账用）</li>
 *   <li>补充履约列表分页查询</li>
 * </ul>
 */
public interface BillRepository {

    void save(FinanceBill order);

    Optional<FinanceBill> findById(BillId id);

    Optional<FinanceBill> findBySourceOrderNo(String sourceOrderNo);

    void delete(BillId id);
}
