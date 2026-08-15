package com.demetrius.tribunal.billing.infrastructure.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.billing.infrastructure.mapper.BillMapper;
import com.demetrius.tribunal.billing.infrastructure.mapper.BillPaymentMapper;
import com.demetrius.tribunal.billing.infrastructure.mapper.ReconcileRecordMapper;
import com.demetrius.tribunal.billing.infrastructure.model.BillPaymentPo;
import com.demetrius.tribunal.billing.infrastructure.model.BillPo;
import com.demetrius.tribunal.billing.infrastructure.model.ReconcileRecordPo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 财务对账任务（F-607：账单金额 vs 收款流水金额核对，差异落库 + 自动补偿）。
 *
 * <p>已结算（SETTLED）账单应有金额一致的收款流水（t_bill_payment）：</p>
 * <ul>
 *   <li>完全缺失收款流水 → 自动补偿：按账单金额补记一条收款流水（标记 FIXED）</li>
 *   <li>金额不一致 → 记录 OPEN（金额差异无法自动判定，交由人工核对）</li>
 * </ul>
 */
@Component
public class PaymentReconcileTask {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconcileTask.class);

    private final BillMapper billMapper;

    private final BillPaymentMapper billPaymentMapper;

    private final ReconcileRecordMapper reconcileRecordMapper;

    public PaymentReconcileTask(BillMapper billMapper,
                                BillPaymentMapper billPaymentMapper,
                                ReconcileRecordMapper reconcileRecordMapper) {
        this.billMapper = billMapper;
        this.billPaymentMapper = billPaymentMapper;
        this.reconcileRecordMapper = reconcileRecordMapper;
    }

    /**
     * 每小时核对一次：已结算账单的收款流水金额是否与账单金额一致。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void reconcile() {
        List<BillPo> settledBills = billMapper.selectList(
                new LambdaQueryWrapper<BillPo>().eq(BillPo::getStatus, "SETTLED"));

        int mismatch = 0;
        for (BillPo bill : settledBills) {
            List<BillPaymentPo> payments = billPaymentMapper.selectList(
                    new LambdaQueryWrapper<BillPaymentPo>().eq(BillPaymentPo::getBillId, bill.getId()));
            BigDecimal paidTotal = payments.stream()
                    .map(BillPaymentPo::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (payments.isEmpty()) {
                // 自动补偿：结算时未记录收款流水的，按账单金额补记
                mismatch++;
                BillPaymentPo payment = new BillPaymentPo();
                payment.setId(UUID.randomUUID().toString().replace("-", ""));
                payment.setBillId(bill.getId());
                payment.setSourceOrderNo(bill.getSourceOrderNo());
                payment.setAmount(bill.getTotalAmount());
                payment.setPaymentTime(LocalDateTime.now());
                payment.setOperator("RECONCILE");
                billPaymentMapper.insert(payment);
                saveRecord("PAYMENT_RECONCILE", "PAYMENT_MISSING", bill.getId(),
                        "已结算账单缺少收款流水，自动补记金额=" + bill.getTotalAmount(), true);
                log.warn("财务对账补偿: 账单 {} 补记收款流水 {}（对账自动补偿）",
                        bill.getId(), bill.getTotalAmount());
            } else if (paidTotal.compareTo(bill.getTotalAmount()) != 0) {
                mismatch++;
                saveRecord("PAYMENT_RECONCILE", "PAYMENT_AMOUNT_MISMATCH", bill.getId(),
                        "账单金额=" + bill.getTotalAmount() + ", 已收 " + payments.size() + " 笔共 " + paidTotal, false);
                log.error("财务对账差异: 账单 {} 金额={}, 已收 {} 笔共 {}",
                        bill.getId(), bill.getTotalAmount(), payments.size(), paidTotal);
            }
        }

        if (mismatch > 0) {
            log.error("财务对账完成: 检查 {} 笔已结算账单, 差异/补偿 {} 笔", settledBills.size(), mismatch);
        } else {
            log.info("财务对账完成: 检查 {} 笔已结算账单, 全部一致", settledBills.size());
        }
    }

    private void saveRecord(String taskCode, String recordType, String refNo,
                            String detail, boolean autoFixed) {
        ReconcileRecordPo record = new ReconcileRecordPo();
        record.setId(UUID.randomUUID().toString().replace("-", ""));
        record.setTaskCode(taskCode);
        record.setRecordType(recordType);
        record.setRefNo(refNo);
        record.setDetail(detail);
        record.setStatus(autoFixed ? "FIXED" : "OPEN");
        record.setAutoFixed(autoFixed ? 1 : 0);
        record.setCreateTime(LocalDateTime.now());
        if (autoFixed) {
            record.setFixTime(LocalDateTime.now());
        }
        reconcileRecordMapper.insert(record);
    }
}
