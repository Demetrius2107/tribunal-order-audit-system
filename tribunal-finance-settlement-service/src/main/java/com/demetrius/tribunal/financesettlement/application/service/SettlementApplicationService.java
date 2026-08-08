package com.demetrius.tribunal.financesettlement.application.service;

import com.demetrius.tribunal.common.dto.finance.ChargeRequest;
import com.demetrius.tribunal.common.dto.finance.SettlementView;
import com.demetrius.tribunal.common.dto.finance.SplitRequest;
import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.financesettlement.domain.model.AccountBalance;
import com.demetrius.tribunal.financesettlement.domain.model.AccountTransaction;
import com.demetrius.tribunal.financesettlement.domain.model.PaymentIdempotent;
import com.demetrius.tribunal.financesettlement.domain.model.SettlementOrder;
import com.demetrius.tribunal.financesettlement.domain.model.SplitRecord;
import com.demetrius.tribunal.financesettlement.domain.repository.AccountBalanceRepository;
import com.demetrius.tribunal.financesettlement.domain.repository.AccountTransactionRepository;
import com.demetrius.tribunal.financesettlement.domain.repository.PaymentIdempotentRepository;
import com.demetrius.tribunal.financesettlement.domain.repository.SettlementOrderRepository;
import com.demetrius.tribunal.financesettlement.domain.repository.SplitRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 结算应用服务（PRD 2.2 支付扣款层 / 2.3 分账层 / 2.5 结算打款层）。
 *
 * <p>正向流程：生成结算单(PENDING) → 幂等扣款(CHARGED) → 分账(SPLIT) → 结算(SETTLED)（PRD 6.2）。</p>
 *
 * <p>基建说明：当前实现扣款幂等检查与分账主链路骨架，支付渠道调用（微信/支付宝等）、
 * 账单明细拆解、分账规则引擎、结算批次等留待后续按 PRD 填充。</p>
 */
@Service
public class SettlementApplicationService {

    private final SettlementOrderRepository settlementOrderRepository;
    private final SplitRecordRepository splitRecordRepository;
    private final PaymentIdempotentRepository paymentIdempotentRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountTransactionRepository accountTransactionRepository;

    public SettlementApplicationService(SettlementOrderRepository settlementOrderRepository,
                                        SplitRecordRepository splitRecordRepository,
                                        PaymentIdempotentRepository paymentIdempotentRepository,
                                        AccountBalanceRepository accountBalanceRepository,
                                        AccountTransactionRepository accountTransactionRepository) {
        this.settlementOrderRepository = settlementOrderRepository;
        this.splitRecordRepository = splitRecordRepository;
        this.paymentIdempotentRepository = paymentIdempotentRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountTransactionRepository = accountTransactionRepository;
    }

    /**
     * 订单事件编排：生成结算单(PENDING) → 幂等扣款(CHARGED)（PRD 6.2 正向流程起点）。
     *
     * <p>幂等：重复收到同一订单事件时，结算单已生成且已扣款则直接返回（FR-003/FR-016）。</p>
     */
    @Transactional
    public SettlementView createSettlementAndCharge(String orderId, String userId, String merchantId,
                                                    BigDecimal netAmount, String paymentMethod, String currency) {
        String settlementId = createSettlement(orderId, userId, merchantId, netAmount, paymentMethod, currency);

        // 幂等：已扣款/扣款中直接返回成功（FR-016），不再重复扣款
        SettlementOrder existing = settlementOrderRepository.findBySettlementId(settlementId)
                .orElseThrow(() -> new BizException("FIN-001", "结算单不存在: " + settlementId));
        if ("CHARGED".equals(existing.getStatus()) || "CHARGING".equals(existing.getStatus())) {
            return toView(existing);
        }

        ChargeRequest chargeRequest = new ChargeRequest();
        chargeRequest.setSettlementId(settlementId);
        chargeRequest.setIdempotencyKey(settlementId + "_BATCH_1");
        chargeRequest.setAmount(netAmount);
        chargeRequest.setCurrency(currency);
        chargeRequest.setPaymentMethod(paymentMethod);
        return charge(chargeRequest);
    }

    /**
     * 按订单生成结算单（监听订单完成事件，PRD 2.1.1 FR-001，幂等：已生成则忽略 FR-003）。
     */
    @Transactional
    public String createSettlement(String orderId, String userId, String merchantId, BigDecimal netAmount,
                                   String paymentMethod, String currency) {
        if (settlementOrderRepository.findByOrderId(orderId).isPresent()) {
            // 基于订单号幂等（PRD 7：重复收到同一订单完成事件，已生成结算单则忽略）
            return settlementOrderRepository.findByOrderId(orderId).get().getSettlementId();
        }
        String settlementId = "SET_" + System.currentTimeMillis();
        SettlementOrder order = new SettlementOrder(
                UUID.randomUUID().toString().replace("-", ""),
                settlementId, orderId, userId, merchantId, "PENDING",
                netAmount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, netAmount,
                paymentMethod, currency, null);
        settlementOrderRepository.save(order);
        return settlementId;
    }

    /**
     * 幂等扣款（PRD 2.2.2 FR-015 核心红线：同一结算单号绝对不能重复扣款）。
     *
     * <p>扣款前查询"该结算单是否已扣款成功"，已扣款直接返回成功（FR-016）。</p>
     */
    @Transactional
    public SettlementView charge(ChargeRequest request) {
        // 幂等检查：幂等键已成功则直接返回，禁止重复扣（FIN-002）
        paymentIdempotentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .filter(record -> "SUCCESS".equals(record.getStatus()))
                .ifPresent(record -> {
                    throw new BizException("FIN-002", "扣款幂等重复，禁止重复扣款: " + request.getSettlementId());
                });

        SettlementOrder order = settlementOrderRepository.findBySettlementId(request.getSettlementId())
                .orElseThrow(() -> new BizException("FIN-001", "结算单不存在: " + request.getSettlementId()));

        // 基建：真实支付渠道调用（PRD 2.2.1/2.2.3）留待后续实现，此处直接标记扣款成功
        String channelTransactionId = "CH_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        order.markCharged(channelTransactionId);
        settlementOrderRepository.save(order);

        saveChargeIdempotent(request, channelTransactionId);
        return toView(order);
    }

    /**
     * 分账执行（PRD 2.3.2 FR-028 实时分账）。
     *
     * <p>比例之和校验（FR-025/FIN-005）+ 分账方账户入账 + 流水记录。</p>
     */
    @Transactional
    public void split(SplitRequest request) {
        List<SplitRecord> existing = splitRecordRepository.findBySettlementId(request.getSettlementId());
        if (!existing.isEmpty()) {
            throw new BizException("FIN-001", "结算单已分账: " + request.getSettlementId());
        }
        // 分账比例校验：各分账方金额之和必须等于分账总金额（PRD 2.3.1 FR-025）
        BigDecimal sum = request.getSplits().stream()
                .map(SplitRequest.SplitItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(request.getTotalAmount()) != 0) {
            throw new BizException("FIN-005", "分账金额之和必须等于总金额");
        }

        for (SplitRequest.SplitItem item : request.getSplits()) {
            SplitRecord record = new SplitRecord(
                    UUID.randomUUID().toString().replace("-", ""),
                    request.getSettlementId(), item.getRecipientId(), item.getRecipientType(),
                    item.getAmount(),
                    item.getAmount().divide(request.getTotalAmount(), 4, RoundingMode.HALF_UP),
                    "SUCCESS", null);
            splitRecordRepository.save(record);
            creditAccount(item.getRecipientId(), item.getAmount(), request.getSettlementId());
        }
    }

    /** 分账方账户入账（账户不存在则自动开户，FR-032） */
    private void creditAccount(String accountId, BigDecimal amount, String settlementId) {
        AccountBalance account = accountBalanceRepository.findByAccountId(accountId)
                .orElseGet(() -> new AccountBalance(
                        UUID.randomUUID().toString().replace("-", ""),
                        accountId, accountId, "MERCHANT",
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "CNY", 0L));
        account.credit(amount);
        accountBalanceRepository.save(account);

        accountTransactionRepository.save(new AccountTransaction(
                UUID.randomUUID().toString().replace("-", ""),
                "TX_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                accountId, "SPLIT_IN", amount, settlementId, null,
                account.getAvailableBalance(), "分账入账"));
    }

    private void saveChargeIdempotent(ChargeRequest request, String channelTransactionId) {
        String expireAt = LocalDateTime.now().plusDays(30)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        paymentIdempotentRepository.save(new PaymentIdempotent(
                request.getIdempotencyKey(), request.getSettlementId(), "SUCCESS",
                "channelTxnId=" + channelTransactionId, expireAt));
    }

    private SettlementView toView(SettlementOrder order) {
        SettlementView view = new SettlementView();
        view.setSettlementId(order.getSettlementId());
        view.setOrderId(order.getOrderId());
        view.setMerchantId(order.getMerchantId());
        view.setStatus(order.getStatus());
        view.setTotalAmount(order.getTotalAmount());
        view.setDiscountAmount(order.getDiscountAmount());
        view.setShippingFee(order.getShippingFee());
        view.setTaxAmount(order.getTaxAmount());
        view.setPlatformFee(order.getPlatformFee());
        view.setPaymentFee(order.getPaymentFee());
        view.setNetAmount(order.getNetAmount());
        view.setPaymentMethod(order.getPaymentMethod());
        view.setPaymentCurrency(order.getPaymentCurrency());
        view.setChannelTransactionId(order.getChannelTransactionId());
        return view;
    }
}
