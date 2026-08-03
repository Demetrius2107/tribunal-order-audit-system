package com.demetrius.tribunal.billing.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * order-service 的 Feign 客户端（状态回传）。
 *
 * <p>对应需求：F-503（状态回传）、F-308、N-304（幂等）。</p>
 *
 * <p>用途：金融账单状态变更后，回传 订单服务 驱动订单状态机。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>回传失败重试（记录待重传表，定时任务补偿）</li>
 *   <li>接入 Nacos 后去掉 url 直连</li>
 * </ul>
 */
@FeignClient(name = "tribunal-order-service",
        url = "${order.service.url:http://localhost:8080}")
public interface OrderStatusFeignClient {

    /**
     * 回传履约状态：调用 订单服务 的 POST /api/orders/status-callback。
     */
    @PostMapping("/api/orders/status-callback")
    void statusCallback(@RequestBody OrderStatusCallbackRequest request);
}
