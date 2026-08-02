package com.demetrius.tribunal.fulfillment.domain.repository;

import com.demetrius.tribunal.fulfillment.domain.model.FulfillmentId;
import com.demetrius.tribunal.fulfillment.domain.model.FulfillmentOrder;

import java.util.Optional;

/**
 * 履约单仓储接口。
 */
public interface FulfillmentRepository {

    void save(FulfillmentOrder order);

    Optional<FulfillmentOrder> findById(FulfillmentId id);

    Optional<FulfillmentOrder> findBySourceOrderNo(String sourceOrderNo);
}
