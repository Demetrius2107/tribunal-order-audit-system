package com.demetrius.tribunal.order.application.service;

import com.demetrius.tribunal.order.application.dto.AfterSaleResult;
import com.demetrius.tribunal.order.client.InventoryFeignClient;
import com.demetrius.tribunal.order.domain.model.*;
import com.demetrius.tribunal.order.domain.repository.AfterSaleRepository;
import com.demetrius.tribunal.order.domain.repository.OrderRepository;
import com.demetrius.tribunal.common.exception.BizException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 售后应用服务（用例编排层）。
 *
 * <p>编排售后全流程：发起申请 → 审核确认 → 退款执行（含库存回滚）。</p>
 *
 * <p>核心规则：</p>
 * <ul>
 *   <li>仅已签收订单可发起售后</li>
 *   <li>退货退款：审核通过 → 仓库收货确认 → 执行退款 + 库存回滚</li>
 *   <li>仅退款：审核通过 → 直接执行退款（不涉及库存回滚）</li>
 *   <li>退款流水号由本服务生成（模拟金融结算返回，TODO 接入 finance-settlement）</li>
 * </ul>
 */
@Service
public class AfterSaleApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AfterSaleApplicationService.class);

    private final AfterSaleRepository afterSaleRepository;
    private final OrderRepository orderRepository;
    private final InventoryFeignClient inventoryFeignClient;

    public AfterSaleApplicationService(AfterSaleRepository afterSaleRepository,
                                       OrderRepository orderRepository,
                                       InventoryFeignClient inventoryFeignClient) {
        this.afterSaleRepository = afterSaleRepository;
        this.orderRepository = orderRepository;
        this.inventoryFeignClient = inventoryFeignClient;
    }

    /**
     * 发起售后申请。
     */
    @Transactional
    public AfterSaleResult createAfterSale(String orderId, String type, String reason,
                                           List<AfterSale.ReturnRequest> returnItems) {
        Order order = orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new BizException("404001", "订单不存在: " + orderId));

        AfterSaleType afterSaleType = AfterSaleType.valueOf(type);
        AfterSaleReason afterSaleReason = AfterSaleReason.valueOf(reason);

        String id = UUID.randomUUID().toString().replace("-", "");
        String afterSaleNo = generateAfterSaleNo();

        AfterSale afterSale = AfterSale.create(id, afterSaleNo, order, afterSaleType,
                afterSaleReason, returnItems);
        afterSaleRepository.save(afterSale);

        log.info("售后申请已创建: afterSaleNo={}, orderId={}, type={}, refund={}",
                afterSaleNo, orderId, type, afterSale.getTotalRefundAmount());
        return AfterSaleResult.from(afterSale);
    }

    /**
     * 审核售后单。
     */
    @Transactional
    public AfterSaleResult review(String afterSaleId, boolean approved, String rejectReason) {
        AfterSale afterSale = afterSaleRepository.findById(afterSaleId)
                .orElseThrow(() -> new BizException("404002", "售后单不存在: " + afterSaleId));

        if (approved) {
            afterSale.approve();
            // 仅退款：审核通过后直接完成退款
            if (afterSale.getType() == AfterSaleType.REFUND_ONLY) {
                String txnNo = executeRefund(afterSale);
                afterSale.complete(txnNo);
            }
        } else {
            if (rejectReason == null || rejectReason.isBlank()) {
                throw new BizException("400002", "拒绝时必须填写拒绝原因");
            }
            afterSale.reject(rejectReason);
        }

        afterSaleRepository.save(afterSale);
        log.info("售后审核完成: afterSaleNo={}, approved={}", afterSale.getAfterSaleNo(), approved);
        return AfterSaleResult.from(afterSale);
    }

    /**
     * 确认收货并完成退款（退货退款类型专用）。
     *
     * <p>仓库收到退回商品后调用：执行库存回滚 + 退款。</p>
     */
    @Transactional
    public AfterSaleResult confirmReceiptAndRefund(String afterSaleId) {
        AfterSale afterSale = afterSaleRepository.findById(afterSaleId)
                .orElseThrow(() -> new BizException("404002", "售后单不存在: " + afterSaleId));

        if (afterSale.getStatus() != AfterSaleStatus.APPROVED) {
            throw new BizException("400003", "仅已审核状态的售后单可确认收货");
        }

        // 1. 库存回滚（退货入库）
        if (afterSale.getType() == AfterSaleType.RETURN_REFUND) {
            for (AfterSaleItem item : afterSale.getItems()) {
                try {
                    inventoryFeignClient.returnStock(item.skuCode(), item.quantity());
                    log.info("退货入库成功: sku={}, qty={}", item.skuCode(), item.quantity());
                } catch (FeignException e) {
                    log.error("退货入库失败（库存服务不可用）: sku={}, qty={}", item.skuCode(), item.quantity(), e);
                    throw new BizException("500001", "库存回滚失败，请重试: " + item.skuCode());
                }
            }
        }

        // 2. 执行退款
        String txnNo = executeRefund(afterSale);
        afterSale.complete(txnNo);
        afterSaleRepository.save(afterSale);

        log.info("售后退款完成: afterSaleNo={}, refund={}", afterSale.getAfterSaleNo(),
                afterSale.getTotalRefundAmount());
        return AfterSaleResult.from(afterSale);
    }

    /**
     * 查询售后单。
     */
    @Transactional(readOnly = true)
    public AfterSaleResult getAfterSale(String afterSaleId) {
        AfterSale afterSale = afterSaleRepository.findById(afterSaleId)
                .orElseThrow(() -> new BizException("404002", "售后单不存在: " + afterSaleId));
        return AfterSaleResult.from(afterSale);
    }

    /**
     * 按订单查询售后列表。
     */
    @Transactional(readOnly = true)
    public List<AfterSaleResult> listByOrder(String orderId) {
        return afterSaleRepository.findByOrderId(orderId).stream()
                .map(AfterSaleResult::from)
                .toList();
    }

    // ---------- 内部方法 ----------

    /**
     * 执行退款（模拟金融结算服务）。
     *
     * <p>TODO（学习任务）：接入 tribunal-finance-settlement-service 的退款接口，
     * 生成真实的退款流水号。当前版本模拟返回。</p>
     */
    private String executeRefund(AfterSale afterSale) {
        String txnNo = "RFN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 9999);
        log.info("退款已执行（模拟）: afterSaleNo={}, amount={}, txnNo={}",
                afterSale.getAfterSaleNo(), afterSale.getTotalRefundAmount(), txnNo);
        return txnNo;
    }

    private String generateAfterSaleNo() {
        return "AS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
