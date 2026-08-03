package com.demetrius.tribunal.order.infrastructure.event;

import com.demetrius.tribunal.order.application.dto.OrderEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单事件发布器（向 Kafka topic: order-events 发布事件）。
 *
 * <p>下游金融结算系统按 PRD 4.1 订阅该 topic 生成结算单；事件采用 JSON 字符串传输，
 * 两侧各自维护 DTO，字段名作为跨系统契约。</p>
 *
 * <p>基建说明：事件触发点（订单状态机 OrderCompleted 等处调用 publishOrderEvent）
 * 留待与审单/状态机业务对接时接入。</p>
 */
@Component
public class OrderEventPublisher {

    /** 订单事件主题（PRD 4.1） */
    public static final String ORDER_EVENTS_TOPIC = "order-events";

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 发布订单事件（eventId 为幂等键，下游按订单号幂等）。
     */
    public void publishOrderEvent(OrderEventMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(ORDER_EVENTS_TOPIC, message.eventId(), payload);
            log.info("已发布订单事件 eventId={} eventType={} orderId={}",
                    message.eventId(), message.eventType(), message.orderId());
        } catch (JsonProcessingException e) {
            log.error("订单事件序列化失败 eventId={}", message.eventId(), e);
        }
    }
}
