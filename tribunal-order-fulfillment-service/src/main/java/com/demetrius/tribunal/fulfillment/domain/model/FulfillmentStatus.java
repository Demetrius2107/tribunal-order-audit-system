package com.demetrius.tribunal.fulfillment.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 履约单状态机。
 *
 * <p>对应需求：F-506（发货/签收回传）、下游履约执行。</p>
 *
 * <p>状态流：已生成（结算后创建履约单）→ 已发货 → 已签收（终态）；已生成/已发货可取消。</p>
 *
 * <p>说明：状态机是幂等核心——重复回传同一状态被迁移表拒绝。</p>
 */
public enum FulfillmentStatus {

    /** 已生成（账单结算后创建履约单） */
    GENERATED("已生成"),

    /** 已发货（出库完成） */
    SHIPPED("已发货"),

    /** 已签收（终端签收，终态） */
    SIGNED("已签收"),

    /** 已取消（终态） */
    CANCELLED("已取消");

    private final String desc;

    private static final Map<FulfillmentStatus, Set<FulfillmentStatus>> TRANSITIONS =
            new EnumMap<>(FulfillmentStatus.class);

    static {
        TRANSITIONS.put(GENERATED, EnumSet.of(SHIPPED, CANCELLED));
        TRANSITIONS.put(SHIPPED, EnumSet.of(SIGNED, CANCELLED));
        TRANSITIONS.put(SIGNED, EnumSet.noneOf(FulfillmentStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(FulfillmentStatus.class));
    }

    FulfillmentStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public boolean canTransitTo(FulfillmentStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    @Override
    public String toString() {
        return name() + "(" + desc + ")";
    }
}
