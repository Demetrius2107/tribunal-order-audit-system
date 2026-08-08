package com.demetrius.tribunal.order.domain.model;

/**
 * 合单（发货单）状态机。
 *
 * <pre>
 *   CREATED（已创建，待打包）
 *     ├──→ PACKED（已打包，待发货）
 *     │       └──→ SHIPPED（已发货）
 *     │              └──→ DELIVERED（已送达，终态）
 *     └──→ CANCELLED（已取消，终态）
 * </pre>
 *
 * <p>说明：合单是拆单的对称操作——拆单把 1 个父单按仓库拆成 N 个子单，
 * 合单把 N 个同收货人订单合并成 1 个发货单。</p>
 */
public enum MergeOrderStatus {

    /** 已创建，待打包 */
    CREATED,

    /** 已打包，待发货 */
    PACKED,

    /** 已发货 */
    SHIPPED,

    /** 已送达（终态） */
    DELIVERED,

    /** 已取消（终态） */
    CANCELLED;

    /**
     * 状态流转校验。
     *
     * @param target 目标状态
     * @throws IllegalStateException 非法迁移
     */
    public void transitionTo(MergeOrderStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                    "非法状态迁移: " + this + " -> " + target);
        }
    }

    private boolean canTransitionTo(MergeOrderStatus target) {
        return switch (this) {
            case CREATED   -> target == PACKED || target == CANCELLED;
            case PACKED    -> target == SHIPPED;
            case SHIPPED   -> target == DELIVERED;
            case DELIVERED, CANCELLED -> false; // 终态
        };
    }
}
