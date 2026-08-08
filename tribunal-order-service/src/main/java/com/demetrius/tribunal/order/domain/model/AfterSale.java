package com.demetrius.tribunal.order.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 售后单聚合根。
 *
 * <p>售后退货是订单生命周期的逆向流程：客户对已签收订单发起退货/退款，
 * 系统审核后执行退款 + 库存回滚 + 押金退还。</p>
 *
 * <p>核心业务规则：</p>
 * <ul>
 *   <li>退款金额按退货 SKU 单价计算，押金按比例分摊退还</li>
 *   <li>退货退款：审核通过后需等待仓库确认收货，再执行退款</li>
 *   <li>仅退款：审核通过后直接执行退款，不涉及库存回滚</li>
 *   <li>所有金额保留两位小数，HALF_UP 舍入</li>
 * </ul>
 */
public class AfterSale {

    /** 售后单 ID */
    private final String id;

    /** 售后单号（业务唯一键） */
    private final String afterSaleNo;

    /** 原订单 ID */
    private final String orderId;

    /** 原订单编号 */
    private final String orderNo;

    /** 客户 ID */
    private final String customerId;

    /** 售后类型 */
    private final AfterSaleType type;

    /** 售后原因 */
    private final AfterSaleReason reason;

    /** 售后明细 */
    private final List<AfterSaleItem> items;

    /** 状态 */
    private AfterSaleStatus status;

    /** 退款总额（商品退款 + 押金退还） */
    private BigDecimal totalRefundAmount;

    /** 拒绝原因 */
    private String rejectReason;

    /** 退款流水号（调用金融结算后回填） */
    private String refundTxnNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 还原构造器（仓储读取持久化数据时调用）。
     */
    private AfterSale(String id, String afterSaleNo, String orderId, String orderNo,
                      String customerId, AfterSaleType type, AfterSaleReason reason,
                      List<AfterSaleItem> items, AfterSaleStatus status,
                      BigDecimal totalRefundAmount, String rejectReason, String refundTxnNo,
                      LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.afterSaleNo = afterSaleNo;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.customerId = customerId;
        this.type = type;
        this.reason = reason;
        this.items = items;
        this.status = status;
        this.totalRefundAmount = totalRefundAmount;
        this.rejectReason = rejectReason;
        this.refundTxnNo = refundTxnNo;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /**
     * 工厂方法：发起售后申请。
     *
     * <p>根据原订单和退货明细计算退款金额，押金按退货金额占订单总额比例分摊退还。</p>
     *
     * @param id           售后单 ID
     * @param afterSaleNo  售后单号
     * @param order        原订单聚合
     * @param type         售后类型
     * @param reason       售后原因
     * @param returnItems  退货明细（skuCode + returnQty）
     * @return 新售后单（初始状态 PENDING）
     */
    public static AfterSale create(String id, String afterSaleNo, Order order,
                                   AfterSaleType type, AfterSaleReason reason,
                                   List<ReturnRequest> returnItems) {
        if (returnItems == null || returnItems.isEmpty()) {
            throw new IllegalArgumentException("退货明细不能为空");
        }
        if (order.getStatus() != OrderStatus.SIGNED) {
            throw new IllegalStateException("仅已签收订单允许售后: 当前状态=" + order.getStatus());
        }

        BigDecimal orderTotal = order.getTotalAmount();
        BigDecimal orderDeposit = order.getDepositAmount() == null
                ? BigDecimal.ZERO : order.getDepositAmount();

        List<AfterSaleItem> items = new ArrayList<>();
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalDepositRefund = BigDecimal.ZERO;

        for (ReturnRequest req : returnItems) {
            OrderSku sku = order.getSkus().stream()
                    .filter(s -> s.getSkuCode().equals(req.skuCode()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("退货 SKU 不在原订单中: " + req.skuCode()));

            // 商品退款 = 单价 × 退货数量
            BigDecimal itemRefund = sku.getPrice().multiply(req.quantity())
                    .setScale(2, RoundingMode.HALF_UP);

            // 押金退还 = 订单总押金 × (该 SKU 退款额 / 订单总额)
            BigDecimal itemDepositRefund = BigDecimal.ZERO;
            if (orderDeposit.compareTo(BigDecimal.ZERO) > 0 && orderTotal.compareTo(BigDecimal.ZERO) > 0) {
                itemDepositRefund = orderDeposit.multiply(itemRefund)
                        .divide(orderTotal, 2, RoundingMode.HALF_UP);
            }

            items.add(new AfterSaleItem(sku.getSkuCode(), sku.getSkuName(),
                    req.quantity(), itemRefund, itemDepositRefund));
            totalRefund = totalRefund.add(itemRefund);
            totalDepositRefund = totalDepositRefund.add(itemDepositRefund);
        }

        // 尾差吸收：押金退还总额与订单押金的差额，加到最后一条明细
        BigDecimal depositDiff = orderDeposit.subtract(totalDepositRefund);
        if (depositDiff.compareTo(BigDecimal.ZERO) > 0 && !items.isEmpty()) {
            AfterSaleItem last = items.getLast();
            items.set(items.size() - 1, new AfterSaleItem(last.skuCode(), last.skuName(),
                    last.quantity(), last.refundAmount(), last.depositRefund().add(depositDiff)));
            totalDepositRefund = totalDepositRefund.add(depositDiff);
        }

        BigDecimal finalRefund = totalRefund.add(totalDepositRefund).setScale(2, RoundingMode.HALF_UP);
        LocalDateTime now = LocalDateTime.now();
        return new AfterSale(id, afterSaleNo, order.getId().value(), order.getOrderNo(),
                order.getCustomerId(), type, reason, items, AfterSaleStatus.PENDING,
                finalRefund, null, null, now, now);
    }

    /**
     * 还原工厂方法（仓储读取时使用）。
     */
    public static AfterSale restore(String id, String afterSaleNo, String orderId, String orderNo,
                                    String customerId, AfterSaleType type, AfterSaleReason reason,
                                    List<AfterSaleItem> items, AfterSaleStatus status,
                                    BigDecimal totalRefundAmount, String rejectReason, String refundTxnNo,
                                    LocalDateTime createTime, LocalDateTime updateTime) {
        return new AfterSale(id, afterSaleNo, orderId, orderNo, customerId, type, reason,
                items, status, totalRefundAmount, rejectReason, refundTxnNo, createTime, updateTime);
    }

    /**
     * 审核通过：PENDING → APPROVED。
     */
    public void approve() {
        transitTo(AfterSaleStatus.APPROVED);
    }

    /**
     * 审核拒绝：PENDING → REJECTED。
     */
    public void reject(String reason) {
        transitTo(AfterSaleStatus.REJECTED);
        this.rejectReason = reason;
    }

    /**
     * 完成退款：APPROVED → COMPLETED。
     *
     * @param refundTxnNo 退款流水号
     */
    public void complete(String refundTxnNo) {
        transitTo(AfterSaleStatus.COMPLETED);
        this.refundTxnNo = refundTxnNo;
    }

    private void transitTo(AfterSaleStatus target) {
        if (!status.canTransitTo(target)) {
            throw new IllegalStateException("非法售后状态迁移: " + status + " -> " + target);
        }
        this.status = target;
        this.updateTime = LocalDateTime.now();
    }

    // ---------- getters ----------

    public String getId() {
        return id;
    }

    public String getAfterSaleNo() {
        return afterSaleNo;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getCustomerId() {
        return customerId;
    }

    public AfterSaleType getType() {
        return type;
    }

    public AfterSaleReason getReason() {
        return reason;
    }

    public List<AfterSaleItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public AfterSaleStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalRefundAmount() {
        return totalRefundAmount;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public String getRefundTxnNo() {
        return refundTxnNo;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 退货申请明细（工厂方法入参）。
     *
     * @param skuCode  SKU 编码
     * @param quantity 退货数量
     */
    public record ReturnRequest(String skuCode, BigDecimal quantity) {
    }
}
