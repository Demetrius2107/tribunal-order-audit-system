package com.demetrius.tribunal.order.domain.event;

import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.model.OrderStatus;

import java.time.LocalDateTime;

/**
 * 订单状态变更领域事件。
 *
 * <p>对照旧项目：{@code OrderStatusProcessRecord}（状态流水）、
 * {@code asynchronousMessageSending}（状态变更后异步发消息）、
 * RabbitMqReceiverHandler（消费通知队列）。</p>
 *
 * <p>订阅者可以做的事：</p>
 * <ul>
 *   <li>记录状态流水（审计）</li>
 *   <li>发送邮件/微信/站内信（通知解耦）</li>
 *   <li>驱动下游流程（如转单、发货触发）——跨领域协作的事件驱动雏形</li>
 * </ul>
 */
public record OrderStatusChangedEvent(
        OrderId orderId,
        String orderNo,
        OrderStatus from,
        OrderStatus to,
        LocalDateTime occurredAt) {

    public OrderStatusChangedEvent {
        occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }
}
