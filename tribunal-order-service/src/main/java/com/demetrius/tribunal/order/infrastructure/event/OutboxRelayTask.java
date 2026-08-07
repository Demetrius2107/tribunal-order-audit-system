package com.demetrius.tribunal.order.infrastructure.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.order.infrastructure.mapper.OutboxMessageMapper;
import com.demetrius.tribunal.order.infrastructure.model.OutboxMessagePo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 消息中继任务（M3 异步化）。
 *
 * <p>定时轮询 PENDING 消息投递到 Kafka，保证"业务事务提交 → 消息最终送达"的 at-least-once 语义。</p>
 *
 * <ul>
 *   <li>投递成功 → 标记 SENT</li>
 *   <li>投递失败 → 指数退避（2s/4s/8s），超 3 次标记 FAILED（由对账任务/人工兜底）</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "tribunal.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayTask {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayTask.class);

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 3;

    private final OutboxMessageMapper outboxMessageMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelayTask(OutboxMessageMapper outboxMessageMapper,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxMessageMapper = outboxMessageMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 每 5 秒扫描一次 PENDING 消息（next_retry_time 为空或已到期）。
     */
    @Scheduled(fixedDelay = 5000)
    public void relay() {
        LambdaQueryWrapper<OutboxMessagePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutboxMessagePo::getStatus, "PENDING")
               .and(w -> w.isNull(OutboxMessagePo::getNextRetryTime)
                          .or().le(OutboxMessagePo::getNextRetryTime, LocalDateTime.now()))
               .orderByAsc(OutboxMessagePo::getId)
               .last("LIMIT " + BATCH_SIZE);

        List<OutboxMessagePo> pending = outboxMessageMapper.selectList(wrapper);
        if (pending.isEmpty()) {
            return;
        }

        log.debug("outbox relay: 扫描到 {} 条 PENDING 消息", pending.size());

        for (OutboxMessagePo msg : pending) {
            sendOne(msg);
        }
    }

    private void sendOne(OutboxMessagePo msg) {
        try {
            kafkaTemplate.send(msg.getTopic(), msg.getMessageKey(), msg.getPayload()).get(10, java.util.concurrent.TimeUnit.SECONDS);
            outboxMessageMapper.markSent(msg.getId(), msg.getVersion(), LocalDateTime.now());
            log.debug("outbox 已投递 messageId={} topic={}", msg.getMessageId(), msg.getTopic());
        } catch (Exception e) {
            log.warn("outbox 投递失败 messageId={} retryCount={}", msg.getMessageId(), msg.getRetryCount(), e);
            handleFailure(msg);
        }
    }

    private void handleFailure(OutboxMessagePo msg) {
        int newRetryCount = msg.getRetryCount() + 1;
        if (newRetryCount >= MAX_RETRIES) {
            outboxMessageMapper.markFailed(msg.getId(), msg.getVersion());
            log.error("outbox 消息达最大重试次数，标记 FAILED messageId={}", msg.getMessageId());
        } else {
            // 指数退避：2s → 4s → 8s
            long delaySeconds = (long) Math.pow(2, newRetryCount);
            LocalDateTime nextRetry = LocalDateTime.now().plus(Duration.ofSeconds(delaySeconds));
            outboxMessageMapper.incrementRetry(msg.getId(), msg.getVersion(), nextRetry);
        }
    }
}
