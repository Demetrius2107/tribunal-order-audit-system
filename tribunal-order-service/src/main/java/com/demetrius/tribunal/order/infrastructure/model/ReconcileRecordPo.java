package com.demetrius.tribunal.order.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对账差异记录持久化对象（对应 t_reconcile_record 表，M3 收尾）。
 *
 * <p>对账任务发现差异时落库，替代纯日志告警；自动修复的差异标记 FIXED。</p>
 */
@Data
@TableName("t_reconcile_record")
public class ReconcileRecordPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 任务编码（OUTBOX_RECONCILE/STATUS_RECONCILE） */
    private String taskCode;

    /** 差异类型（FAILED_MSG/STALE_PENDING/BILL_MISSING/BILL_CANCELLED） */
    private String recordType;

    /** 关联单号（messageId/orderNo） */
    private String refNo;

    /** 差异描述 */
    private String detail;

    /** 处理状态（OPEN/FIXED/IGNORED） */
    private String status;

    /** 是否已自动修复 0否1是 */
    private Integer autoFixed;

    private LocalDateTime createTime;

    private LocalDateTime fixTime;
}
