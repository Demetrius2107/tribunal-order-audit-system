package com.demetrius.tribunal.order.application.service;

import com.demetrius.tribunal.common.dto.CustomerCreditDto;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.application.dto.OrderReviewCommand;
import com.demetrius.tribunal.order.client.BillingFeignClient;
import com.demetrius.tribunal.order.client.CustomerFeignClient;
import com.demetrius.tribunal.order.client.FulfillmentFeignClient;
import com.demetrius.tribunal.order.client.InventoryFeignClient;
import com.demetrius.tribunal.order.client.MarketingFeignClient;
import com.demetrius.tribunal.order.client.NotificationFeignClient;
import com.demetrius.tribunal.order.client.PriceQuoteResult;
import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import com.demetrius.tribunal.order.domain.model.OrderStatus;
import com.demetrius.tribunal.order.domain.repository.OrderRepository;
import com.demetrius.tribunal.order.domain.service.OrderAmountCalculator;
import com.demetrius.tribunal.order.domain.service.OrderReviewDomainService;
import com.demetrius.tribunal.order.infrastructure.event.OrderEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审单应用服务单元测试（重点：审单拒绝的信用/库存补偿，F-403/F-503）。
 */
@ExtendWith(MockitoExtension.class)
class OrderReviewApplicationServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerFeignClient customerFeignClient;
    @Mock
    private MarketingFeignClient marketingFeignClient;
    @Mock
    private InventoryFeignClient inventoryFeignClient;
    @Mock
    private BillingFeignClient billingFeignClient;
    @Mock
    private FulfillmentFeignClient fulfillmentFeignClient;
    @Mock
    private NotificationFeignClient notificationFeignClient;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private OrderEventPublisher orderEventPublisher;

    private OrderReviewApplicationService service;
    private final OrderAmountCalculator amountCalculator = new OrderAmountCalculator();
    private final OrderReviewDomainService reviewDomainService = new OrderReviewDomainService();

    @BeforeEach
    void setUp() {
        service = new OrderReviewApplicationService(
                orderRepository, customerFeignClient, marketingFeignClient,
                inventoryFeignClient, billingFeignClient, fulfillmentFeignClient,
                notificationFeignClient, reviewDomainService, amountCalculator,
                eventPublisher, orderEventPublisher);
    }

    private Order pendingOrder() {
        return Order.create(
                new OrderId("ord-001"),
                "ORD1001",
                "cust-001",
                List.of(new OrderSku("SKU001", "商品A", BigDecimal.TEN, BigDecimal.valueOf(50))));
    }

    @Test
    @DisplayName("审单拒绝：释放信用预占 + 释放库存预占，订单进入已拒绝")
    void shouldReleaseCreditAndInventoryOnReject() {
        Order order = pendingOrder();
        when(orderRepository.findById(new OrderId("ord-001"))).thenReturn(Optional.of(order));

        service.review(new OrderReviewCommand("ord-001", false, "信用不足", "ops-01"));

        // 信用释放（金额 = 应付金额）
        verify(customerFeignClient).releaseCredit(any(), any());
        // 库存释放（逐 SKU）
        verify(inventoryFeignClient).release("SKU001", BigDecimal.TEN);
        // 不触发库存预占/账单/履约/通知
        verify(inventoryFeignClient, never()).reserve(any(), any());
        verify(billingFeignClient, never()).transfer(any());
        assertEquals(OrderStatus.REJECTED, order.getStatus());
        assertEquals("信用不足", order.getRejectReason());
    }

    @Test
    @DisplayName("审单拒绝：订单不存在抛业务异常")
    void shouldThrowWhenOrderNotFound() {
        when(orderRepository.findById(new OrderId("ord-999"))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.review(
                new OrderReviewCommand("ord-999", false, "原因", "ops-01")));
        verify(customerFeignClient, never()).releaseCredit(any(), any());
    }

    @Test
    @DisplayName("审单拒绝：释放信用后状态保存")
    void shouldSaveAfterReject() {
        Order order = pendingOrder();
        when(orderRepository.findById(new OrderId("ord-001"))).thenReturn(Optional.of(order));

        service.review(new OrderReviewCommand("ord-001", false, "客户取消", "ops-01"));

        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("审单通过：信用校验通过后逐 SKU 预占库存")
    void shouldReserveInventoryOnApprove() {
        Order order = pendingOrder();
        when(orderRepository.findById(new OrderId("ord-001"))).thenReturn(Optional.of(order));
        when(customerFeignClient.getCustomerCredit("cust-001"))
                .thenReturn(new CustomerCreditDto("cust-001", "C001",
                        BigDecimal.valueOf(10000), BigDecimal.ZERO));
        when(marketingFeignClient.quotePrice(any(), any(), any(), any()))
                .thenReturn(ApiResponse.ok(new PriceQuoteResult("SKU001", BigDecimal.valueOf(50), "CNY")));
        when(inventoryFeignClient.reserve("SKU001", BigDecimal.TEN))
                .thenReturn(ApiResponse.ok(null));
        when(billingFeignClient.transfer(any()))
                .thenReturn(ApiResponse.ok(null));
        when(fulfillmentFeignClient.create(any()))
                .thenReturn(ApiResponse.ok(null));
        when(notificationFeignClient.send(any()))
                .thenReturn(ApiResponse.ok(null));

        service.review(new OrderReviewCommand("ord-001", true, null, "ops-01"));

        verify(inventoryFeignClient).reserve("SKU001", BigDecimal.TEN);
        verify(billingFeignClient).transfer(any());
        verify(fulfillmentFeignClient).create(any());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }
}
