package com.demetrius.tribunal.billing.application.dto;

import java.util.List;

/**
 * 账单分页查询出参（对外报表）。
 */
public record BillPageResult(
        long total,
        long pageNum,
        long pageSize,
        List<BillListItemResult> bills) {

    public static BillPageResult of(long total, long pageNum, long pageSize,
                                    List<BillListItemResult> bills) {
        return new BillPageResult(total, pageNum, pageSize, bills);
    }
}
