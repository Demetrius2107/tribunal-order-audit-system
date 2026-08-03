package com.demetrius.tribunal.order.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 订单聚合根（★核心类）。
 *
 * <p>、。</p>
 *
 * <p>聚合根是 DDD 的核心：所有对订单的修改必须经过聚合根的方法，
 * 业务规则（状态机校验、金额校验）内聚在聚合内部，而不是散落在 Service 里。</p>
 *
 * <p>TODO（学习任务）——参照通用做法，注意不要在领域层注入仓储——通过领域服务/应用层编排）</li>
 *   <li>金额计算：总金额 = Σ明细金额；折扣、押金、税、运费如何参与（参照促销计算）</li>
 *   <li>信用校验：审单前校验客户信用额度（参照通用做法</li>
 *   <li>操作日志 / 状态流水：每次状态迁移写 order_status_record（参照通用做法</li>
 *   <li>幂等：同一订单重复提交/重复状态回传的处理（参照通用做法</li>
 * </ul>
 */
public class Order {

    private final OrderId id;

    /** 订单编号（业务唯一键，数据库唯一约束 → 幂等第一道防线） */
    private final String orderNo;

    private final String customerId;

    private OrderStatus status;

    /** 订单明细（聚合内实体） */
    private final List<OrderSku> skus;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payableAmount;

    /** 拒绝原因（审单拒绝时记录，参照通用做法 */
    private String rejectReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Order(OrderId id, String orderNo, String customerId, List<OrderSku> skus) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerId = customerId;
        this.skus = skus;
        this.status = OrderStatus.TO_BE_CONFIRMED;
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
        // TODO（学习任务）：初始化金额计算（可抽取到 OrderAmountCalculator 领域服务）
        this.totalAmount = skus.stream()
                .map(OrderSku::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.discountAmount = BigDecimal.ZERO;
        this.payableAmount = this.totalAmount;
    }

    /**
     * 还原构造器：从持久化数据完整还原聚合（由 restore() 调用）。
     */
    private Order(OrderId id, String orderNo, String customerId, List<OrderSku> skus,
                  OrderStatus status, BigDecimal totalAmount, BigDecimal discountAmount,
                  BigDecimal payableAmount, String rejectReason,
                  LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerId = customerId;
        this.skus = skus;
        this.status = status;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.payableAmount = payableAmount;
        this.rejectReason = rejectReason;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /**
     * 工厂方法：创建订单（初始状态 = 待确认）。
     *
     * @param id         订单 ID
     * @param orderNo    订单编号
     * @param customerId 客户 ID
     * @param skus       订单明细
     * @return 新订单聚合
     */
    public static Order create(OrderId id, String orderNo, String customerId, List<OrderSku> skus) {
        if (skus == null || skus.isEmpty()) {
            throw new IllegalArgumentException("订单明细不能为空");
        }
        return new Order(id, orderNo, customerId, new ArrayList<>(skus));
    }

    /**
     * 还原工厂方法：从持久化数据完整还原聚合（仓储读取时使用）。
     *
     * <p>与 create() 的区别：create 用于新订单（初始状态待确认、金额由 SKU 计算），
     * restore 用于还原已有订单（状态/金额/时间戳均来自数据库，不做任何重算）。</p>
     *
     * @param status         已持久化的状态
     * @param totalAmount    已持久化的总金额
     * @param discountAmount 已持久化的折扣金额
     * @param payableAmount  已持久化的应付金额
     * @param rejectReason   拒绝原因（可能为 null）
     * @param createTime     创建时间
     * @param updateTime     更新时间
     */
    public static Order restore(OrderId id, String orderNo, String customerId, List<OrderSku> skus,
                                OrderStatus status, BigDecimal totalAmount, BigDecimal discountAmount,
                                BigDecimal payableAmount, String rejectReason,
                                LocalDateTime createTime, LocalDateTime updateTime) {
        return new Order(id, orderNo, customerId, skus, status, totalAmount, discountAmount,
                payableAmount, rejectReason, createTime, updateTime);
    }

    /** 审单通过：待确认 → 已确认 */
    public void confirm() {
        transitTo(OrderStatus.CONFIRMED);
    }

    /**
     * 重算订单金额（明细重新定价后调用，金额规则集中在 OrderAmountCalculator）。
     *
     * <p>总金额 = Σ明细金额；应付金额 = 总金额 - 折扣金额。</p>
     */
    public void recalculateAmounts() {
        this.totalAmount = skus.stream()
                .map(OrderSku::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.payableAmount = this.totalAmount.subtract(this.discountAmount == null
                ? BigDecimal.ZERO : this.discountAmount);
    }

    /**
     * 应用折扣并重算金额（F-202 促销/折扣基础版；折扣金额不得大于总金额）。
     */
    public void applyDiscount(BigDecimal discount) {
        if (discount == null || discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("折扣金额不能为负");
        }
        this.discountAmount = discount;
        recalculateAmounts();
        if (this.payableAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("折扣金额不能超过订单总金额");
        }
    }

    /** 审单拒绝：待确认 → 已拒绝 */
    public void reject(String reason) {
        transitTo(OrderStatus.REJECTED);
        this.rejectReason = reason;
    }

    /** 转单：已确认 → 转单中（参照通用做法 */
    public void startTransfer() {
        transitTo(OrderStatus.TRANSFERRING);
    }

    /** 转单成功：转单中 → 已转单（参照通用做法 */
    public void transferSuccess() {
        transitTo(OrderStatus.TRANSFERRED);
    }

    /** 发货：已转单 → 已发货（参照通用做法 */
    public void ship() {
        transitTo(OrderStatus.SHIPPED);
    }

    /** 签收：已发货 → 已签收 */
    public void sign() {
        transitTo(OrderStatus.SIGNED);
    }

    /** 取消订单 */
    public void cancel() {
        transitTo(OrderStatus.CANCELLED);
    }

    /**
     * 统一状态迁移入口（★状态机 = 幂等的核心）。
     *
     * <p>参照通用做法。</p>
     *
     * @param target 目标状态
     */
    private void transitTo(OrderStatus target) {
        if (!status.canTransitTo(target)) {
            throw new IllegalStateException(
                    "非法状态迁移: " + status + " -> " + target);
        }
        this.status = target;
        this.updateTime = LocalDateTime.now();
        // TODO（学习任务）：发布 OrderStatusChangedEvent（由应用层统一发布，领域层只记录）
    }

    // ---------- getters ----------

    public OrderId getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderSku> getSkus() {
        return Collections.unmodifiableList(skus);
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
}
