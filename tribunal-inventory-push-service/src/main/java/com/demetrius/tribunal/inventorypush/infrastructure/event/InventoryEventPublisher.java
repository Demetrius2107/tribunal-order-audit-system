package com.demetrius.tribunal.inventorypush.infrastructure.event;

import com.demetrius.tribunal.inventorypush.application.dto.InventoryChangedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 库存事件发布器（向下游分发，topic: inventory-events）。
 *
 * <p>对应 PRD 2.4.1 FR-034 主动推送（Push）/ FR-036 事件订阅模式：
 * 库存变动后实时发布事件，下游订单系统订阅消费。</p>
 */
@Component
public class InventoryEventPublisher {

    /** 库存事件主题（下游订单系统订阅） */
    public static final String INVENTORY_EVENTS_TOPIC = "inventory-events";

    private static final Logger log = LoggerFactory.getLogger(InventoryEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public InventoryEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 发布库存变更事件（messageId 为下游去重键，PRD 2.5.2 FR-048）。
     */
    public void publish(InventoryChangedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(INVENTORY_EVENTS_TOPIC, event.messageId(), payload);
            log.info("已发布库存事件 messageId={} skuId={} warehouseId={}",
                    event.messageId(), event.skuId(), event.warehouseId());
        } catch (JsonProcessingException e) {
            log.error("库存事件序列化失败 messageId={}", event.messageId(), e);
        }
    }
}
