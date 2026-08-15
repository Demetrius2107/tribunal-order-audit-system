package com.demetrius.tribunal.order.infrastructure.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.order.infrastructure.mapper.OutboxMessageMapper;
import com.demetrius.tribunal.order.infrastructure.model.OutboxMessagePo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Outbox 消息对账任务（F-802：消息对账）。
 *
 * <p>核对本地消息表：FAILED（重试超限终态失败）与超时未投递的 PENDING 消息，
 * 差异告警（由人工/后续任务兜底修复）。</p>
 */
@Component
public class OutboxReconcileTask {

    private static final Logger log = LoggerFactory.getLogger(OutboxReconcileTask.class);

    /** PENDING 超过该分钟数视为异常积压 */
    private static final long STALE_MINUTES = 15;

    private final OutboxMessageMapper outboxMessageMapper;

    public OutboxReconcileTask(OutboxMessageMapper outboxMessageMapper) {
        this.outboxMessageMapper = outboxMessageMapper;
    }

    /**
     * 每 10 分钟核对一次 outbox 消息状态。
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void reconcile() {
        Long failed = outboxMessageMapper.selectCount(
                new LambdaQueryWrapper<OutboxMessagePo>().eq(OutboxMessagePo::getStatus, "FAILED"));

        LocalDateTime staleThreshold = LocalDateTime.now().minusMinutes(STALE_MINUTES);
        Long stalePending = outboxMessageMapper.selectCount(
                new LambdaQueryWrapper<OutboxMessagePo>()
                        .eq(OutboxMessagePo::getStatus, "PENDING")
                        .lt(OutboxMessagePo::getCreateTime, staleThreshold));

        if (failed > 0 || stalePending > 0) {
            log.error("消息对账发现异常: FAILED={}, 超时PENDING(>{}分钟)={}", failed, STALE_MINUTES, stalePending);
        } else {
            log.info("消息对账正常: 无 FAILED / 无超时 PENDING 消息");
        }
    }
}
