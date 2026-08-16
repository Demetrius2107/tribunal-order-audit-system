package com.demetrius.tribunal.billing.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.billing.application.dto.BillReceiveCommand;
import com.demetrius.tribunal.billing.application.dto.BillResult;
import com.demetrius.tribunal.billing.domain.event.BillStatusChangedEvent;
import com.demetrius.tribunal.billing.domain.model.BillId;
import com.demetrius.tribunal.billing.domain.model.BillLine;
import com.demetrius.tribunal.billing.domain.model.FinanceBill;
import com.demetrius.tribunal.billing.domain.repository.BillRepository;
import com.demetrius.tribunal.billing.infrastructure.mapper.BillPaymentMapper;
import com.demetrius.tribunal.billing.infrastructure.model.BillPaymentPo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 金融账单应用服务（用例编排层）。
 *
 * <p>对应需求：F-307（生成账单）、F-404（收款确认）、N-304（状态回传幂等）。</p>
 *
 * <p>职责：</p>
 * <ol>
 *   <li>接收订单服务转单 → 生成账单聚合 → 保存</li>
 *   <li>账单动作（确认/结算/核销/取消）→ 状态机迁移 → 保存</li>
 *   <li>状态变更发布事件 → 订阅者 Feign 回传订单服务</li>
 * </ol>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>生成账单幂等：按 sourceOrderNo 查重（重复转单拒绝或幂等返回）</li>
 *   <li>金额核对：账单金额与订单金额一致性校验（对账，F-701）</li>
 *   <li>回传失败重试：回传订单服务失败记录待重试（对照对账任务 F-701）</li>
 * </ul>
 */
@Service
public class BillingApplicationService {

    private static final Logger log = LoggerFactory.getLogger(BillingApplicationService.class);

    private final BillRepository billRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final BillPaymentMapper billPaymentMapper;

    public BillingApplicationService(BillRepository billRepository,
                                     ApplicationEventPublisher eventPublisher,
                                     BillPaymentMapper billPaymentMapper) {
        this.billRepository = billRepository;
        this.eventPublisher = eventPublisher;
        this.billPaymentMapper = billPaymentMapper;
    }

    /**
     * 接收订单服务转单，生成账单（初始状态 = 已生成）。
     *
     * <p>幂等：按 sourceOrderNo 查重，重复转单（含对账自动补账单重发）直接返回已有账单，
     * 不重复生成（N-205 / 异步补偿闭环前提）。</p>
     */
    @Transactional
    public BillResult generateBill(BillReceiveCommand command) {
        // 幂等查重：同一上游订单号已生成账单则直接返回（对账自动修复重发场景）
        FinanceBill exist = billRepository.findBySourceOrderNo(command.sourceOrderNo()).orElse(null);
        if (exist != null) {
            log.info("转单幂等命中: sourceOrderNo={} 返回已有账单 billId={}",
                    command.sourceOrderNo(), exist.getId().value());
            return BillResult.from(exist);
        }

        List<BillLine> lines = command.lines().stream()
                .map(l -> new BillLine(l.skuCode(), l.skuName(), l.quantity(), l.price()))
                .toList();
        FinanceBill bill = FinanceBill.generate(
                new BillId(generateId()),
                command.sourceOrderNo(),
                command.customerId(),
                lines);

        billRepository.save(bill);
        // TODO（学习任务）：发布 BillGeneratedEvent（通知/对账订阅）
        return BillResult.from(bill);
    }

    /**
     * 确认（用例：账单审核通过，待收款）。
     */
    @Transactional
    public BillResult confirm(String billId) {
        FinanceBill bill = findRequired(billId);
        BillStatusChangedEvent event = snapshot(bill);
        bill.confirm();
        billRepository.save(bill);
        eventPublisher.publishEvent(new BillStatusChangedEvent(
                event.billId(), event.sourceOrderNo(), event.from(), bill.getStatus(), bill.getUpdateTime()));
        return BillResult.from(bill);
    }

    /**
     * 结算（用例：款项到位，终态）。
     */
    @Transactional
    public BillResult settle(String billId) {
        FinanceBill bill = findRequired(billId);
        BillStatusChangedEvent event = snapshot(bill);
        bill.settle();
        billRepository.save(bill);
        recordPayment(bill);
        eventPublisher.publishEvent(new BillStatusChangedEvent(
                event.billId(), event.sourceOrderNo(), event.from(), bill.getStatus(), bill.getUpdateTime()));
        return BillResult.from(bill);
    }

    /**
     * 核销（用例：账务核销完成，终态）。
     */
    @Transactional
    public BillResult verify(String billId) {
        FinanceBill bill = findRequired(billId);
        BillStatusChangedEvent event = snapshot(bill);
        bill.verify();
        billRepository.save(bill);
        eventPublisher.publishEvent(new BillStatusChangedEvent(
                event.billId(), event.sourceOrderNo(), event.from(), bill.getStatus(), bill.getUpdateTime()));
        return BillResult.from(bill);
    }

    /**
     * 取消账单。
     */
    @Transactional
    public BillResult cancel(String billId) {
        FinanceBill bill = findRequired(billId);
        BillStatusChangedEvent event = snapshot(bill);
        bill.cancel();
        billRepository.save(bill);
        eventPublisher.publishEvent(new BillStatusChangedEvent(
                event.billId(), event.sourceOrderNo(), event.from(), bill.getStatus(), bill.getUpdateTime()));
        return BillResult.from(bill);
    }

    /**
     * 查询账单。
     */
    @Transactional(readOnly = true)
    public BillResult getBill(String billId) {
        return BillResult.from(findRequired(billId));
    }

    /**
     * 按上游订单编号查询账单（对账任务用：F-801 状态对账）。
     */
    @Transactional(readOnly = true)
    public BillResult getBillBySourceOrderNo(String sourceOrderNo) {
        FinanceBill bill = billRepository.findBySourceOrderNo(sourceOrderNo)
                .orElseThrow(() -> new BizException("300002", "账单不存在: " + sourceOrderNo));
        return BillResult.from(bill);
    }

    private FinanceBill findRequired(String billId) {
        return billRepository.findById(new BillId(billId))
                .orElseThrow(() -> new BizException("300001", "账单不存在: " + billId));
    }

    /** 事件发布前快照（from 状态）。 */
    private BillStatusChangedEvent snapshot(FinanceBill bill) {
        return new BillStatusChangedEvent(
                bill.getId(), bill.getSourceOrderNo(), bill.getStatus(), null, null);
    }

    /**
     * TODO（学习任务）：生成账单 ID。
     */
    private String generateId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 记录收款流水（F-606：审计 + 对账）。
     */
    private void recordPayment(FinanceBill bill) {
        BillPaymentPo payment = new BillPaymentPo();
        payment.setBillId(bill.getId().value());
        payment.setSourceOrderNo(bill.getSourceOrderNo());
        payment.setAmount(bill.getTotalAmount());
        payment.setPaymentTime(LocalDateTime.now());
        billPaymentMapper.insert(payment);
    }
}
