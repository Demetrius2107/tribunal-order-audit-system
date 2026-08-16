package com.demetrius.tribunal.order.infrastructure.event;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.record.TimestampType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 订单事件死信队列自动重投（异步补偿闭环：DLT 自动重新消费）。
 *
 * <p>下游消费失败重试 3 次后，由 {@code DeadLetterPublishingRecoverer} 投递到
 * {@code order-events.DLT} 死信队列；本任务消费死信并重投回原 topic：</p>
 * <ul>
 *   <li>保留 partition key（orderId），保证同一订单事件顺序消费</li>
 *   <li>重投时携带自增头 {@code x-requeue-count}，跨 DLT 循环持久计数</li>
 *   <li>重投次数达到 {@link #MAX_REQUEUE} 后丢弃并告警（避免无限循环）</li>
 * </ul>
 */
@Component
public class OrderEventDltRequeueTask {

    private static final Logger log = LoggerFactory.getLogger(OrderEventDltRequeueTask.class);

    /** 死信队列 topic（DefaultErrorHandler 默认后缀 .DLT） */
    private static final String DLT_TOPIC = "order-events.DLT";

    /** 原 topic（重投目标） */
    private static final String ORIGIN_TOPIC = "order-events";

    /** 重投次数头（跨 DLT 循环持久计数） */
    private static final String REQUEUE_COUNT_HEADER = "x-requeue-count";

    /** 最大重投次数（超过则丢弃告警，防止死信无限循环） */
    private static final int MAX_REQUEUE = 3;

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderEventDltRequeueTask(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 消费死信队列并重投回原 topic。
     */
    @KafkaListener(topics = DLT_TOPIC, groupId = "dlt-requeue")
    public void requeue(ConsumerRecord<String, String> record) {
        int count = readRequeueCount(record);
        if (count >= MAX_REQUEUE) {
            log.error("死信消息已达最大重投次数，丢弃告警 key={} count={}", record.key(), count);
            return;
        }
        // 重投回原 topic，保留 partition（顺序保证）与 key（orderId），并携带自增计数头
        RecordHeader header = new RecordHeader(REQUEUE_COUNT_HEADER,
                String.valueOf(count + 1).getBytes(StandardCharsets.UTF_8));
        org.apache.kafka.clients.producer.ProducerRecord<String, String> producerRecord =
                new org.apache.kafka.clients.producer.ProducerRecord<>(
                        ORIGIN_TOPIC, record.partition(), record.timestamp(),
                        record.key(), record.value(), List.of(header));
        kafkaTemplate.send(producerRecord);
        log.warn("死信消息重投回 {} key={} count={}", ORIGIN_TOPIC, record.key(), count + 1);
    }

    /** 读取重投计数头（缺失视为 0）。 */
    private int readRequeueCount(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(REQUEUE_COUNT_HEADER);
        if (header == null) {
            return 0;
        }
        try {
            return Integer.parseInt(new String(header.value(), StandardCharsets.UTF_8));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
