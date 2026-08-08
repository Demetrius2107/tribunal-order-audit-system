package com.demetrius.tribunal.billing.infrastructure.listener;

import com.demetrius.tribunal.billing.domain.event.BillStatusChangedEvent;
import com.demetrius.tribunal.billing.infrastructure.event.BillingEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 金融账单状态变更事件监听器（M3 异步化：发布到 Kafka billing-events 主题）。
 *
 * <p>billing-service 内部状态变更 → Spring 进程内事件 → 本监听 → Kafka 发布。
 * order-service 的 BillingEventConsumer 接收后驱动订单状态机。</p>
 *
 * <p>对应需求：F-503（状态回传）、N-304（回传幂等）。</p>
 */
@Component
public class BillStatusCallbackListener {

    private static final Logger log = LoggerFactory.getLogger(BillStatusCallbackListener.class);

    private final BillingEventPublisher billingEventPublisher;

    public BillStatusCallbackListener(BillingEventPublisher billingEventPublisher) {
        this.billingEventPublisher = billingEventPublisher;
    }

    @EventListener
    public void onStatusChanged(BillStatusChangedEvent event) {
        try {
            billingEventPublisher.publishBillStatusChanged(event);
            log.info("已发布账单状态事件到 Kafka: sourceOrderNo={}, billStatus={}",
                    event.sourceOrderNo(), event.to());
        } catch (Exception e) {
            log.error("发布账单状态事件失败: sourceOrderNo={}, billStatus={}, error={}",
                    event.sourceOrderNo(), event.to(), e.getMessage());
        }
    }
}
