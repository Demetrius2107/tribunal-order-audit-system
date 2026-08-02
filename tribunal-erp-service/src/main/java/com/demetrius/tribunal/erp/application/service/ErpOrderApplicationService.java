package com.demetrius.tribunal.erp.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.erp.application.dto.ErpOrderReceiveCommand;
import com.demetrius.tribunal.erp.application.dto.ErpOrderResult;
import com.demetrius.tribunal.erp.domain.event.ErpOrderStatusChangedEvent;
import com.demetrius.tribunal.erp.domain.model.ErpOrder;
import com.demetrius.tribunal.erp.domain.model.ErpOrderId;
import com.demetrius.tribunal.erp.domain.model.ErpOrderLine;
import com.demetrius.tribunal.erp.domain.repository.ErpOrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ERP 履约应用服务（用例编排层）。
 *
 * <p>对应需求：F-307（接收转单）、F-503（发货/签收回传）、N-304（状态回传幂等）。</p>
 *
 * <p>职责：</p>
 * <ol>
 *   <li>接收 OMS 转单 → 创建履约聚合 → 保存</li>
 *   <li>履约动作（发货/签收/关闭/取消）→ 状态机迁移 → 保存</li>
 *   <li>状态变更发布事件 → 订阅者 Feign 回传 OMS（任务 #5）</li>
 * </ol>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>接收转单幂等：按 sourceOrderNo 查重（重复转单拒绝或幂等返回）</li>
 *   <li>库存锁定：接收时校验并锁定库存（F-502）</li>
 *   <li>状态回传失败重试：回传 OMS 失败记录待重试（对照对账任务 F-701）</li>
 * </ul>
 */
@Service
public class ErpOrderApplicationService {

    private final ErpOrderRepository erpOrderRepository;

    private final ApplicationEventPublisher eventPublisher;

    public ErpOrderApplicationService(ErpOrderRepository erpOrderRepository,
                                      ApplicationEventPublisher eventPublisher) {
        this.erpOrderRepository = erpOrderRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 接收 OMS 转单，创建履约单（初始状态 = 已接收）。
     */
    @Transactional
    public ErpOrderResult receiveOrder(ErpOrderReceiveCommand command) {
        // TODO（学习任务）：按 sourceOrderNo 幂等查重，重复转单直接返回已有履约单

        List<ErpOrderLine> lines = command.lines().stream()
                .map(l -> new ErpOrderLine(l.skuCode(), l.skuName(), l.quantity(), l.price()))
                .toList();
        ErpOrder order = ErpOrder.receive(
                new ErpOrderId(generateId()),
                command.sourceOrderNo(),
                command.customerId(),
                lines);

        erpOrderRepository.save(order);
        // TODO（学习任务）：发布 ErpOrderCreatedEvent（通知/对账订阅）
        return ErpOrderResult.from(order);
    }

    /**
     * 发货（用例：ERP 出库）。
     */
    @Transactional
    public ErpOrderResult ship(String erpOrderId) {
        ErpOrder order = findRequired(erpOrderId);
        ErpOrderStatusChangedEvent event = snapshot(order);
        order.ship();
        erpOrderRepository.save(order);
        eventPublisher.publishEvent(new ErpOrderStatusChangedEvent(
                event.erpOrderId(), event.sourceOrderNo(), event.from(), order.getStatus(), order.getUpdateTime()));
        return ErpOrderResult.from(order);
    }

    /**
     * 签收（用例：终端签收，终态）。
     */
    @Transactional
    public ErpOrderResult sign(String erpOrderId) {
        ErpOrder order = findRequired(erpOrderId);
        ErpOrderStatusChangedEvent event = snapshot(order);
        order.sign();
        erpOrderRepository.save(order);
        eventPublisher.publishEvent(new ErpOrderStatusChangedEvent(
                event.erpOrderId(), event.sourceOrderNo(), event.from(), order.getStatus(), order.getUpdateTime()));
        return ErpOrderResult.from(order);
    }

    /**
     * 关闭履约单。
     */
    @Transactional
    public ErpOrderResult close(String erpOrderId) {
        ErpOrder order = findRequired(erpOrderId);
        ErpOrderStatusChangedEvent event = snapshot(order);
        order.close();
        erpOrderRepository.save(order);
        eventPublisher.publishEvent(new ErpOrderStatusChangedEvent(
                event.erpOrderId(), event.sourceOrderNo(), event.from(), order.getStatus(), order.getUpdateTime()));
        return ErpOrderResult.from(order);
    }

    /**
     * 取消履约单。
     */
    @Transactional
    public ErpOrderResult cancel(String erpOrderId) {
        ErpOrder order = findRequired(erpOrderId);
        ErpOrderStatusChangedEvent event = snapshot(order);
        order.cancel();
        erpOrderRepository.save(order);
        eventPublisher.publishEvent(new ErpOrderStatusChangedEvent(
                event.erpOrderId(), event.sourceOrderNo(), event.from(), order.getStatus(), order.getUpdateTime()));
        return ErpOrderResult.from(order);
    }

    /**
     * 查询履约单。
     */
    @Transactional(readOnly = true)
    public ErpOrderResult getOrder(String erpOrderId) {
        return ErpOrderResult.from(findRequired(erpOrderId));
    }

    private ErpOrder findRequired(String erpOrderId) {
        return erpOrderRepository.findById(new ErpOrderId(erpOrderId))
                .orElseThrow(() -> new BizException("300001", "履约单不存在: " + erpOrderId));
    }

    /** 事件发布前快照（from 状态）。 */
    private ErpOrderStatusChangedEvent snapshot(ErpOrder order) {
        return new ErpOrderStatusChangedEvent(
                order.getId(), order.getSourceOrderNo(), order.getStatus(), null, null);
    }

    /**
     * TODO（学习任务）：生成履约单 ID（雪花/雪花替代方案）。
     */
    private String generateId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
