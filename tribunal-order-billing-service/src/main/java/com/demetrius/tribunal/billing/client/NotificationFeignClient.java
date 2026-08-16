package com.demetrius.tribunal.billing.client;

import com.demetrius.tribunal.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 通知服务（notification-service）的 Feign 客户端。
 *
 * <p>对应需求：F-701~F-703（站内信/邮件/短信）。</p>
 *
 * <p>用途：对账发现差异时发送站内信告警（对账结果产品化）。</p>
 *
 * <p>M5 熔断/降级：通知为非关键路径，fallback 静默降级（仅记日志，不阻断对账主流程）。</p>
 */
@FeignClient(name = "tribunal-order-notification-service",
        fallbackFactory = NotificationFeignFallbackFactory.class)
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
