package com.demetrius.tribunal.financesettlement.application.service;

import com.demetrius.tribunal.financesettlement.common.dto.ChargeRequest;
import com.demetrius.tribunal.financesettlement.common.dto.SettlementView;
import com.demetrius.tribunal.financesettlement.domain.model.PaymentIdempotent;
import com.demetrius.tribunal.financesettlement.domain.model.SettlementOrder;
import com.demetrius.tribunal.financesettlement.domain.repository.AccountBalanceRepository;
import com.demetrius.tribunal.financesettlement.domain.repository.AccountTransactionRepository;
import com.demetrius.tribunal.financesettlement.domain.repository.PaymentIdempotentRepository;
import com.demetrius.tribunal.financesettlement.domain.repository.SettlementOrderRepository;
import com.demetrius.tribunal.financesettlement.domain.repository.SplitRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 结算应用服务单元测试（订单事件编排：结算单生成 → 幂等扣款，PRD 6.2）。
 */
@ExtendWith(MockitoExtension.class)
class SettlementApplicationServiceTest {

    @Mock
    private SettlementOrderRepository settlementOrderRepository;
    @Mock
    private SplitRecordRepository splitRecordRepository;
    @Mock
    private PaymentIdempotentRepository paymentIdempotentRepository;
    @Mock
    private AccountBalanceRepository accountBalanceRepository;
    @Mock
    private AccountTransactionRepository accountTransactionRepository;

    private SettlementApplicationService service;

    @BeforeEach
    void setUp() {
        service = new SettlementApplicationService(
                settlementOrderRepository, splitRecordRepository, paymentIdempotentRepository,
                accountBalanceRepository, accountTransactionRepository);
    }

    private SettlementOrder pendingOrder(String settlementId) {
        return new SettlementOrder(
                "id-1", settlementId, "ORD001", "USR_789", "MCH_001",
                "PENDING", new BigDecimal("851400"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("851400"), "WECHAT_PAY", "CNY", null);
    }

    @Test
    @DisplayName("订单事件编排：新结算单 PENDING → 扣款成功 CHARGED")
    void shouldCreateAndChargeNewSettlement() {
        // 首次：按订单号无结算单 → 新建
        when(settlementOrderRepository.findByOrderId("ORD001")).thenReturn(Optional.empty());
        // 新建后能查到 PENDING 结算单
        when(settlementOrderRepository.findBySettlementId(anyString()))
                .thenAnswer(inv -> Optional.of(pendingOrder(inv.getArgument(0))));
        // 无幂等记录
        when(paymentIdempotentRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());

        SettlementView view = service.createSettlementAndCharge(
                "ORD001", "USR_789", "MCH_001",
                new BigDecimal("851400"), "WECHAT_PAY", "CNY");

        assertEquals("CHARGED", view.getStatus());
        // save 被调用 2 次：建单 1 次 + 扣款更新 1 次
        verify(settlementOrderRepository, times(2)).save(any(SettlementOrder.class));
        verify(paymentIdempotentRepository).save(any(PaymentIdempotent.class));
    }

    @Test
    @DisplayName("幂等：结算单已扣款（CHARGED）时直接返回，不重复扣款")
    void shouldReturnExistingWhenAlreadyCharged() {
        SettlementOrder charged = pendingOrder("SET_1");
        charged.markCharged("CH_1234567890abcdef");

        when(settlementOrderRepository.findByOrderId("ORD001")).thenReturn(Optional.of(charged));
        when(settlementOrderRepository.findBySettlementId("SET_1")).thenReturn(Optional.of(charged));

        SettlementView view = service.createSettlementAndCharge(
                "ORD001", "USR_789", "MCH_001",
                new BigDecimal("851400"), "WECHAT_PAY", "CNY");

        assertEquals("CHARGED", view.getStatus());
        // 不产生新的幂等记录（未再次扣款）
        verify(paymentIdempotentRepository, never()).save(any(PaymentIdempotent.class));
    }

    @Test
    @DisplayName("重复事件：订单号已存在结算单时复用，不重复建单")
    void shouldReuseSettlementOnDuplicateEvent() {
        SettlementOrder existing = pendingOrder("SET_9");
        // createSettlement 只按订单号查重（FR-003），不查询 settlementId
        when(settlementOrderRepository.findByOrderId("ORD001")).thenReturn(Optional.of(existing));

        String settlementId = service.createSettlement("ORD001", "USR_789", "MCH_001",
                new BigDecimal("851400"), "WECHAT_PAY", "CNY");

        assertEquals("SET_9", settlementId);
    }

    @Test
    @DisplayName("直接扣款：重复幂等键已成功时禁止重复扣款（FIN-002 红线）")
    void shouldRejectDuplicateChargeBySameIdempotencyKey() {
        when(paymentIdempotentRepository.findByIdempotencyKey("SET_1_BATCH_1"))
                .thenReturn(Optional.of(new PaymentIdempotent(
                        "SET_1_BATCH_1", "SET_1", "SUCCESS", "channelTxnId=x", "2026-12-31")));

        ChargeRequest request = new ChargeRequest();
        request.setSettlementId("SET_1");
        request.setIdempotencyKey("SET_1_BATCH_1");
        request.setAmount(new BigDecimal("851400"));
        request.setPaymentMethod("WECHAT_PAY");

        try {
            service.charge(request);
        } catch (Exception e) {
            assertEquals("FIN-002", ((com.demetrius.tribunal.financesettlement.common.exception.BizException) e).getCode());
            return;
        }
        throw new AssertionError("预期抛 FIN-002 幂等重复异常");
    }
}
