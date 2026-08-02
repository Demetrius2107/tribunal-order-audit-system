package com.demetrius.tribunal.order.domain.event;

import com.demetrius.tribunal.order.domain.model.OrderId;

import java.time.LocalDateTime;

/**
 * 订单创建领域事件。
 *
 * <p>对照旧项目：{@code unifySendMessage}（下单后异步发站内消息/通知）。
 * 旧项目是「业务代码直接调用通知逻辑」，DDD 做法是「发布事件，订阅者处理」，
 * 从而把订单业务与通知解耦。</p>
 *
 * <p>学习要点：领域事件是纯数据载体（POJO），不依赖 Spring。
 * 发布动作由应用层完成（ApplicationEventPublisher 或消息中间件）。</p>
 */
public record OrderCreatedEvent(
        OrderId orderId,
        String orderNo,
        String customerId,
        LocalDateTime occurredAt) {

    public OrderCreatedEvent {
        occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }
}
