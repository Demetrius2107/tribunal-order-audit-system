package com.demetrius.tribunal.order.infrastructure.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.client.BillTransferResult;
import com.demetrius.tribunal.order.client.BillingFeignClient;
import com.demetrius.tribunal.order.client.NotificationFeignClient;
import com.demetrius.tribunal.order.infrastructure.mapper.OrderMapper;
import com.demetrius.tribunal.order.infrastructure.mapper.ReconcileRecordMapper;
import com.demetrius.tribunal.order.infrastructure.model.OrderPo;
import com.demetrius.tribunal.order.infrastructure.model.ReconcileRecordPo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 状态对账任务（F-801：订单 vs 账单状态一致性核对，差异落库）。
 *
 * <p>审单通过后的订单（CONFIRMED 及其后续状态）应存在非取消状态的金融账单；
 * 差异写入 {@code t_reconcile_record}（OPEN），供人工/后续兜底处理。</p>
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

    private final BillingFeignClient billingFeignClient;

    private final ReconcileRecordMapper reconcileRecordMapper;

    private final NotificationFeignClient notificationFeignClient;

    public OrderBillStatusReconcileTask(OrderMapper orderMapper,
                                        BillingFeignClient billingFeignClient,
                                        ReconcileRecordMapper reconcileRecordMapper,
                                        NotificationFeignClient notificationFeignClient) {
        this.orderMapper = orderMapper;
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
                if (bill == null || "CANCELLED".equals(bill.status())) {
                    mismatch++;
                    String detail = "订单 " + order.getOrderNo() + " 状态=" + order.getStatus()
                            + "，账单缺失或已取消";
                    saveRecord("STATUS_RECONCILE", "BILL_MISSING", order.getOrderNo(), detail);
                    log.error("状态对账差异: {}", detail);
                }
            } catch (Exception e) {
                mismatch++;
                log.warn("状态对账查询失败: orderNo={}, error={}", order.getOrderNo(), e.getMessage());
            }
        }

        if (mismatch > 0) {
            log.error("状态对账完成: 检查 {} 单, 差异 {} 单", orders.size(), mismatch);
            sendAlert("状态对账异常告警", "检查 " + orders.size() + " 单, 差异 " + mismatch + " 单");
        } else {
            log.info("状态对账完成: 检查 {} 单, 全部一致", orders.size());
        }
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

    private void saveRecord(String taskCode, String recordType, String refNo, String detail) {
        ReconcileRecordPo record = new ReconcileRecordPo();
        record.setId(UUID.randomUUID().toString().replace("-", ""));
        record.setTaskCode(taskCode);
        record.setRecordType(recordType);
        record.setRefNo(refNo);
        record.setDetail(detail);
        record.setStatus("OPEN");
        record.setAutoFixed(0);
        record.setCreateTime(LocalDateTime.now());
        reconcileRecordMapper.insert(record);
    }
}
