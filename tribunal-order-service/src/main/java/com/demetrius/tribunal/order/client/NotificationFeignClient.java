package com.demetrius.tribunal.order.client;

import com.demetrius.tribunal.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 通知服务（notification-service）的 Feign 客户端。
 *
 * <p>对应需求：F-701~F-703（站内信/邮件/短信）。</p>
 *
 * <p>用途：下单/审单/状态变更时发送通知（站内信给经销商、邮件给销售）。</p>
 */
@FeignClient(name = "tribunal-order-notification-service",
        url = "${notification.service.url:http://localhost:8086}")
public interface NotificationFeignClient {

    /**
     * 发送通知：POST /api/notifications
     */
    @PostMapping("/api/notifications")
    ApiResponse<Void> send(@RequestBody NotificationSendRequest request);

    /**
     * 发送通知请求体。
     */
    record NotificationSendRequest(
            String type,
            String receiver,
            String title,
            String content) {
    }
}
