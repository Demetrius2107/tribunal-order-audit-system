package com.demetrius.tribunal.billing.infrastructure.event;

import com.demetrius.tribunal.billing.domain.event.BillStatusChangedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 账单状态事件 Kafka 发布器（M3 异步化：替代 Feign 同步回传）。
 *
 * <p>billing-service 生成/确认/结算账单后发布到 billing-events 主题，
 * order-service 的 BillingEventConsumer 接收后驱动订单状态机。</p>
 */
@Component
public class BillingEventPublisher {

    public static final String BILLING_EVENTS_TOPIC = "billing-events";

    private static final Logger log = LoggerFactory.getLogger(BillingEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public BillingEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 发布账单状态变更事件（orderId 为 partition key，保证同订单顺序消费）。
     */
    public void publishBillStatusChanged(BillStatusChangedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(new BillEventPayload(
                    UUID.randomUUID().toString().replace("-", ""),
                    event.sourceOrderNo(),
                    event.billId().value(),
                    event.to() != null ? event.to().name() : null,
                    null,
                    null,
                    System.currentTimeMillis()));

            kafkaTemplate.send(BILLING_EVENTS_TOPIC, event.sourceOrderNo(), payload);
            log.info("已发布账单状态事件 orderId={} billId={} status={}",
                    event.sourceOrderNo(), event.billId().value(),
                    event.to());
        } catch (JsonProcessingException e) {
            log.error("账单事件序列化失败 billId={}", event.billId(), e);
        }
    }

    /** billing-events 主题载荷（JSON 契约，与 order-service 的 BillingEventDto 字段名一致） */
    private record BillEventPayload(
            String eventId,
            String orderId,
            String billId,
            String billStatus,
            java.math.BigDecimal amount,
            String paymentMethod,
            long timestamp) {
    }
}
