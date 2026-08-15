package com.demetrius.tribunal.order.client;

import com.demetrius.tribunal.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 金融账单服务（billing-service）的 Feign 客户端（转单调用）。
 *
 * <p>对应需求：F-307（转单）、F-308（状态回传）。</p>
 *
 * <p>用途：审单通过后，把订单数据转给下游生成金融账单；
 * 账单状态变更后通过回传接口驱动本地订单状态机。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>转单失败处理：重试 + 记录失败表</li>
 *   <li>接入 Nacos 后去掉 url 直连，用服务名路由</li>
 * </ul>
 */
@FeignClient(name = "tribunal-order-billing-service",
        url = "${billing.service.url:http://localhost:8082}")
public interface BillingFeignClient {

    /**
     * 转单：调用账单服务的 POST /api/bills 生成账单。
     */
    @PostMapping("/api/bills")
    ApiResponse<BillTransferResult> transfer(@RequestBody BillTransferRequest request);

    /**
     * 按上游订单编号查询账单（对账任务用：F-801 状态对账）。
     */
    @GetMapping("/api/bills/by-order/{sourceOrderNo}")
    ApiResponse<BillTransferResult> getBillBySourceOrderNo(@PathVariable String sourceOrderNo);
}
