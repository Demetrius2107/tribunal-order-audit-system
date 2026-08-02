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
import com.demetrius.tribunal.order.client.InventoryFeignClient;
import com.demetrius.tribunal.order.domain.event.OrderStatusChangedEvent;
import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import com.demetrius.tribunal.order.domain.repository.OrderRepository;
import com.demetrius.tribunal.order.domain.service.OrderReviewDomainService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    private final InventoryFeignClient inventoryFeignClient;

    private final BillingFeignClient billingFeignClient;

    private final OrderReviewDomainService reviewDomainService;

    private final ApplicationEventPublisher eventPublisher;

    public OrderReviewApplicationService(OrderRepository orderRepository,
                                         CustomerFeignClient customerFeignClient,
                                         InventoryFeignClient inventoryFeignClient,
                                         BillingFeignClient billingFeignClient,
                                         OrderReviewDomainService reviewDomainService,
                                         ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.customerFeignClient = customerFeignClient;
        this.inventoryFeignClient = inventoryFeignClient;
        this.billingFeignClient = billingFeignClient;
        this.reviewDomainService = reviewDomainService;
        this.eventPublisher = eventPublisher;
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
     * 审单通过：信用校验 → 预占库存 → 生成账单 → 状态迁移 → 保存 → 发布事件。
     *
     * <p>三合一编排链（对应需求 F-306/F-302/F-307）：</p>
     * <ol>
     *   <li>信用校验（customer-service）</li>
     *   <li>预占库存（inventory-service，逐 SKU 预占）</li>
     *   <li>生成账单（billing-service，转单）</li>
     *   <li>订单状态迁移：待确认 → 已确认</li>
     * </ol>
     */
    private void approve(Order order, String operator) {
        // ① 信用校验（跨服务，DTO 边界）
        CustomerCreditDto credit = customerFeignClient.getCustomerCredit(order.getCustomerId());
        reviewDomainService.validateForReview(order, credit, operator);

        // ② 预占库存（逐 SKU 调 inventory-service；TODO：失败处理/整体回滚）
        for (OrderSku sku : order.getSkus()) {
            inventoryFeignClient.reserve(sku.getSkuCode(), sku.getQuantity());
        }

        // ③ 生成账单（转单到 billing-service；TODO：失败处理）
        ApiResponse<BillTransferResult> billResponse = billingFeignClient.transfer(new BillTransferRequest(
                order.getOrderNo(),
                order.getCustomerId(),
                order.getSkus().stream()
                        .map(s -> new BillTransferRequest.BillTransferLine(
                                s.getSkuCode(), s.getSkuName(), s.getQuantity(), s.getPrice()))
                        .toList()));

        // ④ 状态迁移（聚合内部校验，非法迁移抛异常）
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                order.getId(), order.getOrderNo(), order.getStatus(), null, null);
        order.confirm();
        orderRepository.save(order);

        // 发布状态变更事件（通知/流水订阅者处理）
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                event.orderId(), event.orderNo(), event.from(), order.getStatus(), order.getUpdateTime()));
    }

    /**
     * 审单拒绝。
     */
    private void reject(Order order, String reason, String operator) {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                order.getId(), order.getOrderNo(), order.getStatus(), null, null);
        order.reject(reason);
        orderRepository.save(order);
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                event.orderId(), event.orderNo(), event.from(), order.getStatus(), order.getUpdateTime()));
    }
}
