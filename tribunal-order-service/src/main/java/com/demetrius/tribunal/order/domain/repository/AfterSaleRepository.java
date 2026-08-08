package com.demetrius.tribunal.order.domain.repository;

import com.demetrius.tribunal.order.domain.model.AfterSale;
import java.util.List;
import java.util.Optional;

/**
 * 售后单仓储接口（领域层定义契约，基础设施层实现）。
 */
public interface AfterSaleRepository {

    void save(AfterSale afterSale);

    Optional<AfterSale> findById(String id);

    Optional<AfterSale> findByAfterSaleNo(String afterSaleNo);

    /** 查询订单的所有售后单 */
    List<AfterSale> findByOrderId(String orderId);

    /** 查询客户的所有售后单（分页简化版） */
    List<AfterSale> findByCustomerId(String customerId);
}
