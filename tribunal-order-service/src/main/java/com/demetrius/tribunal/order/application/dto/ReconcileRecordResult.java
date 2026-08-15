package com.demetrius.tribunal.order.application.dto;

import com.demetrius.tribunal.order.infrastructure.model.ReconcileRecordPo;

import java.time.LocalDateTime;

/**
 * 对账差异记录查询出参（F-801/F-802：对账结果产品化）。
 */
public record ReconcileRecordResult(
        String id,
        String taskCode,
        String recordType,
        String refNo,
        String detail,
        String status,
        Integer autoFixed,
        LocalDateTime createTime,
        LocalDateTime fixTime) {

    public static ReconcileRecordResult from(ReconcileRecordPo po) {
        return new ReconcileRecordResult(
                po.getId(),
                po.getTaskCode(),
                po.getRecordType(),
                po.getRefNo(),
                po.getDetail(),
                po.getStatus(),
                po.getAutoFixed(),
                po.getCreateTime(),
                po.getFixTime());
    }
}
