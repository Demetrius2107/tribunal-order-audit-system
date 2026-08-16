package com.demetrius.tribunal.order.infrastructure.event;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * 异步补偿闭环单测：DLT 死信队列自动重投（重投回原 topic / 计数头 / 超限丢弃）。
 */
class OrderEventDltRequeueTaskTest {

    /** 捕获 kafkaTemplate.send 的 ProducerRecord 参数。 */
    private KafkaTemplate<String, String> capturingTemplate(AtomicReference<ProducerRecord<String, String>> captured) {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        doAnswer(inv -> {
            captured.set(inv.getArgument(0));
            return null;
        }).when(template).send(any(ProducerRecord.class));
        return template;
    }

    private ConsumerRecord<String, String> dltRecord(int requeueCount) {
        Headers headers = new RecordHeaders();
        if (requeueCount > 0) {
            headers.add(new RecordHeader(
                    "x-requeue-count", String.valueOf(requeueCount).getBytes(StandardCharsets.UTF_8)));
        }
        return new ConsumerRecord<>("order-events.DLT", 0, 0L, 0L,
                TimestampType.CREATE_TIME, -1, -1, "ORD001", "{\"eventType\":\"OrderApproved\"}",
                headers, java.util.Optional.empty());
    }

    @Test
    @DisplayName("DLT 消息重投回原 topic：保留 key、携带自增计数头")
    void requeueBackToOriginTopic() {
        AtomicReference<ProducerRecord<String, String>> captured = new AtomicReference<>();
        KafkaTemplate<String, String> template = capturingTemplate(captured);
        OrderEventDltRequeueTask task = new OrderEventDltRequeueTask(template);

        task.requeue(dltRecord(0)); // 首次死信（无计数头）

        ProducerRecord<String, String> record = captured.get();
        assertNotNull(record, "应重投到 Kafka");
        assertEquals("order-events", record.topic(), "应重投回原 topic");
        assertEquals("ORD001", record.key(), "应保留 partition key（orderId）");
        Header header = record.headers().lastHeader("x-requeue-count");
        assertNotNull(header, "应携带重投计数头");
        assertEquals("1", new String(header.value(), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("重投次数递增：第二次死信重投计数为 2")
    void requeueCountIncrements() {
        AtomicReference<ProducerRecord<String, String>> captured = new AtomicReference<>();
        KafkaTemplate<String, String> template = capturingTemplate(captured);
        OrderEventDltRequeueTask task = new OrderEventDltRequeueTask(template);

        task.requeue(dltRecord(1)); // 已重投 1 次

        Header header = captured.get().headers().lastHeader("x-requeue-count");
        assertEquals("2", new String(header.value(), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("达到最大重投次数：丢弃不再重投（防死信无限循环）")
    void discardWhenRequeueExhausted() {
        AtomicReference<ProducerRecord<String, String>> captured = new AtomicReference<>();
        KafkaTemplate<String, String> template = capturingTemplate(captured);
        OrderEventDltRequeueTask task = new OrderEventDltRequeueTask(template);

        task.requeue(dltRecord(3)); // 已达上限

        assertNull(captured.get(), "超限应丢弃，不再重投");
    }
}
