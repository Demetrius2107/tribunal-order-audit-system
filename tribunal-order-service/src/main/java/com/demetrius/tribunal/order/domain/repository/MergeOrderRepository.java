package com.demetrius.tribunal.order.domain.repository;

import com.demetrius.tribunal.order.domain.model.MergeOrder;

import java.util.List;
import java.util.Optional;

/**
 * 合单仓储接口（领域层定义契约，基础设施层实现）。
 */
public interface MergeOrderRepository {

    void save(MergeOrder mergeOrder);

    Optional<MergeOrder> findById(String id);

    Optional<MergeOrder> findByMergeNo(String mergeNo);

    /** 查询客户的所有合单（分页简化版） */
    List<MergeOrder> findByCustomerId(String customerId);

    /** 查询某订单参与的合单 */
    Optional<MergeOrder> findByMemberOrderId(String orderId);
}
