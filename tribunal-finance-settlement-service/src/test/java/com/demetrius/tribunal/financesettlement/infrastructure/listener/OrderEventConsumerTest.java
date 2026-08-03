package com.demetrius.tribunal.financesettlement.infrastructure.listener;

import com.demetrius.tribunal.financesettlement.application.dto.OrderEventMessage;
import com.demetrius.tribunal.financesettlement.application.service.SettlementApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 订单事件消费者单元测试（OrderApproved → 结算单生成链路，PRD 4.1）。
 */
@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private SettlementApplicationService settlementApplicationService;

    private OrderEventConsumer consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        consumer = new OrderEventConsumer(objectMapper, settlementApplicationService);
    }

    private String eventJson(String eventType) {
        OrderEventMessage event = new OrderEventMessage(
                "evt-001",
                eventType,
                "ORD20260803001",
                "USR_789",
                "MCH_001",
                List.of(new OrderEventMessage.Item(
                        "SKU_001", "iPhone 16 Pro", 1, new BigDecimal("899900"), "ELECTRONICS", "WH_BJ_01")),
                new BigDecimal("1500"),
                new BigDecimal("50000"),
                "WECHAT_PAY",
                "CNY",
                "2026-08-03T10:00:00",
                null,
                null);
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("OrderApproved 事件触发结算单生成")
    void shouldCreateSettlementOnApproved() {
        consumer.onOrderEvent(eventJson("OrderApproved"));

        verify(settlementApplicationService).createSettlement(
                eq("ORD20260803001"), eq("USR_789"), eq("MCH_001"),
                any(BigDecimal.class), eq("WECHAT_PAY"), eq("CNY"));
    }

    @Test
    @DisplayName("OrderCompleted 事件也触发结算单生成（兼容旧事件）")
    void shouldCreateSettlementOnCompleted() {
        consumer.onOrderEvent(eventJson("OrderCompleted"));

        verify(settlementApplicationService).createSettlement(
                eq("ORD20260803001"), eq("USR_789"), eq("MCH_001"),
                any(BigDecimal.class), eq("WECHAT_PAY"), eq("CNY"));
    }

    @Test
    @DisplayName("非结算触发事件（OrderShipped）不生成结算单")
    void shouldIgnoreNonTriggerEvent() {
        consumer.onOrderEvent(eventJson("OrderShipped"));

        verify(settlementApplicationService, never()).createSettlement(anyString(), anyString(), anyString(),
                any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    @DisplayName("netAmount = 商品金额 - 优惠 + 运费（899900 - 50000 + 1500 = 851400）")
    void shouldPassCalculatedNetAmount() {
        consumer.onOrderEvent(eventJson("OrderApproved"));

        verify(settlementApplicationService).createSettlement(
                anyString(), anyString(), anyString(),
                eq(new BigDecimal("851400")), anyString(), anyString());
    }

    @Test
    @DisplayName("异常消息不抛出（记日志兜底），避免消费者中断")
    void shouldSwallowParseError() {
        consumer.onOrderEvent("{invalid json");
        verify(settlementApplicationService, never()).createSettlement(anyString(), anyString(), anyString(),
                any(BigDecimal.class), anyString(), anyString());
    }
}
