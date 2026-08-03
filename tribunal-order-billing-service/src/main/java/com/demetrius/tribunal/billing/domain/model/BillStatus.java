package com.demetrius.tribunal.billing.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 金融账单状态机。
 *
 * <p>对应需求：F-404（收款确认）、F-308（状态回传）、N-304（回传幂等）。</p>
 *
 * <p>状态流：已生成（订单转来生成账单）→ 已确认 → 已结算 / 已核销（终态）；已生成/已确认可取消。</p>
 *
 * <p>说明：状态机是幂等核心——重复回传同一状态被迁移表拒绝。</p>
 */
public enum BillStatus {

    /** 已生成（订单服务转单到达，创建账单） */
    GENERATED("已生成"),

    /** 已确认（账单审核通过，待收款） */
    CONFIRMED("已确认"),

    /** 已结算（款项到位，终态） */
    SETTLED("已结算"),

    /** 已核销（账务核销完成，终态） */
    VERIFIED("已核销"),

    /** 已取消（终态） */
    CANCELLED("已取消");

    private final String desc;

    /** 状态迁移表：当前状态 → 允许迁移的目标状态集合 */
    private static final Map<BillStatus, Set<BillStatus>> TRANSITIONS =
            new EnumMap<>(BillStatus.class);

    static {
        TRANSITIONS.put(GENERATED, EnumSet.of(CONFIRMED, CANCELLED));
        TRANSITIONS.put(CONFIRMED, EnumSet.of(SETTLED, VERIFIED, CANCELLED));
        // 终态：不可再迁移
        TRANSITIONS.put(SETTLED, EnumSet.noneOf(BillStatus.class));
        TRANSITIONS.put(VERIFIED, EnumSet.noneOf(BillStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(BillStatus.class));
    }

    BillStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 校验是否允许迁移到目标状态。
     */
    public boolean canTransitTo(BillStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    @Override
    public String toString() {
        return name() + "(" + desc + ")";
    }
}
