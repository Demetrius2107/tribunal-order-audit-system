package com.demetrius.tribunal.inventorypush.infrastructure.listener;

import com.demetrius.tribunal.inventorypush.application.service.InventoryReceiveApplicationService;
import com.demetrius.tribunal.inventorypush.common.dto.InventoryReceiveRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 上游推送 MQ 接收消费者（PRD 2.1.1 FR-002：支持消息队列消费模式接收）。
 *
 * <p>上游库存系统走 Kafka 推送时，与 HTTP 接收接口走同一套幂等/清洗/落库链路。</p>
 */
@Component
public class UpstreamInventoryConsumer {

    private static final Logger log = LoggerFactory.getLogger(UpstreamInventoryConsumer.class);

    private final ObjectMapper objectMapper;
    private final InventoryReceiveApplicationService receiveApplicationService;

    public UpstreamInventoryConsumer(ObjectMapper objectMapper,
                                     InventoryReceiveApplicationService receiveApplicationService) {
        this.objectMapper = objectMapper;
        this.receiveApplicationService = receiveApplicationService;
    }

    @KafkaListener(topics = "inventory-push-input", groupId = "inventory-push")
    public void onUpstreamPush(String message) {
        try {
            InventoryReceiveRequest request = objectMapper.readValue(message, InventoryReceiveRequest.class);
            receiveApplicationService.receive(request);
            log.info("已通过 MQ 接收上游推送 batchId={}", request.getBatchId());
        } catch (Exception e) {
            log.error("MQ 接收上游推送失败: {}", message, e);
        }
    }
}
