package com.demetrius.tribunal.fulfillment.infrastructure.consumer;

import com.demetrius.tribunal.fulfillment.application.dto.FulfillmentReceiveCommand;
import com.demetrius.tribunal.fulfillment.application.service.FulfillmentApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单事件消费者（M3 异步化：消费 OrderApproved 事件创建履约单）。
 *
 * <p>消费组 fulfillment-service 独立于 billing-service / finance-settlement，各自消费全量消息。</p>
 * <p>同一 orderId 的事件以 orderId 为 partition key，保证顺序消费。</p>
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private static final String ORDER_EVENTS_TOPIC = "order-events";
    private static final String GROUP_ID = "fulfillment-service";

    private final FulfillmentApplicationService fulfillmentApplicationService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(FulfillmentApplicationService fulfillmentApplicationService,
                              ObjectMapper objectMapper) {
        this.fulfillmentApplicationService = fulfillmentApplicationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = ORDER_EVENTS_TOPIC, groupId = GROUP_ID)
    public void onOrderEvent(String message) {
        try {
            OrderApprovedEventDto event = objectMapper.readValue(message, OrderApprovedEventDto.class);

            if (!"OrderApproved".equals(event.eventType())) {
                log.debug("忽略非 OrderApproved 事件 eventType={}", event.eventType());
                return;
            }

            log.info("收到 OrderApproved 事件 orderId={}", event.orderId());

            List<FulfillmentReceiveCommand.FulfillmentLineItem> lines = event.items().stream()
                    .map(i -> new FulfillmentReceiveCommand.FulfillmentLineItem(
                            i.skuId(), i.skuName(), BigDecimal.valueOf(i.quantity()), i.unitPrice()))
                    .toList();

            fulfillmentApplicationService.create(new FulfillmentReceiveCommand(
                    event.orderId(), event.customerId(), lines));

            log.info("履约单创建完成 orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("处理 OrderApproved 事件失败 message={}", message, e);
            throw new RuntimeException("履约事件处理失败", e);
        }
    }
}
