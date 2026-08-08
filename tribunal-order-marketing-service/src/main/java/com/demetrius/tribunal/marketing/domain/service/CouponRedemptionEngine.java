package com.demetrius.tribunal.marketing.domain.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.marketing.domain.model.UserCoupon;
import com.demetrius.tribunal.marketing.domain.repository.UserCouponRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券核销引擎（M4 领域服务）。
 *
 * <p>封装用户券的状态流转操作，供应用层/订单链路调用：</p>
 * <ul>
 *   <li>{@link #lock} — 下单时锁定（预占，防重复使用）</li>
 *   <li>{@link #release} — 订单取消/超时未支付时释放回滚</li>
 *   <li>{@link #use} — 订单支付成功后核销（终态）</li>
 *   <li>{@link #calculateDiscount} — 抵扣金额试算（只读）</li>
 * </ul>
 *
 * <p>防刷：所有写操作先经 {@link UserCoupon} 内部校验有效期与状态机合法性，
 * 已核销/已过期的券无法被再次锁定或核销。</p>
 */
@Service
public class CouponRedemptionEngine {

    private final UserCouponRepository userCouponRepository;

    public CouponRedemptionEngine(UserCouponRepository userCouponRepository) {
        this.userCouponRepository = userCouponRepository;
    }

    /**
     * 锁定券（下单时预占，防止重复使用）。
     *
     * @param couponCode 券码
     * @param orderId    关联订单 ID
     * @return 锁定后的用户券
     */
    public UserCoupon lock(String couponCode, String orderId) {
        UserCoupon coupon = loadOrThrow(couponCode);
        coupon.lock(orderId);
        userCouponRepository.save(coupon);
        return coupon;
    }

    /**
     * 释放券（订单取消/超时未支付时回滚到 AVAILABLE）。
     *
     * @param couponCode 券码
     * @return 释放后的用户券
     */
    public UserCoupon release(String couponCode) {
        UserCoupon coupon = loadOrThrow(couponCode);
        coupon.release();
        userCouponRepository.save(coupon);
        return coupon;
    }

    /**
     * 核销券（订单支付成功后，终态）。
     *
     * @param couponCode 券码
     * @param orderId    关联订单 ID
     * @return 核销后的用户券
     */
    public UserCoupon use(String couponCode, String orderId) {
        UserCoupon coupon = loadOrThrow(couponCode);
        coupon.use(orderId);
        userCouponRepository.save(coupon);
        return coupon;
    }

    /**
     * 试算券可抵扣金额（只读，不改变券状态）。
     *
     * @param couponCode  券码
     * @param orderAmount 订单金额（促销后）
     * @return 抵扣金额（无效券/过期券返回 0）
     */
    public BigDecimal calculateDiscount(String couponCode, BigDecimal orderAmount) {
        UserCoupon coupon = loadOrThrow(couponCode);
        if (!coupon.isValid(LocalDateTime.now())) {
            return BigDecimal.ZERO;
        }
        return coupon.calculateDiscount(orderAmount);
    }

    private UserCoupon loadOrThrow(String couponCode) {
        return userCouponRepository.findByCouponCode(couponCode)
                .orElseThrow(() -> new BizException("404202", "用户券不存在: " + couponCode));
    }
}
