package com.demetrius.tribunal.billing.application.service;

import com.demetrius.tribunal.billing.application.dto.BillReceiveCommand;
import com.demetrius.tribunal.billing.application.dto.BillResult;
import com.demetrius.tribunal.billing.domain.model.BillId;
import com.demetrius.tribunal.billing.domain.model.BillLine;
import com.demetrius.tribunal.billing.domain.model.BillStatus;
import com.demetrius.tribunal.billing.domain.model.FinanceBill;
import com.demetrius.tribunal.billing.domain.repository.BillRepository;
import com.demetrius.tribunal.billing.infrastructure.mapper.BillPaymentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 异步补偿闭环单测：billing generateBill 幂等（对账自动补账单重发不重复生成）。
 */
class BillingApplicationServiceIdempotencyTest {

    private final BillRepository billRepository = mock(BillRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final BillPaymentMapper billPaymentMapper = mock(BillPaymentMapper.class);

    private final BillingApplicationService service =
            new BillingApplicationService(billRepository, eventPublisher, billPaymentMapper);

    private BillReceiveCommand command(String sourceOrderNo) {
        return new BillReceiveCommand(
                sourceOrderNo, "cust-001",
                List.of(new BillReceiveCommand.BillLineItem("SKU001", "商品A", BigDecimal.TEN, new BigDecimal("100.00"))));
    }

    @Test
    @DisplayName("首次转单：生成账单并保存")
    void generateBillFirstTime() {
        when(billRepository.findBySourceOrderNo("ORD001")).thenReturn(Optional.empty());

        BillResult result = service.generateBill(command("ORD001"));

        assertNotNull(result.billId());
        verify(billRepository).save(any(FinanceBill.class));
    }

    @Test
    @DisplayName("重复转单（对账自动补账单重发）：幂等命中，不重复生成")
    void generateBillIdempotentOnDuplicate() {
        FinanceBill exist = FinanceBill.generate(
                new BillId("bill-001"), "ORD001", "cust-001",
                List.of(new BillLine("SKU001", "商品A", BigDecimal.TEN, new BigDecimal("100.00"))));
        when(billRepository.findBySourceOrderNo("ORD001")).thenReturn(Optional.of(exist));

        BillResult result = service.generateBill(command("ORD001"));

        assertEquals("bill-001", result.billId(), "应返回已有账单");
        assertEquals(BillStatus.GENERATED, result.status());
        verify(billRepository, never()).save(any(FinanceBill.class));
    }
}
