package com.demetrius.tribunal.billing.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demetrius.tribunal.billing.application.dto.BillListItemResult;
import com.demetrius.tribunal.billing.application.dto.BillPageResult;
import com.demetrius.tribunal.billing.application.dto.BillPaymentResult;
import com.demetrius.tribunal.billing.infrastructure.mapper.BillMapper;
import com.demetrius.tribunal.billing.infrastructure.mapper.BillPaymentMapper;
import com.demetrius.tribunal.billing.infrastructure.model.BillPaymentPo;
import com.demetrius.tribunal.billing.infrastructure.model.BillPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 账单报表查询应用服务（对外报表：账单分页列表 + 收款流水明细）。
 */
@Service
public class BillQueryApplicationService {

    private final BillMapper billMapper;

    private final BillPaymentMapper billPaymentMapper;

    public BillQueryApplicationService(BillMapper billMapper,
                                       BillPaymentMapper billPaymentMapper) {
        this.billMapper = billMapper;
        this.billPaymentMapper = billPaymentMapper;
    }

    /**
     * 账单分页列表（可按状态/客户过滤，时间倒序）。
     */
    @Transactional(readOnly = true)
    public BillPageResult pageBills(String status, String customerId, long pageNum, long pageSize) {
        LambdaQueryWrapper<BillPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null && !status.isBlank(), BillPo::getStatus, status)
               .eq(customerId != null && !customerId.isBlank(), BillPo::getCustomerId, customerId)
               .orderByDesc(BillPo::getGeneratedAt);

        Page<BillPo> page = billMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<BillListItemResult> bills = page.getRecords().stream()
                .map(BillListItemResult::from)
                .toList();
        return BillPageResult.of(page.getTotal(), pageNum, pageSize, bills);
    }

    /**
     * 账单收款流水明细（按账单 ID 查询）。
     */
    @Transactional(readOnly = true)
    public List<BillPaymentResult> listPayments(String billId) {
        List<BillPaymentPo> payments = billPaymentMapper.selectList(
                new LambdaQueryWrapper<BillPaymentPo>()
                        .eq(BillPaymentPo::getBillId, billId)
                        .orderByAsc(BillPaymentPo::getPaymentTime));
        return payments.stream().map(BillPaymentResult::from).toList();
    }
}
