package com.demetrius.tribunal.order.infrastructure.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 库存变更事件消费者（订阅 inventory-push 系统发布的 inventory-events）。
 *
 * <p>对应 PRD 2.4.1 FR-036 事件订阅模式：订单系统订阅库存变动事件，用于库存实时感知/预占校验。</p>
 *
 * <p>基建说明：消费到事件后的业务处理（库存快照同步、预占校验等）留待与订单审单逻辑对接。</p>
 */
@Component
public class InventoryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);

    private final ObjectMapper objectMapper;

    public InventoryEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "inventory-events", groupId = "order-service")
    public void onInventoryChanged(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            log.info("收到库存变更事件 skuId={} warehouseId={} availableQty={}",
                    node.path("skuId").asText(),
                    node.path("warehouseId").asText(),
                    node.path("inventory").path("availableQty").asInt());
            // TODO：库存感知/预占校验业务逻辑
        } catch (Exception e) {
            log.error("库存变更事件解析失败: {}", message, e);
        }
    }
}
