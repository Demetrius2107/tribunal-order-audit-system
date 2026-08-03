package com.demetrius.tribunal.order.application.service;

import com.demetrius.tribunal.order.application.dto.OrderCreateCommand;
import com.demetrius.tribunal.order.application.dto.OrderPageResult;
import com.demetrius.tribunal.order.application.dto.OrderResult;
import com.demetrius.tribunal.order.client.CustomerFeignClient;
import com.demetrius.tribunal.order.client.InventoryFeignClient;
import com.demetrius.tribunal.order.client.InventoryItemResult;
import com.demetrius.tribunal.order.domain.event.OrderCreatedEvent;
import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import com.demetrius.tribunal.order.domain.repository.OrderPage;
import com.demetrius.tribunal.order.domain.repository.OrderRepository;
import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.infrastructure.idempotency.OrderIdempotencyGuard;
import feign.FeignException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单应用服务（用例编排层）。
 *
 * <p>参照通用做法。</p>
 *
 * <p>应用层的职责（DDD）：</p>
 * <ol>
 *   <li>接收接口层传来的命令（Command）</li>
 *   <li>编排领域对象：创建聚合 → 调领域服务校验 → 保存 → 发布事件</li>
 *   <li>管理事务边界（@Transactional）</li>
 *   <li>领域对象 ↔ 应用层 DTO 的转换</li>
 * </ol>
 * <p>应用层不包含业务规则——业务规则在 domain 层（Order 聚合内部）。</p>
 *
 * <p>TODO（学习任务）——参照通用做法</li>
 *   <li>② 订单编号生成策略：参照通用做法</li>
 *   <li>③ 幂等：同客户同参数短时间重复提交拦截（参照通用做法，里程碑 4 实现）</li>
 *   <li>④ 信用预占：下单即占用信用额度（参照通用做法</li>
 *   <li>⑤ 折扣池 / 促销计算：下单时重算金额（参照促销计算引擎，先做基础版）</li>
 * </ul>
 */
@Service
public class OrderApplicationService {

    private final OrderRepository orderRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final CustomerFeignClient customerFeignClient;

    private final InventoryFeignClient inventoryFeignClient;

    private final OrderIdempotencyGuard idempotencyGuard;

    public OrderApplicationService(OrderRepository orderRepository,
                                   ApplicationEventPublisher eventPublisher,
                                   CustomerFeignClient customerFeignClient,
                                   InventoryFeignClient inventoryFeignClient,
                                   OrderIdempotencyGuard idempotencyGuard) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.customerFeignClient = customerFeignClient;
        this.inventoryFeignClient = inventoryFeignClient;
        this.idempotencyGuard = idempotencyGuard;
    }

    /**
     * 创建订单（用例：下单）。
     *
     * <p>下单即占用客户信用额度（F-403/N-301），审单拒绝/订单取消时释放。</p>
     *
     * @param command 下单命令
     * @return 订单结果
     */
    @Transactional
    public OrderResult createOrder(OrderCreateCommand command) {
        // 前置校验：客户存在性 + SKU 存在性（下单前校验，避免占用信用/落库后才暴露问题）
        validateCustomerExists(command.customerId());
        validateSkusExist(command.skus());

        // 组装聚合
        String orderIdValue = generateOrderId();
        String orderNo = generateOrderNo();
        List<OrderSku> skus = command.skus().stream()
                .map(s -> new OrderSku(s.skuCode(), s.skuName(), s.quantity(), s.price()))
                .toList();

        // 幂等防重：同客户同明细 30 秒内重复提交直接拒绝（N-405，数据库唯一键兜底）
        idempotencyGuard.checkDuplicate(command.customerId(), skus);

        Order order = Order.create(new OrderId(orderIdValue), orderNo, command.customerId(), skus);

        // 下单即占用信用（信用是 customer 领域的动作，走 customer-service 接口）
        customerFeignClient.occupyCredit(order.getCustomerId(),
                new CustomerFeignClient.CreditOperationRequest(order.getPayableAmount()));

        // 保存聚合（事务内）
        orderRepository.save(order);

        // 发布领域事件（通知/审计解耦）
        eventPublisher.publishEvent(new OrderCreatedEvent(
                order.getId(), order.getOrderNo(), order.getCustomerId(), order.getCreateTime()));

        return OrderResult.from(order);
    }

    /**
     * 取消订单（用例：用户取消/超时关单）。
     *
     * <p>取消即释放信用预占（F-403），与下单占用的额度对称。</p>
     */
    @Transactional
    public OrderResult cancelOrder(String orderId) {
        Order order = orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new BizException("200002", "订单不存在: " + orderId));

        // 释放信用预占
        customerFeignClient.releaseCredit(order.getCustomerId(),
                new CustomerFeignClient.CreditOperationRequest(order.getPayableAmount()));

        order.cancel();
        orderRepository.save(order);
        return OrderResult.from(order);
    }

    /**
     * 查询订单。
     */
    @Transactional(readOnly = true)
    public OrderResult getOrder(String orderId) {
        Order order = orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new BizException("200002", "订单不存在: " + orderId));
        return OrderResult.from(order);
    }

    /**
     * 分页查询订单列表（可按客户/状态过滤）。
     */
    @Transactional(readOnly = true)
    public OrderPageResult listOrders(String customerId, String status, int pageNum, int pageSize) {
        OrderPage page = orderRepository.findPage(customerId, status, pageNum, pageSize);
        List<OrderResult> results = page.orders().stream()
                .map(OrderResult::from)
                .toList();
        return OrderPageResult.of(page.total(), page.pageNum(), page.pageSize(), results);
    }

    /**
     * 校验客户存在性（customer-service 查询信用，客户不存在时远程返回 400 → FeignException）。
     */
    private void validateCustomerExists(String customerId) {
        try {
            customerFeignClient.getCustomerCredit(customerId);
        } catch (FeignException e) {
            throw new BizException("200008", "客户不存在: " + customerId);
        }
    }

    /**
     * 校验 SKU 存在性（逐 SKU 调 inventory-service 查询物料主数据，F-101）。
     */
    private void validateSkusExist(List<OrderCreateCommand.SkuItem> skus) {
        for (OrderCreateCommand.SkuItem sku : skus) {
            ApiResponse<InventoryItemResult> resp = inventoryFeignClient.getBySkuCode(sku.skuCode());
            if (resp == null || !resp.isSuccess() || resp.getData() == null) {
                throw new BizException("200009", "SKU 不存在: " + sku.skuCode());
            }
        }
    }

    /**
     * TODO（学习任务）：生成订单 ID。
     * 可用 MyBatis-Plus 的 ASSIGN_ID 或自建 ID 生成器（雪花算法）。
     */
    private String generateOrderId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成订单编号：ORD + 时间戳（yyyyMMddHHmmss）+ 4 位随机数。
     *
     * <p>业务唯一性由数据库唯一约束（t_order.order_no）兜底（N-205 幂等第一道防线），
     * 高并发下若冲突可重试或改用分布式发号器（后续接入）。</p>
     */
    private String generateOrderNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "ORD" + ts + rand;
    }
}
