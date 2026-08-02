package com.demetrius.tribunal.task.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务执行日志持久化对象（对应 t_task_log 表）。
 */
@Data
@TableName("t_task_log")
public class TaskLogPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 任务编码：TIMEOUT_CLOSE_ORDER / STATUS_RECONCILE / DATA_ARCHIVE */
    private String taskCode;

    /** 执行结果：SUCCESS / FAILED */
    private String result;

    private Integer processedCount;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
