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

    /** 订单类型（普通/预购，业务文档七节） */
    private final OrderType orderType;

    /** 是否拼车订单（业务文档八节：whether_the_car_pool，多经销商合并一车运输） */
    private boolean carPooling;

    /** 是否已参与拼车（拼车组确认后置 true；已拼车订单不可关闭，CARPOOL_CANNOT_BE_CLOSED） */
    private boolean carPoolJoined;

    private OrderStatus status;

    /** 订单明细（聚合内实体） */
    private final List<OrderSku> skus;

    /** 空包装回收明细（业务文档九节：可包含回收明细，参与押金计算） */
    private final List<ReturnablePackaging> returnablePackagings;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    /** 折扣池抵扣（业务文档三节：用折扣池余额冲抵应付金额） */
    private BigDecimal discountPoolDeduction;

    /** 押金（业务文档四节：包装物押金，按 SKU-客户押金配置计算） */
    private BigDecimal depositAmount;

    /** 税费（业务文档四节：与折扣、押金并列参与金额汇总） */
    private BigDecimal taxAmount;

    private BigDecimal payableAmount;

    /** 拒绝原因（审单拒绝时记录，参照通用做法 */
    private String rejectReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 还原构造器：从持久化数据完整还原聚合（由 restore() 调用）。
     */
    private Order(OrderId id, String orderNo, String customerId, OrderType orderType,
                  boolean carPooling, boolean carPoolJoined, List<OrderSku> skus,
                  List<ReturnablePackaging> returnablePackagings,
                  OrderStatus status, BigDecimal totalAmount, BigDecimal discountAmount,
                  BigDecimal discountPoolDeduction, BigDecimal depositAmount, BigDecimal taxAmount,
                  BigDecimal payableAmount, String rejectReason,
                  LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerId = customerId;
        this.orderType = orderType;
        this.carPooling = carPooling;
        this.carPoolJoined = carPoolJoined;
        this.skus = skus;
        this.returnablePackagings = returnablePackagings;
        this.status = status;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.discountPoolDeduction = discountPoolDeduction;
        this.depositAmount = depositAmount;
        this.taxAmount = taxAmount;
        this.payableAmount = payableAmount;
        this.rejectReason = rejectReason;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /**
     * 工厂方法：创建订单（初始状态 = 待确认，默认普通订单）。
     *
     * @param id         订单 ID
     * @param orderNo    订单编号
     * @param customerId 客户 ID
     * @param skus       订单明细
     * @return 新订单聚合
     */
    public static Order create(OrderId id, String orderNo, String customerId, List<OrderSku> skus) {
        return create(id, orderNo, customerId, OrderType.NORMAL, false, skus);
    }

    /**
     * 工厂方法：创建订单（初始状态 = 待确认，默认普通订单，可带空包装回收明细）。
     */
    public static Order create(OrderId id, String orderNo, String customerId,
                               OrderType orderType, boolean carPooling, List<OrderSku> skus) {
        return create(id, orderNo, customerId, orderType, carPooling, skus, List.of());
    }

    /**
     * 工厂方法：创建订单（初始状态 = 待确认，可指定订单类型/拼车/回收明细）。
     */
    public static Order create(OrderId id, String orderNo, String customerId,
                               OrderType orderType, boolean carPooling, List<OrderSku> skus,
                               List<ReturnablePackaging> returnablePackagings) {
        if (skus == null || skus.isEmpty()) {
            throw new IllegalArgumentException("订单明细不能为空");
        }
        List<ReturnablePackaging> rps = returnablePackagings == null
                ? new ArrayList<>() : new ArrayList<>(returnablePackagings);
        BigDecimal total = skus.stream()
                .map(OrderSku::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 应付金额 = 商品总额 + 空包装回收押金（业务文档九节）
        BigDecimal returnableDeposit = rps.stream()
                .map(ReturnablePackaging::getDepositAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payable = total.add(returnableDeposit);
        LocalDateTime now = LocalDateTime.now();
        return new Order(id, orderNo, customerId, orderType, carPooling, false,
                new ArrayList<>(skus), rps,
                OrderStatus.TO_BE_CONFIRMED,
                total, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                payable, null, now, now);
    }

    /**
     * 还原工厂方法：从持久化数据完整还原聚合（仓储读取时使用）。
     */
    public static Order restore(OrderId id, String orderNo, String customerId, OrderType orderType,
                                boolean carPooling, boolean carPoolJoined, List<OrderSku> skus,
                                List<ReturnablePackaging> returnablePackagings,
                                OrderStatus status, BigDecimal totalAmount, BigDecimal discountAmount,
                                BigDecimal discountPoolDeduction, BigDecimal depositAmount, BigDecimal taxAmount,
                                BigDecimal payableAmount, String rejectReason,
                                LocalDateTime createTime, LocalDateTime updateTime) {
        return new Order(id, orderNo, customerId, orderType, carPooling, carPoolJoined,
                skus, returnablePackagings,
                status, totalAmount, discountAmount,
                discountPoolDeduction, depositAmount, taxAmount,
                payableAmount, rejectReason, createTime, updateTime);
    }

    /** 审单通过：待确认 → 已确认 */
    public void confirm() {
        transitTo(OrderStatus.CONFIRMED);
    }

    /**
     * 修改订单明细（F-309 改单）：仅待确认状态允许修改，修改后重算金额。
     *
     * @param newSkus 新的订单明细
     */
    public void modifySkus(List<OrderSku> newSkus) {
        if (status != OrderStatus.TO_BE_CONFIRMED) {
            throw new IllegalStateException("仅待确认状态的订单允许修改，当前状态: " + status);
        }
        if (newSkus == null || newSkus.isEmpty()) {
            throw new IllegalArgumentException("订单明细不能为空");
        }
        this.skus.clear();
        this.skus.addAll(newSkus);
        this.updateTime = LocalDateTime.now();
        recalculateAmounts();
    }

    /**
     * 重算订单金额（明细重新定价后调用，金额规则集中在 OrderAmountCalculator）。
     *
     * <p>业务文档三/四/九节：应付金额 = 总金额 - 折扣 - 折扣池抵扣 + 押金 + 税 + 空包装回收押金。</p>
     */
    public void recalculateAmounts() {
        this.totalAmount = skus.stream()
                .map(OrderSku::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal returnableDeposit = returnablePackagings.stream()
                .map(ReturnablePackaging::getDepositAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.payableAmount = this.totalAmount
                .subtract(nz(discountAmount))
                .subtract(nz(discountPoolDeduction))
                .add(nz(depositAmount))
                .add(nz(taxAmount))
                .add(returnableDeposit);
        if (this.payableAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("订单应付金额不能为负（折扣/折扣池抵扣过大）");
        }
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

    /** 应用折扣池抵扣（业务文档三节：用折扣池余额冲抵应付金额） */
    public void applyDiscountPoolDeduction(BigDecimal deduction) {
        if (deduction == null || deduction.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("折扣池抵扣金额不能为负");
        }
        this.discountPoolDeduction = deduction;
        recalculateAmounts();
    }

    /** 应用押金（业务文档四节：包装物押金参与金额汇总） */
    public void applyDeposit(BigDecimal deposit) {
        if (deposit == null || deposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("押金金额不能为负");
        }
        this.depositAmount = deposit;
        recalculateAmounts();
    }

    /** 应用税费（业务文档四节：税参与金额汇总） */
    public void applyTax(BigDecimal tax) {
        if (tax == null || tax.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("税费不能为负");
        }
        this.taxAmount = tax;
        recalculateAmounts();
    }

    /** null 安全：空值按 0 处理 */
    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
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

    /** 取消订单（预购单走专用终态 PRE_ORDER_ENDED，业务文档七节：998 预购已结束） */
    public void cancel() {
        // 已参与拼车的订单不可关闭（业务文档八节：CARPOOL_CANNOT_BE_CLOSED）
        if (carPoolJoined) {
            throw new IllegalStateException("已参与拼车的订单不可关闭（CARPOOL_CANNOT_BE_CLOSED）");
        }
        if (orderType == OrderType.PRE_ORDER) {
            transitTo(OrderStatus.PRE_ORDER_ENDED);
        } else {
            transitTo(OrderStatus.CANCELLED);
        }
    }

    /** 标记参与拼车（拼车组确认后调用；仅拼车订单可加入） */
    public void joinCarPool() {
        if (!carPooling) {
            throw new IllegalStateException("非拼车订单不能参与拼车");
        }
        this.carPoolJoined = true;
        this.updateTime = LocalDateTime.now();
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

    public OrderType getOrderType() {
        return orderType;
    }

    /** 是否预购订单（预购单走独立终态与信用口径，业务文档七节） */
    public boolean isPreOrder() {
        return orderType == OrderType.PRE_ORDER;
    }

    /** 是否拼车订单（业务文档八节） */
    public boolean isCarPooling() {
        return carPooling;
    }

    /** 是否已参与拼车（已拼车订单不可关闭） */
    public boolean isCarPoolJoined() {
        return carPoolJoined;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderSku> getSkus() {
        return Collections.unmodifiableList(skus);
    }

    public List<ReturnablePackaging> getReturnablePackagings() {
        return Collections.unmodifiableList(returnablePackagings);
    }

    /** 空包装回收押金合计（业务文档九节：回收参与金额/押金计算） */
    public BigDecimal getReturnableDepositTotal() {
        return returnablePackagings.stream()
                .map(ReturnablePackaging::getDepositAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getDiscountPoolDeduction() {
        return discountPoolDeduction;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
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
