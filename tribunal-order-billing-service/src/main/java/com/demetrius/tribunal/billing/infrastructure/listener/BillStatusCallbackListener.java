package com.demetrius.tribunal.billing.infrastructure.listener;

import com.demetrius.tribunal.billing.client.OrderStatusCallbackRequest;
import com.demetrius.tribunal.billing.client.OrderStatusFeignClient;
import com.demetrius.tribunal.billing.domain.event.BillStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 金融账单状态变更事件监听器：回传 订单服务 驱动订单状态机。
 *
 * <p>对应需求：F-503（状态回传）、N-304（回传幂等，订单服务 状态机兜底）。</p>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>用 Spring 事件（进程内）触发 Feign 回传；升级 MQ 后替换为消息消费者</li>
 *   <li>TODO：回传失败重试（记录待重传表 + 定时补偿，对应 F-701 对账）</li>
 * </ul>
 */
@Component
public class BillStatusCallbackListener {

    private static final Logger log = LoggerFactory.getLogger(BillStatusCallbackListener.class);

    private final OrderStatusFeignClient orderStatusFeignClient;

    public BillStatusCallbackListener(OrderStatusFeignClient orderStatusFeignClient) {
        this.orderStatusFeignClient = orderStatusFeignClient;
    }

    @EventListener
    public void onStatusChanged(BillStatusChangedEvent event) {
        try {
            orderStatusFeignClient.statusCallback(new OrderStatusCallbackRequest(
                    event.sourceOrderNo(),
                    event.to().name(),
                    event.billId().value()));
            log.info("已回传订单服务: sourceOrderNo={}, billStatus={}", event.sourceOrderNo(), event.to());
        } catch (Exception e) {
            // TODO（学习任务）：记录回传失败，定时补偿（对应 F-701）
            log.error("回传订单服务失败: sourceOrderNo={}, billStatus={}, error={}",
                    event.sourceOrderNo(), event.to(), e.getMessage());
        }
    }
}
