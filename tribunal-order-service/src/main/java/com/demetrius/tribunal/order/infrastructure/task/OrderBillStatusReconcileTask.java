package com.demetrius.tribunal.order.infrastructure.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.client.BillTransferResult;
import com.demetrius.tribunal.order.client.BillingFeignClient;
import com.demetrius.tribunal.order.infrastructure.mapper.OrderMapper;
import com.demetrius.tribunal.order.infrastructure.model.OrderPo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 状态对账任务（F-801：订单 vs 账单状态一致性核对，差异告警）。
 *
 * <p>审单通过后的订单（CONFIRMED 及其后续状态）应存在非取消状态的金融账单；
 * 账单缺失或已取消即为差异，记录告警日志，由人工/后续任务兜底。</p>
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

    public OrderBillStatusReconcileTask(OrderMapper orderMapper,
                                        BillingFeignClient billingFeignClient) {
        this.orderMapper = orderMapper;
        this.billingFeignClient = billingFeignClient;
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
                    log.error("状态对账差异: 订单 {} 状态={}，但账单缺失或已取消",
                            order.getOrderNo(), order.getStatus());
                }
            } catch (Exception e) {
                mismatch++;
                log.warn("状态对账查询失败: orderNo={}, error={}", order.getOrderNo(), e.getMessage());
            }
        }

        if (mismatch > 0) {
            log.error("状态对账完成: 检查 {} 单, 差异 {} 单", orders.size(), mismatch);
        } else {
            log.info("状态对账完成: 检查 {} 单, 全部一致", orders.size());
        }
    }
}
