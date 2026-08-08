package com.demetrius.tribunal.order.infrastructure.listener;

import com.demetrius.tribunal.order.domain.event.OrderStatusChangedEvent;
import com.demetrius.tribunal.order.infrastructure.mapper.OrderStatusRecordMapper;
import com.demetrius.tribunal.order.infrastructure.model.OrderStatusRecordPo;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 订单状态变更事件监听器：每次状态迁移写一条流水（t_order_status_record）。
 *
 * <p>参照通用做法：状态流水落库。</p>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>用 Spring 事件（进程内）实现，异步状态变更（如 billing 回传）由 Kafka 消费者触发同一事件链</li>
 *   <li>REQUIRES_NEW：即使主事务回滚，流水也已落库（审计不随业务回滚）</li>
 *   <li>operator 来自事件内容：同步操作（审单/发货）透传操作员，异步操作（账单回传）默认 "system"</li>
 * </ul>
 */
@Component
public class OrderStatusRecordListener {

    private final OrderStatusRecordMapper recordMapper;

    public OrderStatusRecordListener(OrderStatusRecordMapper recordMapper) {
        this.recordMapper = recordMapper;
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStatusChanged(OrderStatusChangedEvent event) {
        OrderStatusRecordPo po = new OrderStatusRecordPo();
        po.setOrderId(event.orderId().value());
        po.setFromStatus(event.from() == null ? null : event.from().name());
        po.setToStatus(event.to().name());
        po.setOperator(event.operator() == null ? "system" : event.operator());
        po.setCreateTime(event.occurredAt() == null ? LocalDateTime.now() : event.occurredAt());
        recordMapper.insert(po);
    }
}
