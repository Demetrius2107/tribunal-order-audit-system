package com.demetrius.tribunal.order.application.service;

import com.demetrius.tribunal.order.application.dto.OrderCreateCommand;
import com.demetrius.tribunal.order.application.dto.OrderPageResult;
import com.demetrius.tribunal.order.application.dto.OrderResult;
import com.demetrius.tribunal.order.client.CustomerFeignClient;
import com.demetrius.tribunal.order.client.InventoryFeignClient;
import com.demetrius.tribunal.order.client.InventoryItemResult;
import com.demetrius.tribunal.order.client.MarketingFeignClient;
import com.demetrius.tribunal.order.client.PromotionCalculateRequest;
import com.demetrius.tribunal.order.client.PromotionCalculateResponse;
import com.demetrius.tribunal.order.domain.event.OrderCreatedEvent;
import com.demetrius.tribunal.order.domain.event.OrderStatusChangedEvent;
import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import com.demetrius.tribunal.order.domain.model.OrderStatus;
import com.demetrius.tribunal.order.domain.model.PreOrderActivity;
import com.demetrius.tribunal.order.domain.model.PreOrderRecord;
import com.demetrius.tribunal.order.domain.model.ReturnablePackaging;
import com.demetrius.tribunal.order.domain.repository.OrderPage;
import com.demetrius.tribunal.order.domain.repository.OrderRepository;
import com.demetrius.tribunal.order.domain.repository.PreOrderActivityRepository;
import com.demetrius.tribunal.order.domain.repository.PreOrderRecordRepository;
import com.demetrius.tribunal.order.domain.service.DepositCalculator;
import com.demetrius.tribunal.order.domain.service.DiscountCapPolicy;
import com.demetrius.tribunal.order.domain.service.OrderReviewDomainService;
import com.demetrius.tribunal.order.domain.service.ShippingFeeCalculator;
import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.common.dto.TimeoutCloseResult;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.infrastructure.idempotency.OrderIdempotencyGuard;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);

    private final OrderRepository orderRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final CustomerFeignClient customerFeignClient;

    private final InventoryFeignClient inventoryFeignClient;

    private final MarketingFeignClient marketingFeignClient;

    private final OrderReviewDomainService reviewDomainService;

    private final DepositCalculator depositCalculator;

    private final OrderIdempotencyGuard idempotencyGuard;

    private final ShippingFeeCalculator shippingFeeCalculator;

    private final DiscountCapPolicy discountCapPolicy;

    private final PreOrderActivityRepository preOrderActivityRepository;

    private final PreOrderRecordRepository preOrderRecordRepository;

    public OrderApplicationService(OrderRepository orderRepository,
                                   ApplicationEventPublisher eventPublisher,
                                   CustomerFeignClient customerFeignClient,
                                   InventoryFeignClient inventoryFeignClient,
                                   MarketingFeignClient marketingFeignClient,
                                   OrderReviewDomainService reviewDomainService,
                                   DepositCalculator depositCalculator,
                                   OrderIdempotencyGuard idempotencyGuard,
                                   ShippingFeeCalculator shippingFeeCalculator,
                                   DiscountCapPolicy discountCapPolicy,
                                   PreOrderActivityRepository preOrderActivityRepository,
                                   PreOrderRecordRepository preOrderRecordRepository) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.customerFeignClient = customerFeignClient;
        this.inventoryFeignClient = inventoryFeignClient;
        this.marketingFeignClient = marketingFeignClient;
        this.reviewDomainService = reviewDomainService;
        this.depositCalculator = depositCalculator;
        this.idempotencyGuard = idempotencyGuard;
        this.shippingFeeCalculator = shippingFeeCalculator;
        this.discountCapPolicy = discountCapPolicy;
        this.preOrderActivityRepository = preOrderActivityRepository;
        this.preOrderRecordRepository = preOrderRecordRepository;
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

        Order order = Order.create(new OrderId(orderIdValue), orderNo, command.customerId(),
                command.orderType(), command.carPooling(), skus,
                command.returnablePackagings() == null ? List.of() : command.returnablePackagings().stream()
                        .map(r -> new ReturnablePackaging(
                                r.packagingType(), r.packagingName(), r.quantity(), r.unitDeposit()))
                        .toList());

        // F-312 预购：走独立计价口径（预购专享折扣率 + 保证金模式），预购单跳过普通营销促销
        if (order.isPreOrder()) {
            applyPreOrderPricing(order, command);
        }

        // 折扣池抵扣（F-204：用折扣池余额冲抵应付金额，业务文档三节）
        if (command.discountPoolDeduction() != null
                && command.discountPoolDeduction().compareTo(BigDecimal.ZERO) > 0) {
            order.applyDiscountPoolDeduction(command.discountPoolDeduction());
        }
        // 运费（F-103：按送货地址/SKU 计算，参与金额汇总；未指定时按规则自动计算）
        BigDecimal shippingFee = command.shippingFee();
        if (shippingFee == null || shippingFee.compareTo(BigDecimal.ZERO) <= 0) {
            int itemCount = order.getSkus().stream()
                    .mapToInt(s -> s.getQuantity().intValue())
                    .sum();
            shippingFee = shippingFeeCalculator.calculate(itemCount, order.getTotalAmount());
        }
        order.applyShippingFee(shippingFee);
        // 促销折扣 + 押金（F-202/F-205：调用 marketing-service 引擎计算，远程不可用时降级到本地押金）
        applyPromotionAndDeposit(order, command);

        // 整托校验：SKU 数量必须是整托规格的倍数（业务文档五节 F-302，规格来自 SKU 主数据）
        reviewDomainService.validateWholePallet(order, command.palletSpecs());

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
     * F-312 预购计价：独立计价口径（预购专享折扣率 + 保证金模式）。
     *
     * <p>逐 SKU 校验活动可参与、按预购价重算单价、重算金额，
     * 计算保证金/补缴并写预购占用记录（t_pre_order_record）。</p>
     */
    private void applyPreOrderPricing(Order order, OrderCreateCommand command) {
        if (command.preOrderActivityNo() == null || command.preOrderActivityNo().isBlank()) {
            throw new BizException("200013", "预购订单必须指定预购活动编号");
        }
        PreOrderActivity activity = preOrderActivityRepository.findByActivityNo(command.preOrderActivityNo())
                .orElseThrow(() -> new BizException("200014", "预购活动不存在: " + command.preOrderActivityNo()));
        LocalDateTime now = LocalDateTime.now();
        for (OrderSku sku : order.getSkus()) {
            activity.validateParticipate(sku.getSkuCode(), now);
            sku.reprice(activity.preOrderPrice(sku.getPrice()));
        }
        order.recalculateAmounts();
        BigDecimal total = order.getTotalAmount();
        BigDecimal deposit = activity.depositAmount(total);
        BigDecimal supplement = activity.supplementAmount(total);
        preOrderRecordRepository.save(new PreOrderRecord(
                java.util.UUID.randomUUID().toString().replace("-", ""),
                command.preOrderActivityNo(),
                order.getOrderNo(),
                total, deposit, supplement, now));
        log.info("预购下单: orderNo={} activityNo={} deposit={} supplement={}",
                order.getOrderNo(), command.preOrderActivityNo(), deposit, supplement);
    }

    /**
     * 促销折扣 + 押金计算（F-202 + F-205）。
     *
     * <p>调用 marketing-service 一次返回促销折扣、赠品、押金及分摊明细。
     * 远程不可用时降级到本地 DepositCalculator（保证下单主流程不被外部依赖阻断）。</p>
     */
    private void applyPromotionAndDeposit(Order order, OrderCreateCommand command) {
        try {
            List<PromotionCalculateRequest.SkuItemDto> skuItems = order.getSkus().stream()
                    .map(s -> new PromotionCalculateRequest.SkuItemDto(
                            s.getSkuCode(), s.getSkuName(), s.getQuantity(), s.getPrice()))
                    .toList();
            List<String> skuCodes = order.getSkus().stream()
                    .map(OrderSku::getSkuCode).toList();
            // customerCode/customerGroupId 从客户域获取，此处暂用 customerId 透传
            PromotionCalculateRequest req = new PromotionCalculateRequest(
                    command.customerId(), null, skuCodes, skuItems);
            ApiResponse<PromotionCalculateResponse> resp = marketingFeignClient.calculate(req);
            if (resp != null && resp.getData() != null) {
                PromotionCalculateResponse data = resp.getData();
                if (data.discountAmount() != null
                        && data.discountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    // F-206 折扣上限：促销折扣超过商品总额×50% 时截断，防止折扣失控
                    BigDecimal capped = discountCapPolicy.cap(order.getTotalAmount(), data.discountAmount());
                    order.applyDiscount(capped);
                }
                if (data.depositAmount() != null
                        && data.depositAmount().compareTo(BigDecimal.ZERO) > 0) {
                    order.applyDeposit(data.depositAmount());
                }
                return;
            }
        } catch (Exception ex) {
            log.warn("marketing-service 促销/押金计算失败 orderId={}, 降级到本地押金计算",
                    order.getId().value(), ex);
        }
        // 降级：仅本地押金计算
        depositCalculator.applyDeposit(order, command.depositConfigBySku());
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

        // 释放信用预占（F-403：取消订单必须释放信用，失败不阻断流程，对账任务兜底）
        try {
            customerFeignClient.releaseCredit(order.getCustomerId(),
                    new CustomerFeignClient.CreditOperationRequest(order.getPayableAmount()));
        } catch (Exception ex) {
            log.error("取消订单时信用释放失败 orderId={}, customerId={}, amount={}",
                    orderId, order.getCustomerId(), order.getPayableAmount(), ex);
        }

        order.cancel();
        orderRepository.save(order);
        // F-312 预购：订单关闭时删除预购占用记录（业务文档七节）
        if (order.isPreOrder()) {
            preOrderRecordRepository.deleteByOrderNo(order.getOrderNo());
        }
        return OrderResult.from(order);
    }

    /**
     * 超时关单（用例：task-service 定时调度调用，F-801 状态对账兜底）。
     *
     * <p>查询超时未确认订单（status=TO_BE_CONFIRMED 且 createTime ≤ now - minutes），
     * 逐单关闭：释放信用预占 → 状态机迁移终态 → 落库。幂等由订单状态机守卫
     * （已审单/已关闭的订单重复关闭被非法迁移拦截）。</p>
     *
     * @param minutes 超时分钟数
     * @return 关闭结果（关闭数量 + 订单编号列表）
     */
    @Transactional
    public TimeoutCloseResult timeoutClose(int minutes) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(Math.max(minutes, 1));
        List<Order> timeoutOrders = orderRepository.findTimeoutOrders(
                OrderStatus.TO_BE_CONFIRMED, deadline, 100);
        List<String> closedOrderNos = new ArrayList<>();
        for (Order order : timeoutOrders) {
            try {
                releaseCreditQuietly(order);
                order.cancel();
                orderRepository.save(order);
                if (order.isPreOrder()) {
                    preOrderRecordRepository.deleteByOrderNo(order.getOrderNo());
                }
                closedOrderNos.add(order.getOrderNo());
                log.info("超时关单 orderNo={} minutes={}", order.getOrderNo(), minutes);
            } catch (Exception ex) {
                log.warn("超时关单失败 orderNo={}, error={}", order.getOrderNo(), ex.getMessage());
            }
        }
        log.info("超时关单完成: 扫描 {} 单, 关闭 {} 单", timeoutOrders.size(), closedOrderNos.size());
        return TimeoutCloseResult.of(closedOrderNos.size(), closedOrderNos);
    }

    /** 释放信用预占（失败不阻断流程，对账任务兜底，F-403）。 */
    private void releaseCreditQuietly(Order order) {
        try {
            customerFeignClient.releaseCredit(order.getCustomerId(),
                    new CustomerFeignClient.CreditOperationRequest(order.getPayableAmount()));
        } catch (Exception ex) {
            log.error("释放信用失败 orderNo={}, customerId={}, amount={}",
                    order.getOrderNo(), order.getCustomerId(), order.getPayableAmount(), ex);
        }
    }

    /**
     * 修改订单（F-309 改单）：仅待确认状态允许，替换明细 + 重算金额 + 整托校验。
     *
     * <p>金额变化后按差额调整信用预占：应付增加则补占用，减少则释放（F-403 保持一致）。</p>
     */
    @Transactional
    public OrderResult modifyOrder(String orderId, List<OrderCreateCommand.SkuItem> skuItems,
                                   Map<String, BigDecimal> palletSpecs) {
        Order order = orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new BizException("200002", "订单不存在: " + orderId));

        BigDecimal beforePayable = order.getPayableAmount();

        List<OrderSku> newSkus = skuItems.stream()
                .map(s -> new OrderSku(s.skuCode(), s.skuName(), s.quantity(), s.price()))
                .toList();
        order.modifySkus(newSkus);
        // 改单后的明细同样要过整托校验（业务文档五节）
        reviewDomainService.validateWholePallet(order, palletSpecs);

        // 信用预占按差额调整（改单后应付变化）
        BigDecimal delta = order.getPayableAmount().subtract(beforePayable);
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            customerFeignClient.occupyCredit(order.getCustomerId(),
                    new CustomerFeignClient.CreditOperationRequest(delta));
        } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
            customerFeignClient.releaseCredit(order.getCustomerId(),
                    new CustomerFeignClient.CreditOperationRequest(delta.abs()));
        }

        orderRepository.save(order);
        return OrderResult.from(order);
    }

    /**
     * M4：子单状态回传——聚合父单状态（蓝图 §4.3）。
     *
     * <p>当某张子单发货/签收后，履约服务回调本方法。本方法：</p>
     * <ol>
     *   <li>根据子单的 parentOrderId 定位父单</li>
     *   <li>加载全部子单，统计已发货（含已签收）/已签收数量</li>
     *   <li>调用 {@link Order#aggregateChildStatus} 计算并迁移父单状态</li>
     *   <li>状态变更时保存父单并发布事件</li>
     * </ol>
     *
     * <p>幂等：重复回传同一子单状态时，聚合结果不变，{@code aggregateChildStatus} 返回 false，不重复迁移。</p>
     *
     * @param childOrderId 发生状态变更的子单 ID
     */
    @Transactional
    public void handleChildStatusCallback(String childOrderId) {
        Order child = orderRepository.findById(new OrderId(childOrderId))
                .orElseThrow(() -> new BizException("200002", "子单不存在: " + childOrderId));
        if (!child.isChildOrder()) {
            // 非子单（普通单/父单），无需聚合
            return;
        }

        Order parent = orderRepository.findById(new OrderId(child.getParentOrderId()))
                .orElseThrow(() -> new BizException("200002", "父单不存在: " + child.getParentOrderId()));
        List<Order> children = orderRepository.findByParentOrderId(parent.getId().value());
        if (children.isEmpty()) {
            return;
        }

        // 统计：已发货（含已签收）数 / 已签收数
        int shippedCount = 0;
        int signedCount = 0;
        for (Order c : children) {
            OrderStatus s = c.getStatus();
            if (s == OrderStatus.SIGNED) {
                signedCount++;
                shippedCount++;
            } else if (s == OrderStatus.SHIPPED) {
                shippedCount++;
            }
        }

        OrderStatus from = parent.getStatus();
        boolean changed = parent.aggregateChildStatus(shippedCount, signedCount, children.size());
        if (changed) {
            orderRepository.save(parent);
            eventPublisher.publishEvent(new OrderStatusChangedEvent(
                    parent.getId(), parent.getOrderNo(), from, parent.getStatus(),
                    "SYSTEM", parent.getUpdateTime()));
            log.info("M4 父单状态聚合 orderNo={} {} → {} (shipped={}/{}, signed={}/{})",
                    parent.getOrderNo(), from, parent.getStatus(),
                    shippedCount, children.size(), signedCount, children.size());
        }
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
