package com.demetrius.tribunal.order.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 售后单状态机。
 *
 * <p>状态流转：</p>
 * <pre>
 *   PENDING（待审核）
 *     ├──→ APPROVED（已审核/待退货入库）
 *     │      └──→ COMPLETED（已完成，已退款）
 *     └──→ REJECTED（已拒绝，终态）
 * </pre>
 *
 * <p>设计要点：与 {@link OrderStatus} 一致，状态迁移表用静态块 + EnumMap 构建，
 * 非法迁移直接抛异常——状态机是幂等和业务安全的保障。</p>
 */
public enum AfterSaleStatus {

    /** 待审核（客户发起退货申请，等待客服/系统审核） */
    PENDING("待审核"),

    /** 已审核（审核通过，等待客户寄回商品 / 仅退款直接完成） */
    APPROVED("已审核"),

    /** 已完成（退款已执行，终态） */
    COMPLETED("已完成"),

    /** 已拒绝（审核拒绝，终态） */
    REJECTED("已拒绝");

    private final String desc;

    private static final Map<AfterSaleStatus, Set<AfterSaleStatus>> TRANSITIONS =
            new EnumMap<>(AfterSaleStatus.class);

    static {
        TRANSITIONS.put(PENDING, EnumSet.of(APPROVED, REJECTED));
        TRANSITIONS.put(APPROVED, EnumSet.of(COMPLETED));
        // 终态不可再迁移
        TRANSITIONS.put(COMPLETED, EnumSet.noneOf(AfterSaleStatus.class));
        TRANSITIONS.put(REJECTED, EnumSet.noneOf(AfterSaleStatus.class));
    }

    AfterSaleStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public boolean canTransitTo(AfterSaleStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    @Override
    public String toString() {
        return name() + "(" + desc + ")";
    }
}
