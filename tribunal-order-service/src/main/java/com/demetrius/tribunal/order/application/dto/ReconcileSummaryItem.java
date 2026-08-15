package com.demetrius.tribunal.order.application.dto;

/**
 * 对账差异汇总项（对账结果产品化：按差异类型/处理状态计数）。
 */
public record ReconcileSummaryItem(
        String recordType,
        String status,
        long count) {
}
