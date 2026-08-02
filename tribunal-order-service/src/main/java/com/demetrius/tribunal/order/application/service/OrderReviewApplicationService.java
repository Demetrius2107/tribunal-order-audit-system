package com.demetrius.tribunal.order.application.service;

import com.demetrius.tribunal.common.dto.CustomerCreditDto;
import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.order.application.dto.OrderReviewCommand;
import com.demetrius.tribunal.order.application.dto.OrderResult;
import com.demetrius.tribunal.order.client.CustomerFeignClient;
import com.demetrius.tribunal.order.domain.event.OrderStatusChangedEvent;
import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.repository.OrderRepository;
import com.demetrius.tribunal.order.domain.service.OrderReviewDomainService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审单应用服务。
 *
 * <p>对照旧项目：{@code SalesmanController.reviewOrder} / {@code OrderServiceImpl.orderReview}
 * （875 行审单逻辑）。</p>
 *
 * <p>微服务说明：信用校验通过 {@link CustomerFeignClient} 远程调用 customer-service
 * 获取信用 DTO，再交给领域服务校验——跨服务边界用 DTO，业务规则仍在 order 领域层。</p>
 *
 * <p>TODO（学习任务）——对照旧项目 {@code orderReview} 完整实现：</p>
 * <ul>
 *   <li>① 审单权限：AD 账号层级审批链（两级审批）——里程碑 3/5</li>
 *   <li>② 审单前重新计价：促销/折扣/押金在审单时重算（对照促销计算引擎）</li>
 *   <li>③ 审单通过后动作：通过 customer-service 接口正式扣减信用（下单是预占，审单是确定）、通知</li>
 *   <li>④ 审单拒绝：记录原因、释放信用预占、通知（对照 refuseToReason）</li>
 *   <li>⑤ 状态流水：每次迁移写 order_status_record（对照 saveOrderStatusProcessRecordDomain）</li>
 *   <li>⑥ Feign 失败处理：超时/熔断（骨架未引入，进阶项）</li>
 * </ul>
 */
@Service
public class OrderReviewApplicationService {

    private final OrderRepository orderRepository;

    private final CustomerFeignClient customerFeignClient;

    private final OrderReviewDomainService reviewDomainService;

    private final ApplicationEventPublisher eventPublisher;

    public OrderReviewApplicationService(OrderRepository orderRepository,
                                         CustomerFeignClient customerFeignClient,
                                         OrderReviewDomainService reviewDomainService,
                                         ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.customerFeignClient = customerFeignClient;
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
     * 审单通过：远程查信用 → 领域服务校验 → 状态迁移 → 保存 → 发布事件。
     */
    private void approve(Order order, String operator) {
        // 跨服务调用 customer-service 获取信用（TODO：失败重试/熔断）
        CustomerCreditDto credit = customerFeignClient.getCustomerCredit(order.getCustomerId());
        reviewDomainService.validateForReview(order, credit, operator);

        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                order.getId(), order.getOrderNo(), order.getStatus(), null, null);
        // 状态迁移（聚合内部校验，非法迁移抛异常）
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
