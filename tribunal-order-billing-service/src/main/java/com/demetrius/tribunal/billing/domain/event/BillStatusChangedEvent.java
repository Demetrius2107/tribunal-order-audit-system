package com.demetrius.tribunal.billing.domain.event;

import com.demetrius.tribunal.billing.domain.model.BillId;
import com.demetrius.tribunal.billing.domain.model.BillStatus;

import java.time.LocalDateTime;

/**
 * 金融账单状态变更事件。
 *
 * <p>对应需求：F-503（状态回传）、N-304（幂等）。</p>
 *
 * <p>用途：金融账单状态变更后发布事件，由应用层订阅并 Feign 回传 订单服务（对照任务 #5）。</p>
 */
public record BillStatusChangedEvent(
        BillId billId,
        String sourceOrderNo,
        BillStatus from,
        BillStatus to,
        LocalDateTime occurredAt) {

    public BillStatusChangedEvent {
        occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }
}
