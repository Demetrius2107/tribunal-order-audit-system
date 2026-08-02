package com.demetrius.tribunal.erp.infrastructure.listener;

import com.demetrius.tribunal.erp.client.OrderStatusCallbackRequest;
import com.demetrius.tribunal.erp.client.OrderStatusFeignClient;
import com.demetrius.tribunal.erp.domain.event.ErpOrderStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * ERP 履约状态变更事件监听器：回传 OMS 驱动订单状态机。
 *
 * <p>对应需求：F-503（状态回传）、N-304（回传幂等，OMS 状态机兜底）。</p>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>用 Spring 事件（进程内）触发 Feign 回传；升级 MQ 后替换为消息消费者</li>
 *   <li>TODO：回传失败重试（记录待重传表 + 定时补偿，对应 F-701 对账）</li>
 * </ul>
 */
@Component
public class ErpOrderStatusCallbackListener {

    private static final Logger log = LoggerFactory.getLogger(ErpOrderStatusCallbackListener.class);

    private final OrderStatusFeignClient orderStatusFeignClient;

    public ErpOrderStatusCallbackListener(OrderStatusFeignClient orderStatusFeignClient) {
        this.orderStatusFeignClient = orderStatusFeignClient;
    }

    @EventListener
    public void onStatusChanged(ErpOrderStatusChangedEvent event) {
        try {
            orderStatusFeignClient.statusCallback(new OrderStatusCallbackRequest(
                    event.sourceOrderNo(),
                    event.to().name(),
                    event.erpOrderId().value()));
            log.info("已回传OMS: sourceOrderNo={}, erpStatus={}", event.sourceOrderNo(), event.to());
        } catch (Exception e) {
            // TODO（学习任务）：记录回传失败，定时补偿（对应 F-701）
            log.error("回传OMS失败: sourceOrderNo={}, erpStatus={}, error={}",
                    event.sourceOrderNo(), event.to(), e.getMessage());
        }
    }
}
