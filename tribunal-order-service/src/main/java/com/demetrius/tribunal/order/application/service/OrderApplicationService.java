package com.demetrius.tribunal.order.application.service;

import com.demetrius.tribunal.order.application.dto.OrderCreateCommand;
import com.demetrius.tribunal.order.application.dto.OrderResult;
import com.demetrius.tribunal.order.domain.event.OrderCreatedEvent;
import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import com.demetrius.tribunal.order.domain.repository.OrderRepository;
import com.demetrius.tribunal.common.exception.BizException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public OrderApplicationService(OrderRepository orderRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 创建订单（用例：下单）。
     *
     * @param command 下单命令
     * @return 订单结果
     */
    @Transactional
    public OrderResult createOrder(OrderCreateCommand command) {
        // TODO（学习任务）：校验客户/SKU 存在性、整托、渠道（行业通用做法 saveFinalOrder 前半段）

        // 组装聚合
        String orderIdValue = generateOrderId();
        String orderNo = generateOrderNo();
        List<OrderSku> skus = command.skus().stream()
                .map(s -> new OrderSku(s.skuCode(), s.skuName(), s.quantity(), s.price()))
                .toList();
        Order order = Order.create(new OrderId(orderIdValue), orderNo, command.customerId(), skus);

        // TODO（学习任务）：促销/折扣计算后，订单金额可能变化（先做基础版：金额=Σ明细）

        // 保存聚合（事务内）
        orderRepository.save(order);

        // 发布领域事件（通知/审计解耦，参照通用做法
        eventPublisher.publishEvent(new OrderCreatedEvent(
                order.getId(), order.getOrderNo(), order.getCustomerId(), order.getCreateTime()));

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
