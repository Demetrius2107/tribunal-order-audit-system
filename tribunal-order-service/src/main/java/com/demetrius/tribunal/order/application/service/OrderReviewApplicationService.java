package com.demetrius.tribunal.order.application.service;

import com.demetrius.tribunal.common.dto.CustomerCreditDto;
import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.application.dto.OrderReviewCommand;
import com.demetrius.tribunal.order.application.dto.OrderResult;
import com.demetrius.tribunal.order.client.CustomerFeignClient;
import com.demetrius.tribunal.order.client.InventoryFeignClient;
import com.demetrius.tribunal.order.client.InventoryItemResult;
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
import com.demetrius.tribunal.order.domain.service.PromotionCalculator;
import com.demetrius.tribunal.order.infrastructure.event.OrderEventPublisher;
import com.demetrius.tribunal.order.application.dto.OrderEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 审单应用服务（M3 异步化）。
 *
 * <p>审单通过编排：① 取价（marketing，同步）→ ② 信用校验（customer，同步）→
 * ③ 预占库存（inventory，同步）→ ④ 信用占用（customer，同步）→
 * ⑤ 发通知（notification，同步，非关键）→ ⑥ 订单确认 + outbox 事件发布 →
 * ⑦ billing/fulfillment/finance-settlement 异步消费 order-events。</p>
 *
 * <p>同步 3 子域（customer/marketing/inventory）保证审单时数据一致性；
 * billing/fulfillment 通过 outbox → Kafka 异步驱动，审单接口不再同步等待。</p>
 */
@Service
public class OrderReviewApplicationService {

    private static final Logger log = LoggerFactory.getLogger(OrderReviewApplicationService.class);

    private final OrderRepository orderRepository;

    private final CustomerFeignClient customerFeignClient;

    private final MarketingFeignClient marketingFeignClient;

    private final InventoryFeignClient inventoryFeignClient;

    private final NotificationFeignClient notificationFeignClient;

    private final OrderReviewDomainService reviewDomainService;

    private final OrderAmountCalculator amountCalculator;

    private final PromotionCalculator promotionCalculator;

    private final ApplicationEventPublisher eventPublisher;

    private final OrderEventPublisher orderEventPublisher;

    public OrderReviewApplicationService(OrderRepository orderRepository,
                                         CustomerFeignClient customerFeignClient,
                                         MarketingFeignClient marketingFeignClient,
                                         InventoryFeignClient inventoryFeignClient,
                                         NotificationFeignClient notificationFeignClient,
                                         OrderReviewDomainService reviewDomainService,
                                         OrderAmountCalculator amountCalculator,
                                         PromotionCalculator promotionCalculator,
                                         ApplicationEventPublisher eventPublisher,
                                         OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.customerFeignClient = customerFeignClient;
        this.marketingFeignClient = marketingFeignClient;
        this.inventoryFeignClient = inventoryFeignClient;
        this.notificationFeignClient = notificationFeignClient;
        this.reviewDomainService = reviewDomainService;
        this.amountCalculator = amountCalculator;
        this.promotionCalculator = promotionCalculator;
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

        // ①' 促销折扣计算（F-202）：命中客户/客户组型促销的 SKU 按行算折扣
        // 基建说明：促销规则来源为 marketing-service（后续提供促销接口），当前骨架由应用层传入空规则
        promotionCalculator.applyPromotions(order, List.of(), order.getCustomerId(), null);

        // ② 信用校验（基于重算后的应付金额，跨服务 DTO 边界）
        CustomerCreditDto credit = customerFeignClient.getCustomerCredit(order.getCustomerId());
        reviewDomainService.validateForReview(order, credit, operator);

        // ③~④ 同步编排：预占库存 → 信用占用（远程副作用须补偿）
        // M3 异步化：账单生成/履约创建改为 outbox → Kafka 异步事件，不再同步等待
        List<String> reservedSkus = new ArrayList<>();
        boolean creditOccupied = false;
        try {
            for (OrderSku sku : order.getSkus()) {
                ApiResponse<InventoryItemResult> resp =
                        inventoryFeignClient.reserve(sku.getSkuCode(), sku.getQuantity());
                checkFeignSuccess(resp, "库存预占失败: " + sku.getSkuCode());
                reservedSkus.add(sku.getSkuCode());
            }

            // ③' 信用占用（F-403/N-301）：冻结应付金额等额的信用额度
            ApiResponse<CustomerCreditDto> creditResp = customerFeignClient.occupyCredit(
                    order.getCustomerId(),
                    new CustomerFeignClient.CreditOperationRequest(order.getPayableAmount()));
            checkFeignSuccess(creditResp, "信用占用失败");
            creditOccupied = true;
        } catch (BizException e) {
            // 补偿：回滚已预占的库存 + 已占用的信用
            compensateReserved(reservedSkus, order);
            if (creditOccupied) {
                compensateCreditOccupy(order);
            }
            throw e;
        } catch (Exception e) {
            compensateReserved(reservedSkus, order);
            if (creditOccupied) {
                compensateCreditOccupy(order);
            }
            throw new BizException("200006", "审单跨服务编排失败: " + e.getMessage());
        }

        // ⑤ 发送通知（非关键路径，失败不阻断审单、不触发补偿）
        try {
            ApiResponse<Void> notificationResponse =
                    notificationFeignClient.send(new NotificationFeignClient.NotificationSendRequest(
                            "SITE_MESSAGE", order.getCustomerId(), "订单已确认",
                            "您的订单 " + order.getOrderNo() + " 已通过审单"));
            checkFeignSuccess(notificationResponse, "通知发送失败");
        } catch (Exception e) {
            log.warn("通知发送失败，审单流程继续 orderId={}", order.getOrderNo(), e);
        }

        // ⑥ 状态迁移（聚合内部校验，非法迁移抛异常）
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                order.getId(), order.getOrderNo(), order.getStatus(), null, operator, null);
        order.confirm();
        orderRepository.save(order);

        // 发布状态变更事件（通知/流水订阅者处理）
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                event.orderId(), event.orderNo(), event.from(), order.getStatus(),
                event.operator(), order.getUpdateTime()));

        // ⑦ 发布订单确认事件到 outbox（relay 投递 Kafka，下游 billing/fulfillment/finance-settlement
        //   异步消费，M3 异步化：审单接口不再同步等待 billing/fulfillment）
        orderEventPublisher.publishOrderEvent(buildApprovedEvent(order));
    }

    /**
     * 校验 Feign 统一响应：success 为 false 时抛业务异常（避免"响应失败还继续编排"）。
     */
    private <T> void checkFeignSuccess(ApiResponse<T> resp, String failMessage) {
        if (resp == null || !resp.isSuccess()) {
            String detail = resp == null ? "响应为空" : resp.getMessage();
            throw new BizException("200006", failMessage + "：" + detail);
        }
    }

    /**
     * 补偿：释放本次已预占的库存（远程副作用回滚，逐 SKU release）。
     */
    private void compensateReserved(List<String> reservedSkus, Order order) {
        for (OrderSku sku : order.getSkus()) {
            if (reservedSkus.contains(sku.getSkuCode())) {
                try {
                    inventoryFeignClient.release(sku.getSkuCode(), sku.getQuantity());
                } catch (Exception ex) {
                    // 补偿失败记录日志，交由对账任务兜底（F-504 库存变动流水/对账）
                    log.error("审单失败后库存补偿失败 skuCode={}", sku.getSkuCode(), ex);
                }
            }
        }
    }

    /**
     * 补偿：释放已占用的信用（F-403，与 approve 中的 occupyCredit 对称）。
     * <p>当跨服务编排过程中信用占用成功后后续步骤失败时调用，避免信用悬空。</p>
     */
    private void compensateCreditOccupy(Order order) {
        try {
            customerFeignClient.releaseCredit(order.getCustomerId(),
                    new CustomerFeignClient.CreditOperationRequest(order.getPayableAmount()));
        } catch (Exception ex) {
            log.error("审单失败后信用补偿失败 customerId={}, amount={}",
                    order.getCustomerId(), order.getPayableAmount(), ex);
        }
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
        // 释放信用预占（与下单占用对称，失败不阻断拒绝流程，对账任务兜底）
        try {
            customerFeignClient.releaseCredit(order.getCustomerId(),
                    new CustomerFeignClient.CreditOperationRequest(order.getPayableAmount()));
        } catch (Exception ex) {
            log.error("审单拒绝时信用释放失败 customerId={}, amount={}",
                    order.getCustomerId(), order.getPayableAmount(), ex);
        }

        // 释放库存预占（逐 SKU，失败不阻断，对账任务兜底）
        for (OrderSku sku : order.getSkus()) {
            try {
                inventoryFeignClient.release(sku.getSkuCode(), sku.getQuantity());
            } catch (Exception ex) {
                log.error("审单拒绝时库存释放失败 skuCode={}", sku.getSkuCode(), ex);
            }
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
