package com.demetrius.tribunal.order.infrastructure.listener;

import com.demetrius.tribunal.order.application.dto.BillingEventDto;
import com.demetrius.tribunal.order.application.service.BillStatusCallbackApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 账单状态事件消费者（M3 异步化：替代 Feign 同步回传）。
 *
 * <p>billing-service 生成/更新账单后发布到 billing-events 主题，
 * 本消费者接收后驱动订单状态机（CONFIRMED → BILLING_COMPLETED 等）。</p>
 *
 * <p>同一 orderId 的事件以 orderId 为 partition key，保证顺序消费。</p>
 */
@Component
public class BillingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BillingEventConsumer.class);

    public static final String BILLING_EVENTS_TOPIC = "billing-events";
    private static final String GROUP_ID = "order-service";

    private final BillStatusCallbackApplicationService callbackService;
    private final ObjectMapper objectMapper;

    public BillingEventConsumer(BillStatusCallbackApplicationService callbackService, ObjectMapper objectMapper) {
        this.callbackService = callbackService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = BILLING_EVENTS_TOPIC, groupId = GROUP_ID)
    public void onBillingEvent(String message) {
        try {
            BillingEventDto event = objectMapper.readValue(message, BillingEventDto.class);
            log.info("收到账单事件 orderId={} billId={} billStatus={}",
                    event.orderId(), event.billId(), event.billStatus());
            callbackService.handleCallback(event.orderId(), event.billStatus(), event.billId());
        } catch (Exception e) {
            log.error("处理账单事件失败 message={}", message, e);
            throw new RuntimeException("账单事件处理失败", e);
        }
    }
}
