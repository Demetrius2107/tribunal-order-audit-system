package com.demetrius.tribunal.marketing.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用户券聚合根（用户领取的券实例）。
 *
 * <p>状态流转由 {@link UserCouponStatus#transitionTo} 保证合法性。</p>
 *
 * <p>关键字段：</p>
 * <ul>
 *   <li>{@link #templateId} / {@link #templateNo} — 来源模板</li>
 *   <li>{@link #customerId} — 领用人</li>
 *   <li>{@link #couponCode} — 唯一券码（核销时凭此码）</li>
 *   <li>{@link #orderId} — 使用时关联的订单</li>
 * </ul>
 */
public class UserCoupon {

    private final String id;
    private final String couponCode;
    private final String templateId;
    private final String templateNo;
    private final String customerId;

    /** 券类型快照（从模板冗余，避免核销时再查模板） */
    private final CouponType type;
    private final BigDecimal threshold;
    private final BigDecimal deductionAmount;
    private final BigDecimal discountRate;

    private UserCouponStatus status;

    /** 使用/锁定时关联的订单 ID */
    private String orderId;

    private final LocalDateTime receiveTime;
    private final LocalDateTime validStartTime;
    private final LocalDateTime validEndTime;
    private LocalDateTime usedTime;

    private UserCoupon(String id, String couponCode, String templateId, String templateNo,
                      String customerId, CouponType type,
                      BigDecimal threshold, BigDecimal deductionAmount, BigDecimal discountRate,
                      UserCouponStatus status, String orderId,
                      LocalDateTime receiveTime, LocalDateTime validStartTime, LocalDateTime validEndTime,
                      LocalDateTime usedTime) {
        this.id = id;
        this.couponCode = couponCode;
        this.templateId = templateId;
        this.templateNo = templateNo;
        this.customerId = customerId;
        this.type = type;
        this.threshold = threshold;
        this.deductionAmount = deductionAmount;
        this.discountRate = discountRate;
        this.status = status;
        this.orderId = orderId;
        this.receiveTime = receiveTime;
        this.validStartTime = validStartTime;
        this.validEndTime = validEndTime;
        this.usedTime = usedTime;
    }

    /**
     * 工厂方法：领券（从模板发放）。
     *
     * @param id       用户券 ID
     * @param couponCode 券码
     * @param template  券模板
     * @param customerId 领用人
     */
    public static UserCoupon receive(String id, String couponCode,
                                     CouponTemplate template, String customerId) {
        Objects.requireNonNull(id, "用户券ID不能为空");
        Objects.requireNonNull(couponCode, "券码不能为空");
        Objects.requireNonNull(template, "券模板不能为空");
        Objects.requireNonNull(customerId, "领用人不能为空");

        LocalDateTime now = LocalDateTime.now();
        return new UserCoupon(id, couponCode, template.getId(), template.getTemplateNo(),
                customerId, template.getType(),
                template.getThreshold(), template.getDeductionAmount(), template.getDiscountRate(),
                UserCouponStatus.AVAILABLE, null,
                now, template.getValidStartTime(), template.getValidEndTime(), null);
    }

    /**
     * 还原工厂方法。
     */
    public static UserCoupon restore(String id, String couponCode, String templateId, String templateNo,
                                     String customerId, CouponType type,
                                     BigDecimal threshold, BigDecimal deductionAmount, BigDecimal discountRate,
                                     UserCouponStatus status, String orderId,
                                     LocalDateTime receiveTime, LocalDateTime validStartTime,
                                     LocalDateTime validEndTime, LocalDateTime usedTime) {
        return new UserCoupon(id, couponCode, templateId, templateNo,
                customerId, type, threshold, deductionAmount, discountRate,
                status, orderId, receiveTime, validStartTime, validEndTime, usedTime);
    }

    // ---------- 状态流转 ----------

    /** 锁定（下单时预占，防止重复使用） */
    public void lock(String orderId) {
        Objects.requireNonNull(orderId, "订单ID不能为空");
        checkExpiry();
        status.transitionTo(UserCouponStatus.LOCKED);
        this.orderId = orderId;
    }

    /** 释放（订单取消/超时未支付时回滚） */
    public void release() {
        status.transitionTo(UserCouponStatus.AVAILABLE);
        this.orderId = null;
    }

    /** 核销（订单支付成功后，LOCKED → USED 或 AVAILABLE → USED） */
    public void use(String orderId) {
        Objects.requireNonNull(orderId, "订单ID不能为空");
        checkExpiry();
        status.transitionTo(UserCouponStatus.USED);
        this.orderId = orderId;
        this.usedTime = LocalDateTime.now();
    }

    /** 过期 */
    public void expire() {
        status.transitionTo(UserCouponStatus.EXPIRED);
    }

    // ---------- 计算 ----------

    /** 券是否在有效期内 */
    public boolean isValid(LocalDateTime now) {
        if (status == UserCouponStatus.USED || status == UserCouponStatus.EXPIRED) {
            return false;
        }
        if (validStartTime != null && now.isBefore(validStartTime)) {
            return false;
        }
        return validEndTime == null || !now.isAfter(validEndTime);
    }

    private void checkExpiry() {
        LocalDateTime now = LocalDateTime.now();
        if (!isValid(now)) {
            expire();
            throw new IllegalStateException("券已过期: " + couponCode);
        }
    }

    /**
     * 计算券可抵扣的金额。
     *
     * @param orderAmount 订单金额（促销后）
     * @return 抵扣金额（0 = 不满足门槛或不适用）
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return switch (type) {
            case FULL_REDUCTION -> {
                if (threshold != null && orderAmount.compareTo(threshold) < 0) {
                    yield BigDecimal.ZERO; // 不满足门槛
                }
                yield deductionAmount.min(orderAmount); // 不超过订单金额
            }
            case DISCOUNT -> orderAmount.subtract(
                    orderAmount.multiply(discountRate)
                            .setScale(2, java.math.RoundingMode.HALF_UP));
        };
    }

    // ---------- getters ----------

    public String getId() { return id; }
    public String getCouponCode() { return couponCode; }
    public String getTemplateId() { return templateId; }
    public String getTemplateNo() { return templateNo; }
    public String getCustomerId() { return customerId; }
    public CouponType getType() { return type; }
    public BigDecimal getThreshold() { return threshold; }
    public BigDecimal getDeductionAmount() { return deductionAmount; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public UserCouponStatus getStatus() { return status; }
    public String getOrderId() { return orderId; }
    public LocalDateTime getReceiveTime() { return receiveTime; }
    public LocalDateTime getValidStartTime() { return validStartTime; }
    public LocalDateTime getValidEndTime() { return validEndTime; }
    public LocalDateTime getUsedTime() { return usedTime; }
}
