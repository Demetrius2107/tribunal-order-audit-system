package com.demetrius.tribunal.billing.infrastructure.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.billing.infrastructure.mapper.BillMapper;
import com.demetrius.tribunal.billing.infrastructure.mapper.BillPaymentMapper;
import com.demetrius.tribunal.billing.infrastructure.model.BillPaymentPo;
import com.demetrius.tribunal.billing.infrastructure.model.BillPo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务对账任务（F-607：账单金额 vs 收款流水金额核对，差异告警）。
 *
 * <p>已结算（SETTLED）账单应有金额一致的收款流水（t_bill_payment）；
 * 金额不一致或缺少收款流水即为差异，记录告警日志。</p>
 */
@Component
public class PaymentReconcileTask {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconcileTask.class);

    private final BillMapper billMapper;

    private final BillPaymentMapper billPaymentMapper;

    public PaymentReconcileTask(BillMapper billMapper,
                                BillPaymentMapper billPaymentMapper) {
        this.billMapper = billMapper;
        this.billPaymentMapper = billPaymentMapper;
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
            if (paidTotal.compareTo(bill.getTotalAmount()) != 0) {
                mismatch++;
                log.error("财务对账差异: 账单 {} 金额={}, 已收 {} 笔共 {}",
                        bill.getId(), bill.getTotalAmount(), payments.size(), paidTotal);
            }
        }

        if (mismatch > 0) {
            log.error("财务对账完成: 检查 {} 笔已结算账单, 差异 {} 笔", settledBills.size(), mismatch);
        } else {
            log.info("财务对账完成: 检查 {} 笔已结算账单, 全部一致", settledBills.size());
        }
    }
}
