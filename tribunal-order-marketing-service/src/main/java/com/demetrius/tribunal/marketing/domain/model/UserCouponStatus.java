package com.demetrius.tribunal.marketing.domain.model;

/**
 * 用户券状态机。
 *
 * <pre>
 *   AVAILABLE（可使用）
 *     ├──→ LOCKED（已锁定，下单时预占）
 *     │       ├──→ USED（已核销，终态）
 *     │       └──→ AVAILABLE（取消订单时释放回滚）
 *     ├──→ USED（直接核销，终态）
 *     └──→ EXPIRED（已过期，终态）
 * </pre>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>LOCKED 状态防止同一张券在订单未完成支付时被重复使用</li>
 *   <li>订单取消/超时未支付时，券释放回 AVAILABLE</li>
 *   <li>订单支付成功后，券从 LOCKED 转 USED</li>
 * </ul>
 */
public enum UserCouponStatus {

    /** 可使用 */
    AVAILABLE,

    /** 已锁定（下单预占） */
    LOCKED,

    /** 已核销（终态） */
    USED,

    /** 已过期（终态） */
    EXPIRED;

    /**
     * 状态流转校验。
     *
     * @param target 目标状态
     * @throws IllegalStateException 非法迁移
     */
    public void transitionTo(UserCouponStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException("非法券状态迁移: " + this + " -> " + target);
        }
    }

    private boolean canTransitionTo(UserCouponStatus target) {
        return switch (this) {
            case AVAILABLE -> target == LOCKED || target == USED || target == EXPIRED;
            case LOCKED    -> target == USED || target == AVAILABLE;
            case USED, EXPIRED -> false; // 终态
        };
    }
}
