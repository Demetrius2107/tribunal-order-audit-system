package com.demetrius.tribunal.fulfillment.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.fulfillment.application.dto.FulfillmentReceiveCommand;
import com.demetrius.tribunal.fulfillment.application.dto.FulfillmentResult;
import com.demetrius.tribunal.fulfillment.domain.model.FulfillmentId;
import com.demetrius.tribunal.fulfillment.domain.model.FulfillmentLine;
import com.demetrius.tribunal.fulfillment.domain.model.FulfillmentOrder;
import com.demetrius.tribunal.fulfillment.domain.repository.FulfillmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 履约执行应用服务（用例编排层）。
 *
 * <p>对应需求：下游履约执行（出库/发货/签收）、发送工厂生产指令。</p>
 *
 * <p>职责：</p>
 * <ol>
 *   <li>账单结算后创建履约单（接收订单数据）</li>
 *   <li>发货/签收/取消（履约状态机）</li>
 *   <li>发送工厂生产/备货指令（TODO）</li>
 * </ol>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>创建履约单幂等：按 sourceOrderNo 查重</li>
 *   <li>发货时调用 inventory-service 出库扣减</li>
 *   <li>状态变更事件 → 回传订单服务（签收驱动订单终态）</li>
 * </ul>
 */
@Service
public class FulfillmentApplicationService {

    private final FulfillmentRepository fulfillmentRepository;

    public FulfillmentApplicationService(FulfillmentRepository fulfillmentRepository) {
        this.fulfillmentRepository = fulfillmentRepository;
    }

    /**
     * 创建履约单（账单结算后触发）。
     */
    @Transactional
    public FulfillmentResult create(FulfillmentReceiveCommand command) {
        // TODO（学习任务）：按 sourceOrderNo 幂等查重

        List<FulfillmentLine> lines = command.lines().stream()
                .map(l -> new FulfillmentLine(l.skuCode(), l.skuName(), l.quantity(), l.price()))
                .toList();
        FulfillmentOrder order = FulfillmentOrder.create(
                new FulfillmentId(generateId()),
                command.sourceOrderNo(),
                command.customerId(),
                lines);

        fulfillmentRepository.save(order);
        // TODO（学习任务）：发送工厂生产指令（dispatchToFactory）
        return FulfillmentResult.from(order);
    }

    /**
     * 发货（出库完成）。
     */
    @Transactional
    public FulfillmentResult ship(String fulfillmentId) {
        FulfillmentOrder order = findRequired(fulfillmentId);
        order.ship();
        fulfillmentRepository.save(order);
        return FulfillmentResult.from(order);
    }

    /**
     * 签收（终端签收，终态）。
     */
    @Transactional
    public FulfillmentResult sign(String fulfillmentId) {
        FulfillmentOrder order = findRequired(fulfillmentId);
        order.sign();
        fulfillmentRepository.save(order);
        // TODO（学习任务）：发布事件回传订单服务（签收终态）
        return FulfillmentResult.from(order);
    }

    /**
     * 取消履约。
     */
    @Transactional
    public FulfillmentResult cancel(String fulfillmentId) {
        FulfillmentOrder order = findRequired(fulfillmentId);
        order.cancel();
        fulfillmentRepository.save(order);
        return FulfillmentResult.from(order);
    }

    /**
     * 查询履约单。
     */
    @Transactional(readOnly = true)
    public FulfillmentResult get(String fulfillmentId) {
        return FulfillmentResult.from(findRequired(fulfillmentId));
    }

    private FulfillmentOrder findRequired(String fulfillmentId) {
        return fulfillmentRepository.findById(new FulfillmentId(fulfillmentId))
                .orElseThrow(() -> new BizException("600001", "履约单不存在: " + fulfillmentId));
    }

    private String generateId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
