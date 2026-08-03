package com.demetrius.tribunal.order.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账单状态回传应用服务（order-service 接收侧）。
 *
 * <p>对应需求：F-308（状态回传）、N-304（回传幂等）。</p>
 *
 * <p>职责：接收金融账单模块的账单状态回传，映射为订单状态机的迁移动作。
 * 非法/重复状态由聚合内部的状态机拒绝（幂等核心）。</p>
 *
 * <p>账单状态 → 订单状态机映射：</p>
 * <ul>
 *   <li>CONFIRMED（账单确认）→ Order.startTransfer()（开始转单）</li>
 *   <li>SETTLED / VERIFIED（结算/核销）→ Order.transferSuccess()（转单成功）</li>
 *   <li>CANCELLED（取消）→ Order.cancel()</li>
 * </ul>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>回传失败重试（账单模块重推或定时对账补偿，对应 F-701）</li>
 *   <li>消息队列化：回传走 MQ 而非同步 Feign（里程碑 M3）</li>
 * </ul>
 */
@Service
public class BillStatusCallbackApplicationService {

    private final OrderRepository orderRepository;

    public BillStatusCallbackApplicationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * 处理账单状态回传，驱动本地订单状态机。
     *
     * <p>账单状态 → 订单状态机映射：</p>
     * <ul>
     *   <li>CONFIRMED（账单确认）→ Order.startTransfer()（开始转单）</li>
     *   <li>SETTLED / VERIFIED（结算/核销）→ Order.transferSuccess()（转单成功）</li>
     *   <li>CANCELLED（取消）→ Order.cancel()</li>
     * </ul>
     *
     * @param sourceOrderNo 订单编号
     * @param billStatus    账单状态
     * @param billId        账单号
     */
    @Transactional
    public void handleCallback(String sourceOrderNo, String billStatus, String billId) {
        Order order = orderRepository.findByOrderNo(sourceOrderNo)
                .orElseThrow(() -> new BizException("200004", "订单不存在: " + sourceOrderNo));

        switch (billStatus) {
            case "CONFIRMED" -> order.startTransfer();
            case "SETTLED", "VERIFIED" -> order.transferSuccess();
            case "CANCELLED" -> order.cancel();
            default -> throw new BizException("200005", "未知的账单状态: " + billStatus);
        }
        // TODO（学习任务）：记录账单号 billId 到订单（对账用）

        orderRepository.save(order);
        // TODO（学习任务）：发布状态变更事件（通知订阅者），由应用层统一发布
    }
}
