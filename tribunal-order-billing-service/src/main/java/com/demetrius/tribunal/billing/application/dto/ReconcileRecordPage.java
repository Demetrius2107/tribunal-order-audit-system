package com.demetrius.tribunal.billing.application.dto;

import java.util.List;

/**
 * 对账差异记录分页查询出参（对账结果产品化）。
 */
public record ReconcileRecordPage(
        long total,
        long pageNum,
        long pageSize,
        List<ReconcileRecordResult> records) {

    public static ReconcileRecordPage of(long total, long pageNum, long pageSize,
                                         List<ReconcileRecordResult> records) {
        return new ReconcileRecordPage(total, pageNum, pageSize, records);
    }
}
