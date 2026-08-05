package com.demetrius.tribunal.order.domain.model;

/**
 * 订单类型（业务文档七节：预购单类型标识 PRE_ORDER_TYPE）。
 */
public enum OrderType {

    /**
     * 普通订单
     */
    NORMAL("普通订单"),

    /**
     * 预购订单（提前采购，保证金模式，专用终态 PRE_ORDER_ENDED）
     */
    PRE_ORDER("预购订单");

    private final String desc;

    OrderType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    @Override
    public String toString() {
        return name() + "(" + desc + ")";
    }
}
