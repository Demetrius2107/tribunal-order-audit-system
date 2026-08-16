package com.demetrius.tribunal.order.infrastructure.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.application.dto.OrderEventMessage;
import com.demetrius.tribunal.order.client.BillTransferResult;
import com.demetrius.tribunal.order.client.BillingFeignClient;
import com.demetrius.tribunal.order.client.NotificationFeignClient;
import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.repository.OrderRepository;
import com.demetrius.tribunal.order.infrastructure.event.OrderEventPublisher;
import com.demetrius.tribunal.order.infrastructure.mapper.OrderMapper;
import com.demetrius.tribunal.order.infrastructure.mapper.ReconcileRecordMapper;
import com.demetrius.tribunal.order.infrastructure.model.OrderPo;
import com.demetrius.tribunal.order.infrastructure.model.ReconcileRecordPo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 状态对账任务（F-801：订单 vs 账单状态一致性核对，差异落库 + 自动修复）。
 *
 * <p>审单通过后的订单（CONFIRMED 及其后续状态）应存在非取消状态的金融账单：</p>
 * <ul>
 *   <li>账单缺失 → 自动补发 OrderApproved 事件（outbox → Kafka → billing 幂等消费生成账单，
 *       标记 FIXED）</li>
 *   <li>账单已取消 → 记录 OPEN（重发会被 billing 幂等拦截，需人工处理）</li>
 * </ul>
 */
@Component
public class OrderBillStatusReconcileTask {

    private static final Logger log = LoggerFactory.getLogger(OrderBillStatusReconcileTask.class);

    /** 审单通过后应已有账单的订单状态集合（订单状态机中 CONFIRMED 及其后续） */
    private static final List<String> SHOULD_HAVE_BILL_STATUSES = List.of(
            "CONFIRMED", "TRANSFERRING", "TRANSFERRED",
            "SPLITTING", "SPLITTED", "PARTIALLY_SHIPPED",
            "SHIPPED", "PARTIALLY_SIGNED", "SIGNED");

    private final OrderMapper orderMapper;

    private final OrderRepository orderRepository;

    private final OrderEventPublisher orderEventPublisher;

    private final BillingFeignClient billingFeignClient;

    private final ReconcileRecordMapper reconcileRecordMapper;

    private final NotificationFeignClient notificationFeignClient;

    public OrderBillStatusReconcileTask(OrderMapper orderMapper,
                                        OrderRepository orderRepository,
                                        OrderEventPublisher orderEventPublisher,
                                        BillingFeignClient billingFeignClient,
                                        ReconcileRecordMapper reconcileRecordMapper,
                                        NotificationFeignClient notificationFeignClient) {
        this.orderMapper = orderMapper;
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.billingFeignClient = billingFeignClient;
        this.reconcileRecordMapper = reconcileRecordMapper;
        this.notificationFeignClient = notificationFeignClient;
    }

    /**
     * 每小时核对一次：应存在账单的订单是否在 billing-service 有非取消账单。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void reconcile() {
        List<OrderPo> orders = orderMapper.selectList(
                new LambdaQueryWrapper<OrderPo>()
                        .in(OrderPo::getStatus, SHOULD_HAVE_BILL_STATUSES));

        int mismatch = 0;
        for (OrderPo order : orders) {
            try {
                ApiResponse<BillTransferResult> resp =
                        billingFeignClient.getBillBySourceOrderNo(order.getOrderNo());
                BillTransferResult bill = resp.getData();
                if (bill == null) {
                    // 账单缺失：自动补发 OrderApproved 事件，billing 幂等消费生成账单
                    mismatch++;
                    autoFixMissingBill(order);
                } else if ("CANCELLED".equals(bill.status())) {
                    // 账单已取消：重发会被 billing 幂等拦截，记录 OPEN 供人工处理
                    mismatch++;
                    String detail = "订单 " + order.getOrderNo() + " 状态=" + order.getStatus()
                            + "，账单已取消";
                    saveRecord("STATUS_RECONCILE", "BILL_CANCELLED", order.getOrderNo(), detail, false);
                    log.error("状态对账差异: {}", detail);
                }
            } catch (Exception e) {
                mismatch++;
                log.warn("状态对账查询失败: orderNo={}, error={}", order.getOrderNo(), e.getMessage());
            }
        }

        if (mismatch > 0) {
            log.error("状态对账完成: 检查 {} 单, 差异 {} 单", orders.size(), mismatch);
            sendAlert("状态对账异常告警", "检查 " + orders.size() + " 单, 差异/修复 " + mismatch + " 单");
        } else {
            log.info("状态对账完成: 检查 {} 单, 全部一致", orders.size());
        }
    }

    /**
     * 自动修复：账单缺失时补发 OrderApproved 事件（outbox → Kafka → billing 幂等消费生成账单）。
     */
    private void autoFixMissingBill(OrderPo orderPo) {
        Order order = orderRepository.findByOrderNo(orderPo.getOrderNo()).orElse(null);
        if (order == null) {
            log.warn("状态对账自动修复失败: 订单不存在 orderNo={}", orderPo.getOrderNo());
            return;
        }
        orderEventPublisher.publishOrderEvent(buildApprovedEvent(order));
        String detail = "订单 " + order.getOrderNo() + " 状态=" + order.getStatus()
                + "，账单缺失，已自动补发 OrderApproved 事件";
        saveRecord("STATUS_RECONCILE", "BILL_MISSING", order.getOrderNo(), detail, true);
        log.error("状态对账自动修复: {}", detail);
    }

    /** 构建 OrderApproved 事件（与审单发布同构，供对账补发）。 */
    private OrderEventMessage buildApprovedEvent(Order order) {
        List<OrderEventMessage.Item> items = order.getSkus().stream()
                .map(s -> new OrderEventMessage.Item(
                        s.getSkuCode(), s.getSkuName(), s.getQuantity().intValue(), s.getPrice(), null, null))
                .toList();
        return new OrderEventMessage(
                UUID.randomUUID().toString().replace("-", ""),
                "OrderApproved",
                order.getOrderNo(),
                order.getCustomerId(),
                null,
                items,
                BigDecimal.ZERO,
                order.getDiscountAmount(),
                null,
                "CNY",
                order.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")),
                null,
                null);
    }

    /**
     * 差异告警：站内信通知运营（通知失败不影响对账结果）。
     */
    private void sendAlert(String title, String content) {
        try {
            notificationFeignClient.send(new NotificationFeignClient.NotificationSendRequest(
                    "SITE_MESSAGE", "admin", title, content));
        } catch (Exception e) {
            log.warn("状态对账告警通知发送失败: title={}, error={}", title, e.getMessage());
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
