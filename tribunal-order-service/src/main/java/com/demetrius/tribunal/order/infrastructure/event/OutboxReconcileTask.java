package com.demetrius.tribunal.order.infrastructure.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.order.infrastructure.mapper.OutboxMessageMapper;
import com.demetrius.tribunal.order.infrastructure.mapper.ReconcileRecordMapper;
import com.demetrius.tribunal.order.infrastructure.model.OutboxMessagePo;
import com.demetrius.tribunal.order.infrastructure.model.ReconcileRecordPo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Outbox 消息对账任务（F-802：消息对账）。
 *
 * <p>核对本地消息表，差异落库 {@code t_reconcile_record} 而非纯日志告警：</p>
 * <ul>
 *   <li>FAILED（重试超限终态失败）→ 自动补偿：重置为 PENDING 重新投递（标记 FIXED）</li>
 *   <li>超时未投递的 PENDING → 记录 OPEN，由人工/后续兜底</li>
 * </ul>
 */
@Component
public class OutboxReconcileTask {

    private static final Logger log = LoggerFactory.getLogger(OutboxReconcileTask.class);

    /** PENDING 超过该分钟数视为异常积压 */
    private static final long STALE_MINUTES = 15;

    private final OutboxMessageMapper outboxMessageMapper;

    private final ReconcileRecordMapper reconcileRecordMapper;

    public OutboxReconcileTask(OutboxMessageMapper outboxMessageMapper,
                               ReconcileRecordMapper reconcileRecordMapper) {
        this.outboxMessageMapper = outboxMessageMapper;
        this.reconcileRecordMapper = reconcileRecordMapper;
    }

    /**
     * 每 10 分钟核对一次 outbox 消息状态。
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void reconcile() {
        List<OutboxMessagePo> failed = outboxMessageMapper.selectList(
                new LambdaQueryWrapper<OutboxMessagePo>().eq(OutboxMessagePo::getStatus, "FAILED"));

        LocalDateTime staleThreshold = LocalDateTime.now().minusMinutes(STALE_MINUTES);
        List<OutboxMessagePo> stalePending = outboxMessageMapper.selectList(
                new LambdaQueryWrapper<OutboxMessagePo>()
                        .eq(OutboxMessagePo::getStatus, "PENDING")
                        .lt(OutboxMessagePo::getCreateTime, staleThreshold));

        for (OutboxMessagePo msg : failed) {
            saveRecord("OUTBOX_RECONCILE", "FAILED_MSG", msg.getMessageId(),
                    "重试超限标记 FAILED，自动重置重投", true);
            outboxMessageMapper.resetFailed(msg.getId());
            log.warn("消息对账补偿: FAILED 消息已重置重投 messageId={}", msg.getMessageId());
        }
        for (OutboxMessagePo msg : stalePending) {
            saveRecord("OUTBOX_RECONCILE", "STALE_PENDING", msg.getMessageId(),
                    "PENDING 超过 " + STALE_MINUTES + " 分钟未投递", false);
            log.error("消息对账发现异常: 超时 PENDING messageId={}", msg.getMessageId());
        }

        if (!failed.isEmpty() || !stalePending.isEmpty()) {
            log.error("消息对账发现异常: FAILED={}, 超时PENDING(>{}分钟)={}",
                    failed.size(), STALE_MINUTES, stalePending.size());
        } else {
            log.info("消息对账正常: 无 FAILED / 无超时 PENDING 消息");
        }
    }

    private void saveRecord(String taskCode, String recordType, String refNo,
                            String detail, boolean autoFixed) {
        ReconcileRecordPo record = new ReconcileRecordPo();
        record.setId(UUID.randomUUID().toString().replace("-", ""));
        record.setTaskCode(taskCode);
        record.setRecordType(recordType);
        record.setRefNo(refNo);
        record.setDetail(detail);
        record.setStatus(autoFixed ? "FIXED" : "OPEN");
        record.setAutoFixed(autoFixed ? 1 : 0);
        record.setCreateTime(LocalDateTime.now());
        if (autoFixed) {
            record.setFixTime(LocalDateTime.now());
        }
        reconcileRecordMapper.insert(record);
    }
}
