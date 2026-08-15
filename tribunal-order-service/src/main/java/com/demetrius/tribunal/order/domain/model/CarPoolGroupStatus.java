package com.demetrius.tribunal.order.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 拼车组状态机（F-310：多订单合并一车运输）。
 *
 * <p>状态流：拼车中（OPEN，可加入）→ 已确认（CONFIRMED，成员锁定）→ 已关闭（CLOSED，终态）；
 * 拼车中/已确认均可取消（CANCELLED，终态）。</p>
 */
public enum CarPoolGroupStatus {

    /** 拼车中（可加入成员） */
    OPEN("拼车中"),

    /** 已确认（成员锁定，不可再加入） */
    CONFIRMED("已确认"),

    /** 已关闭（发车完成，终态） */
    CLOSED("已关闭"),

    /** 已取消（终态） */
    CANCELLED("已取消");

    private final String desc;

    private static final Map<CarPoolGroupStatus, Set<CarPoolGroupStatus>> TRANSITIONS =
            new EnumMap<>(CarPoolGroupStatus.class);

    static {
        TRANSITIONS.put(OPEN, EnumSet.of(CONFIRMED, CANCELLED));
        TRANSITIONS.put(CONFIRMED, EnumSet.of(CLOSED, CANCELLED));
        TRANSITIONS.put(CLOSED, EnumSet.noneOf(CarPoolGroupStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(CarPoolGroupStatus.class));
    }

    CarPoolGroupStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public boolean canTransitTo(CarPoolGroupStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    @Override
    public String toString() {
        return name() + "(" + desc + ")";
    }
}
