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
 * <p>监听订单确认/完成事件（OrderApproved/OrderCompleted）触发结算单生成，正向流程起点（PRD 6.2）。
 * 重复收到同一订单事件时，由 createSettlement 按订单号幂等（FR-003）。</p>
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    /** 触发结算单生成的订单事件类型 */
    private static final String EVENT_ORDER_APPROVED = "OrderApproved";
    private static final String EVENT_ORDER_COMPLETED = "OrderCompleted";

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
            if (!EVENT_ORDER_APPROVED.equals(event.eventType())
                    && !EVENT_ORDER_COMPLETED.equals(event.eventType())) {
                // 仅处理确认/完成事件，其余类型（Shipped/Cancelled 等）不触发结算
                log.info("忽略非结算触发事件 eventType={} orderId={}", event.eventType(), event.orderId());
                return;
            }
            // 正向流程起点：生成结算单(PENDING) → 幂等扣款(CHARGED)（PRD 6.2）
            var view = settlementApplicationService.createSettlementAndCharge(
                    event.orderId(), event.userId(), event.merchantId(),
                    event.netAmount(), event.paymentMethod(), event.paymentCurrency());
            log.info("订单事件已生成结算单并扣款 settlementId={} orderId={} status={} eventType={}",
                    view.getSettlementId(), event.orderId(), view.getStatus(), event.eventType());
        } catch (Exception e) {
            log.error("订单事件处理失败: {}", message, e);
        }
    }
}
