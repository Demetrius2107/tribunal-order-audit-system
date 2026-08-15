package com.demetrius.tribunal.task.application.service;

import com.demetrius.tribunal.common.dto.TimeoutCloseResult;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.task.client.OrderTimeoutFeignClient;
import com.demetrius.tribunal.task.domain.model.TaskLog;
import com.demetrius.tribunal.task.domain.repository.TaskLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 定时任务应用服务。
 *
 * <p>对应需求：F-801（状态对账）、F-802（消息对账）、F-803（数据归档）、超时关单。</p>
 *
 * <p>职责（骨架：任务调度骨架 + 日志记录，业务逻辑留 TODO）：</p>
 * <ol>
 *   <li>超时关单：扫超时未确认订单 → 自动关闭</li>
 *   <li>状态对账：订单 vs 账单状态核对，差异告警</li>
 *   <li>数据归档：历史数据归档冷存储</li>
 * </ol>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>任务实现：调 order-service / billing-service 查询与修复（Feign）</li>
 *   <li>分布式调度：多实例防重复执行（分布式锁/XXL-JOB）</li>
 *   <li>失败重试与告警</li>
 * </ul>
 */
@Service
public class TaskApplicationService {

    private static final Logger log = LoggerFactory.getLogger(TaskApplicationService.class);

    /** 超时分钟数（默认 30 分钟未确认自动关闭） */
    private static final int DEFAULT_TIMEOUT_MINUTES = 30;

    private final TaskLogRepository taskLogRepository;

    private final OrderTimeoutFeignClient orderTimeoutFeignClient;

    public TaskApplicationService(TaskLogRepository taskLogRepository,
                                  OrderTimeoutFeignClient orderTimeoutFeignClient) {
        this.taskLogRepository = taskLogRepository;
        this.orderTimeoutFeignClient = orderTimeoutFeignClient;
    }

    /**
     * 超时关单任务（每 5 分钟执行）。
     *
     * <p>调用 order-service 的超时关单接口（数据在订单侧，幂等由订单状态机保证），
     * 关闭数量写入 TaskLog.processedCount。</p>
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void timeoutCloseOrder() {
        runTask("TIMEOUT_CLOSE_ORDER", () -> {
            ApiResponse<TimeoutCloseResult> resp =
                    orderTimeoutFeignClient.timeoutClose(DEFAULT_TIMEOUT_MINUTES);
            int closed = resp != null && resp.getData() != null ? resp.getData().closedCount() : 0;
            log.info("超时关单任务完成: 关闭 {} 单（minutes={}）", closed, DEFAULT_TIMEOUT_MINUTES);
            return closed;
        });
    }

    /**
     * 状态对账任务（每小时执行）。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void statusReconcile() {
        runTask("STATUS_RECONCILE", () -> {
            // TODO（学习任务）：订单 vs 账单状态一致性核对，差异告警（F-801）
            log.info("状态对账任务执行（骨架：无实际处理）");
            return 0;
        });
    }

    /**
     * 数据归档任务（每日凌晨执行）。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dataArchive() {
        runTask("DATA_ARCHIVE", () -> {
            // TODO（学习任务）：历史订单/流水归档冷存储（F-803）
            log.info("数据归档任务执行（骨架：无实际处理）");
            return 0;
        });
    }

    /**
     * 手动触发任务（Controller 调用）。
     */
    public int runNow(String taskCode) {
        return switch (taskCode) {
            case "TIMEOUT_CLOSE_ORDER" -> { runTask("TIMEOUT_CLOSE_ORDER", () -> 0); yield 0; }
            case "STATUS_RECONCILE" -> { runTask("STATUS_RECONCILE", () -> 0); yield 0; }
            case "DATA_ARCHIVE" -> { runTask("DATA_ARCHIVE", () -> 0); yield 0; }
            default -> throw new IllegalArgumentException("未知任务: " + taskCode);
        };
    }

    private void runTask(String taskCode, TaskAction action) {
        TaskLog taskLog = new TaskLog(UUID.randomUUID().toString().replace("-", ""), taskCode);
        try {
            int count = action.execute();
            taskLog.finish(count);
        } catch (Exception e) {
            taskLog.fail();
            log.error("任务执行失败: taskCode={}, error={}", taskCode, e.getMessage(), e);
        }
        taskLogRepository.save(taskLog);
    }

    @FunctionalInterface
    private interface TaskAction {
        int execute();
    }
}
