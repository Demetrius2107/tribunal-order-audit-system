package com.demetrius.tribunal.order.domain.model;

/**
 * 售后原因（啤酒行业标准分类）。
 */
public enum AfterSaleReason {

    QUALITY_ISSUE("质量问题"),
    DAMAGED("运输破损"),
    WRONG_ITEM("发错货"),
    EXPIRED("临期/过期"),
    MISSING("漏发/少发"),
    CUSTOMER_CHANGE("客户原因（七天无理由）"),
    OTHER("其他");

    private final String desc;

    AfterSaleReason(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
