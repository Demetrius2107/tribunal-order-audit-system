package com.demetrius.tribunal.erp.domain.event;

import com.demetrius.tribunal.erp.domain.model.ErpOrderId;
import com.demetrius.tribunal.erp.domain.model.ErpOrderStatus;

import java.time.LocalDateTime;

/**
 * ERP 履约状态变更事件。
 *
 * <p>对应需求：F-503（状态回传）、N-304（幂等）。</p>
 *
 * <p>用途：ERP 履约状态变更后发布事件，由应用层订阅并 Feign 回传 OMS（对照任务 #5）。</p>
 */
public record ErpOrderStatusChangedEvent(
        ErpOrderId erpOrderId,
        String sourceOrderNo,
        ErpOrderStatus from,
        ErpOrderStatus to,
        LocalDateTime occurredAt) {

    public ErpOrderStatusChangedEvent {
        occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }
}
