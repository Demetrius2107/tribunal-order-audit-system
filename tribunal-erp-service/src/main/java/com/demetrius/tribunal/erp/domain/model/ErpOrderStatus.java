package com.demetrius.tribunal.erp.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * ERP 履约订单状态机。
 *
 * <p>对应需求：F-503（发货/签收回传）、F-307（转单）。</p>
 *
 * <p>状态流：已接收（转单创建）→ 已发货 → 已签收（终态）；已接收/已发货可取消或关闭。</p>
 *
 * <p>说明：状态机是幂等核心——重复回传同一状态被迁移表拒绝（对照 N-304 状态回传幂等）。</p>
 */
public enum ErpOrderStatus {

    /** 已接收（OMS 转单到达，ERP 创建履约单） */
    RECEIVED("已接收"),

    /** 已发货（ERP 完成出库） */
    SHIPPED("已发货"),

    /** 已签收（终端签收，终态） */
    SIGNED("已签收"),

    /** 已关闭（终态） */
    CLOSED("已关闭"),

    /** 已取消（终态） */
    CANCELLED("已取消");

    private final String desc;

    /** 状态迁移表：当前状态 → 允许迁移的目标状态集合 */
    private static final Map<ErpOrderStatus, Set<ErpOrderStatus>> TRANSITIONS =
            new EnumMap<>(ErpOrderStatus.class);

    static {
        TRANSITIONS.put(RECEIVED, EnumSet.of(SHIPPED, CLOSED, CANCELLED));
        TRANSITIONS.put(SHIPPED, EnumSet.of(SIGNED, CANCELLED));
        // 终态：不可再迁移
        TRANSITIONS.put(SIGNED, EnumSet.noneOf(ErpOrderStatus.class));
        TRANSITIONS.put(CLOSED, EnumSet.noneOf(ErpOrderStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(ErpOrderStatus.class));
    }

    ErpOrderStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 校验是否允许迁移到目标状态。
     */
    public boolean canTransitTo(ErpOrderStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    @Override
    public String toString() {
        return name() + "(" + desc + ")";
    }
}
