package com.demetrius.tribunal.order.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 预购活动状态机（F-312：提前采购，经销商预付/保证金模式）。
 *
 * <p>状态流：草稿（DRAFT）→ 进行中（ACTIVE，可参与）→ 已结束（ENDED，终态）；
 * 草稿/进行中均可取消（CANCELLED，终态）。</p>
 */
public enum PreOrderActivityStatus {

    /** 草稿（未上线，不可参与） */
    DRAFT("草稿"),

    /** 进行中（可参与） */
    ACTIVE("进行中"),

    /** 已结束（终态） */
    ENDED("已结束"),

    /** 已取消（终态） */
    CANCELLED("已取消");

    private final String desc;

    private static final Map<PreOrderActivityStatus, Set<PreOrderActivityStatus>> TRANSITIONS =
            new EnumMap<>(PreOrderActivityStatus.class);

    static {
        TRANSITIONS.put(DRAFT, EnumSet.of(ACTIVE, CANCELLED));
        TRANSITIONS.put(ACTIVE, EnumSet.of(ENDED, CANCELLED));
        TRANSITIONS.put(ENDED, EnumSet.noneOf(PreOrderActivityStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(PreOrderActivityStatus.class));
    }

    PreOrderActivityStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public boolean canTransitTo(PreOrderActivityStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    @Override
    public String toString() {
        return name() + "(" + desc + ")";
    }
}
