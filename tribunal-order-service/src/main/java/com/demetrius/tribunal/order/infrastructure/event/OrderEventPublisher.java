package com.demetrius.tribunal.order.infrastructure.event;

import com.demetrius.tribunal.order.application.dto.OrderEventMessage;
import com.demetrius.tribunal.order.infrastructure.mapper.OutboxMessageMapper;
import com.demetrius.tribunal.order.infrastructure.model.OutboxMessagePo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单事件发布器（M3 异步化：通过本地消息表事务性发布）。
 *
 * <p>写 outbox 表与业务事务原子提交 → OutboxRelayTask 轮询投递 Kafka topic: order-events。</p>
 * <p>下游消费方：finance-settlement（生成结算单）、billing-service（生成账单）、fulfillment-service（创建履约单）。</p>
 * <p>messageKey = orderId，保证同一订单的事件在同一 partition 顺序消费。</p>
 */
@Component
public class OrderEventPublisher {

    /** 订单事件主题（PRD 4.1） */
    public static final String ORDER_EVENTS_TOPIC = "order-events";

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final OutboxMessageMapper outboxMessageMapper;
    private final ObjectMapper objectMapper;

    public OrderEventPublisher(OutboxMessageMapper outboxMessageMapper, ObjectMapper objectMapper) {
        this.outboxMessageMapper = outboxMessageMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 发布订单事件（写入 outbox 表，relay 异步投递到 Kafka）。
     */
    public void publishOrderEvent(OrderEventMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);

            OutboxMessagePo po = new OutboxMessagePo();
            po.setMessageId(message.eventId());
            po.setTopic(ORDER_EVENTS_TOPIC);
            po.setMessageKey(message.orderId()); // orderId 作 partition key，保证顺序消费
            po.setPayload(payload);
            po.setStatus("PENDING");
            po.setRetryCount(0);
            po.setCreateTime(LocalDateTime.now());

            outboxMessageMapper.insert(po);
            log.info("已写入 outbox messageId={} eventType={} orderId={}",
                    message.eventId(), message.eventType(), message.orderId());
        } catch (JsonProcessingException e) {
            log.error("订单事件序列化失败 eventId={}", message.eventId(), e);
        }
    }
}
