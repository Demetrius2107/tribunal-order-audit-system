package com.demetrius.tribunal.task.domain.model;

import java.time.LocalDateTime;

/**
 * 定时任务执行日志聚合根。
 *
 * <p>对应需求：F-801（状态对账）、F-802（消息对账）、F-803（归档）、task-service 职责。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>超时关单：扫「待确认/确认中」超时订单自动关闭（对照订单状态机）</li>
 *   <li>状态对账：订单状态 vs 账单状态一致性核对，差异告警（F-801）</li>
 *   <li>数据归档：历史订单/流水归档冷存储（F-803）</li>
 *   <li>任务调度框架：可升级为分布式调度（如 XXL-JOB）</li>
 * </ul>
 */
public class TaskLog {

    private final String id;

    /** 任务编码：TIMEOUT_CLOSE_ORDER / STATUS_RECONCILE / DATA_ARCHIVE */
    private final String taskCode;

    /** 执行结果：SUCCESS / FAILED */
    private String result;

    /** 处理条数 */
    private int processedCount;

    private final LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    public TaskLog(String id, String taskCode) {
        this.id = id;
        this.taskCode = taskCode;
        this.result = "SUCCESS";
        this.startedAt = LocalDateTime.now();
    }

    public void finish(int processedCount) {
        this.processedCount = processedCount;
        this.finishedAt = LocalDateTime.now();
    }

    public void fail() {
        this.result = "FAILED";
        this.finishedAt = LocalDateTime.now();
    }

    // ---------- getters ----------

    public String getId() {
        return id;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public String getResult() {
        return result;
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }
}
