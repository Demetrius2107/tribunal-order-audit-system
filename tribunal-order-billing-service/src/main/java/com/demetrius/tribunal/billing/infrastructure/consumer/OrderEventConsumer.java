package com.demetrius.tribunal.billing.infrastructure.consumer;

import com.demetrius.tribunal.billing.application.dto.BillReceiveCommand;
import com.demetrius.tribunal.billing.application.service.BillingApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单事件消费者（M3 异步化：消费 OrderApproved 事件生成账单）。
 *
 * <p>消费组 billing-service 独立于 finance-settlement / fulfillment，各消费组各自消费全量消息。</p>
 * <p>同一 orderId 的事件以 orderId 为 partition key，保证顺序消费。</p>
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private static final String ORDER_EVENTS_TOPIC = "order-events";
    private static final String GROUP_ID = "billing-service";

    private final BillingApplicationService billingApplicationService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(BillingApplicationService billingApplicationService,
                              ObjectMapper objectMapper) {
        this.billingApplicationService = billingApplicationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = ORDER_EVENTS_TOPIC, groupId = GROUP_ID)
    public void onOrderEvent(String message) {
        try {
            OrderApprovedEventDto event = objectMapper.readValue(message, OrderApprovedEventDto.class);

            // 仅处理审单通过事件（后续可扩展其他事件类型）
            if (!"OrderApproved".equals(event.eventType())) {
                log.debug("忽略非 OrderApproved 事件 eventType={}", event.eventType());
                return;
            }

            log.info("收到 OrderApproved 事件 orderId={}", event.orderId());

            List<BillReceiveCommand.BillLineItem> lines = event.items().stream()
                    .map(i -> new BillReceiveCommand.BillLineItem(
                            i.skuId(), i.skuName(), BigDecimal.valueOf(i.quantity()), i.unitPrice()))
                    .toList();

            billingApplicationService.generateBill(new BillReceiveCommand(
                    event.orderId(), event.customerId(), lines));

            log.info("账单生成完成 orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("处理 OrderApproved 事件失败 message={}", message, e);
            throw new RuntimeException("账单事件处理失败", e);
        }
    }
}
