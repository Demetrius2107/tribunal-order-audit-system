package com.demetrius.tribunal.common.event;

/**
 * 跨系统集成事件契约（Kafka 消息体根接口）。
 *
 * <p>R1 阶段定义契约骨架；M3 异步化时由发布方填充字段、消费方据此反序列化。
 * 所有具体事件以 {@code record} 实现，天然不可变、可作 Kafka value 序列化。</p>
 *
 * <p>约定：</p>
 * <ul>
 *   <li>{@code eventId}：事件唯一标识，消费端据此幂等去重</li>
 *   <li>{@code occurredAt}：事件发生时间（epoch millis，发布方填写）</li>
 * </ul>
 */
public interface IntegrationEvent {

    /** 事件唯一 ID（消费端幂等键） */
    String eventId();

    /** 事件发生时间（epoch millis） */
    long occurredAt();
}
