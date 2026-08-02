package com.demetrius.tribunal.order.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ERP 状态回传应用服务（order-service 接收侧）。
 *
 * <p>对应需求：F-308（状态回传）、N-304（回传幂等）。</p>
 *
 * <p>职责：接收 ERP 履约状态回传，映射为订单状态机的迁移动作。
 * 非法/重复状态由聚合内部的状态机拒绝（幂等核心）。</p>
 *
 * <p>ERP 状态 → 订单状态机映射：</p>
 * <ul>
 *   <li>SHIPPED（已发货）→ Order.ship()</li>
 *   <li>SIGNED（已签收）→ Order.sign()</li>
 *   <li>CLOSED（已关闭）→ Order.cancel()</li>
 *   <li>CANCELLED（已取消）→ Order.cancel()</li>
 * </ul>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>回传失败重试（ERP 重推或定时对账补偿，对应 F-701）</li>
 *   <li>消息队列化：回传走 MQ 而非同步 Feign（里程碑 M3）</li>
 * </ul>
 */
@Service
public class ErpStatusCallbackApplicationService {

    private final OrderRepository orderRepository;

    public ErpStatusCallbackApplicationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * 处理 ERP 状态回传，驱动本地订单状态机。
     *
     * @param sourceOrderNo 订单编号
     * @param erpStatus     ERP 履约状态
     * @param erpOrderId    ERP 履约单号
     */
    @Transactional
    public void handleCallback(String sourceOrderNo, String erpStatus, String erpOrderId) {
        Order order = orderRepository.findByOrderNo(sourceOrderNo)
                .orElseThrow(() -> new BizException("200004", "订单不存在: " + sourceOrderNo));

        switch (erpStatus) {
            case "SHIPPED" -> order.ship();
            case "SIGNED" -> order.sign();
            case "CLOSED", "CANCELLED" -> order.cancel();
            default -> throw new BizException("200005", "未知的ERP履约状态: " + erpStatus);
        }
        // TODO（学习任务）：记录 ERP 履约单号 erpOrderId 到订单（对账用）

        orderRepository.save(order);
        // TODO（学习任务）：发布状态变更事件（通知订阅者），由应用层统一发布
    }
}
