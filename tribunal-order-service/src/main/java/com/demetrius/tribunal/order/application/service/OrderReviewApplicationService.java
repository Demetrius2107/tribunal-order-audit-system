package com.demetrius.tribunal.order.application.service;

import com.demetrius.tribunal.common.dto.CustomerCreditDto;
import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.application.dto.OrderReviewCommand;
import com.demetrius.tribunal.order.application.dto.OrderResult;
import com.demetrius.tribunal.order.client.BillTransferRequest;
import com.demetrius.tribunal.order.client.BillTransferResult;
import com.demetrius.tribunal.order.client.BillingFeignClient;
import com.demetrius.tribunal.order.client.CustomerFeignClient;
import com.demetrius.tribunal.order.client.FulfillmentFeignClient;
import com.demetrius.tribunal.order.client.InventoryFeignClient;
import com.demetrius.tribunal.order.client.MarketingFeignClient;
import com.demetrius.tribunal.order.client.NotificationFeignClient;
import com.demetrius.tribunal.order.client.PriceQuoteResult;
import com.demetrius.tribunal.order.domain.event.OrderStatusChangedEvent;
import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import com.demetrius.tribunal.order.domain.repository.OrderRepository;
import com.demetrius.tribunal.order.domain.service.OrderAmountCalculator;
import com.demetrius.tribunal.order.domain.service.OrderReviewDomainService;
import com.demetrius.tribunal.order.infrastructure.event.OrderEventPublisher;
import com.demetrius.tribunal.order.application.dto.OrderEventMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 审单应用服务。
 *
 * <p>三合一编排：审单通过 → 预占库存（inventory-service）→ 生成账单（billing-service）→
 * 账单结算回传（billing → order，状态机驱动）。</p>
 *
 * <p>微服务说明：跨服务调用均走 Feign（信用/库存/账单），跨服务边界用 DTO，
 * 业务规则仍在 order 领域层。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>审单前重新计价：促销/折扣/押金在审单时重算</li>
 *   <li>审单通过后：通过 customer-service 接口正式扣减信用（下单是预占，审单是确定）</li>
 *   <li>审单拒绝：记录原因、释放信用预占与库存预占</li>
 *   <li>状态流水：每次迁移写 order_status_record</li>
 *   <li>Feign 失败处理：超时/熔断（骨架未引入，进阶项）</li>
 * </ul>
 */
@Service
public class OrderReviewApplicationService {

    private final OrderRepository orderRepository;

    private final CustomerFeignClient customerFeignClient;

    private final MarketingFeignClient marketingFeignClient;

    private final InventoryFeignClient inventoryFeignClient;

    private final BillingFeignClient billingFeignClient;

    private final FulfillmentFeignClient fulfillmentFeignClient;

    private final NotificationFeignClient notificationFeignClient;

    private final OrderReviewDomainService reviewDomainService;

    private final OrderAmountCalculator amountCalculator;

    private final ApplicationEventPublisher eventPublisher;

    private final OrderEventPublisher orderEventPublisher;

    public OrderReviewApplicationService(OrderRepository orderRepository,
                                         CustomerFeignClient customerFeignClient,
                                         MarketingFeignClient marketingFeignClient,
                                         InventoryFeignClient inventoryFeignClient,
                                         BillingFeignClient billingFeignClient,
                                         FulfillmentFeignClient fulfillmentFeignClient,
                                         NotificationFeignClient notificationFeignClient,
                                         OrderReviewDomainService reviewDomainService,
                                         OrderAmountCalculator amountCalculator,
                                         ApplicationEventPublisher eventPublisher,
                                         OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.customerFeignClient = customerFeignClient;
        this.marketingFeignClient = marketingFeignClient;
        this.inventoryFeignClient = inventoryFeignClient;
        this.billingFeignClient = billingFeignClient;
        this.fulfillmentFeignClient = fulfillmentFeignClient;
        this.notificationFeignClient = notificationFeignClient;
        this.reviewDomainService = reviewDomainService;
        this.amountCalculator = amountCalculator;
        this.eventPublisher = eventPublisher;
        this.orderEventPublisher = orderEventPublisher;
    }

    /**
     * 审单用例（通过 / 拒绝）。
     *
     * @param command 审单命令
     * @return 审单后的订单结果
     */
    @Transactional
    public OrderResult review(OrderReviewCommand command) {
        Order order = orderRepository.findById(new OrderId(command.orderId()))
                .orElseThrow(() -> new BizException("200002", "订单不存在: " + command.orderId()));

        if (command.approved()) {
            approve(order, command.operator());
        } else {
            reject(order, command.reason(), command.operator());
        }

        return OrderResult.from(order);
    }

    /**
     * 审单通过：信用校验 → 取价 → 预占库存 → 生成账单 → 创建履约 → 发送通知 → 状态迁移。
     *
     * <p>五合一编排链（对应需求 F-306/F-302/F-307/F-503/F-601）：</p>
     * <ol>
     *   <li>信用校验（customer-service）</li>
     *   <li>取价校验（marketing-service，上游"金额"数据源）</li>
     *   <li>预占库存（inventory-service，逐 SKU 预占）</li>
     *   <li>生成账单（billing-service，转单）</li>
     *   <li>创建履约（fulfillment-service，发货/签收/发送工厂）</li>
     *   <li>发送通知（notification-service，站内信/邮件）</li>
     *   <li>订单状态迁移：待确认 → 已确认</li>
     * </ol>
     */
    private void approve(Order order, String operator) {
        // ① 审单前重新计价：以 marketing-service 取价为准覆盖明细价格（F-306，防前端传价失真）
        Map<String, BigDecimal> priceBySku = new HashMap<>();
        for (OrderSku sku : order.getSkus()) {
            PriceQuoteResult quote = marketingFeignClient.quotePrice(
                    sku.getSkuCode(), order.getCustomerId(), null, null).getData();
            priceBySku.put(sku.getSkuCode(), quote.price());
        }
        amountCalculator.reprice(order, priceBySku);

        // ② 信用校验（基于重算后的应付金额，跨服务 DTO 边界）
        CustomerCreditDto credit = customerFeignClient.getCustomerCredit(order.getCustomerId());
        reviewDomainService.validateForReview(order, credit, operator);

        // ③ 预占库存（逐 SKU 调 inventory-service；TODO：失败处理/整体回滚）
        for (OrderSku sku : order.getSkus()) {
            inventoryFeignClient.reserve(sku.getSkuCode(), sku.getQuantity());
        }

        // ④ 生成账单（转单到 billing-service；TODO：失败处理）
        ApiResponse<BillTransferResult> billResponse = billingFeignClient.transfer(new BillTransferRequest(
                order.getOrderNo(),
                order.getCustomerId(),
                order.getSkus().stream()
                        .map(s -> new BillTransferRequest.BillTransferLine(
                                s.getSkuCode(), s.getSkuName(), s.getQuantity(), s.getPrice()))
                        .toList()));

        // ⑤ 创建履约（fulfillment-service；TODO：失败处理）
        fulfillmentFeignClient.create(new FulfillmentFeignClient.FulfillmentCreateRequest(
                order.getOrderNo(),
                order.getCustomerId(),
                order.getSkus().stream()
                        .map(s -> new FulfillmentFeignClient.FulfillmentCreateRequest.FulfillmentLineItem(
                                s.getSkuCode(), s.getSkuName(), s.getQuantity(), s.getPrice()))
                        .toList()));

        // ⑥ 发送通知（notification-service；TODO：通知模板/接收人规则）
        notificationFeignClient.send(new NotificationFeignClient.NotificationSendRequest(
                "SITE_MESSAGE", order.getCustomerId(), "订单已确认",
                "您的订单 " + order.getOrderNo() + " 已通过审单"));

        // ⑦ 状态迁移（聚合内部校验，非法迁移抛异常）
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                order.getId(), order.getOrderNo(), order.getStatus(), null, operator, null);
        order.confirm();
        orderRepository.save(order);

        // 发布状态变更事件（通知/流水订阅者处理）
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                event.orderId(), event.orderNo(), event.from(), order.getStatus(),
                event.operator(), order.getUpdateTime()));

        // ⑧ 发布订单确认事件到 Kafka（下游金融结算系统订阅生成结算单，对应 PRD 4.1）
        orderEventPublisher.publishOrderEvent(buildApprovedEvent(order));
    }

    /**
     * 构建订单确认事件（OrderApproved），对接下游金融结算系统的 order-events 主题。
     */
    private OrderEventMessage buildApprovedEvent(Order order) {
        List<OrderEventMessage.Item> items = order.getSkus().stream()
                .map(s -> new OrderEventMessage.Item(
                        s.getSkuCode(), s.getSkuName(), s.getQuantity().intValue(), s.getPrice(), null, null))
                .toList();
        return new OrderEventMessage(
                UUID.randomUUID().toString().replace("-", ""),
                "OrderApproved",
                order.getOrderNo(),
                order.getCustomerId(),
                null,
                items,
                BigDecimal.ZERO,
                order.getDiscountAmount(),
                null,
                "CNY",
                order.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")),
                null,
                null);
    }

    /**
     * 审单拒绝：释放信用预占 + 释放库存预占 → 拒绝订单 → 发布状态变更事件。
     *
     * <p>审单拒绝必须回滚下单时占用的信用额度（customer-service）与已预占的库存
     * （与 approve 的预占对称，避免信用/库存悬空，F-403/F-503）。</p>
     */
    private void reject(Order order, String reason, String operator) {
        // 释放信用预占（与下单占用对称）
        customerFeignClient.releaseCredit(order.getCustomerId(),
                new CustomerFeignClient.CreditOperationRequest(order.getPayableAmount()));

        // 释放库存预占（逐 SKU；TODO：失败处理/补偿）
        for (OrderSku sku : order.getSkus()) {
            inventoryFeignClient.release(sku.getSkuCode(), sku.getQuantity());
        }

        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                order.getId(), order.getOrderNo(), order.getStatus(), null, operator, null);
        order.reject(reason);
        orderRepository.save(order);
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                event.orderId(), event.orderNo(), event.from(), order.getStatus(),
                event.operator(), order.getUpdateTime()));
    }
}
