package com.demetrius.tribunal.financesettlement.infrastructure.listener;

import com.demetrius.tribunal.financesettlement.application.dto.OrderEventMessage;
import com.demetrius.tribunal.financesettlement.application.service.SettlementApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 订单事件消费者（PRD 4.1：Topic order-events，Consumer Group: finance-settlement）。
 *
 * <p>监听订单完成事件（OrderCompleted）触发结算单生成，正向流程起点（PRD 6.2）。</p>
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final SettlementApplicationService settlementApplicationService;

    public OrderEventConsumer(ObjectMapper objectMapper,
                              SettlementApplicationService settlementApplicationService) {
        this.objectMapper = objectMapper;
        this.settlementApplicationService = settlementApplicationService;
    }

    @KafkaListener(topics = "order-events", groupId = "finance-settlement")
    public void onOrderEvent(String message) {
        try {
            OrderEventMessage event = objectMapper.readValue(message, OrderEventMessage.class);
            if (!"OrderCompleted".equals(event.eventType())) {
                // 基建：仅处理订单完成事件，其余类型（Approved/Shipped/Cancelled）留待后续
                log.info("忽略非完成事件 eventType={} orderId={}", event.eventType(), event.orderId());
                return;
            }
            String settlementId = settlementApplicationService.createSettlement(
                    event.orderId(), event.userId(), event.merchantId(),
                    event.netAmount(), event.paymentMethod(), event.paymentCurrency());
            log.info("订单完成事件已生成结算单 settlementId={} orderId={}", settlementId, event.orderId());
        } catch (Exception e) {
            log.error("订单事件处理失败: {}", message, e);
        }
    }
}
