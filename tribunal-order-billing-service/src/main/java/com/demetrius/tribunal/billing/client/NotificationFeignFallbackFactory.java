package com.demetrius.tribunal.billing.client;

import com.demetrius.tribunal.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * NotificationFeignClient 降级回退（M5 Resilience4j）。
 *
 * <p>通知（对账差异告警）是非关键路径，失败可静默降级——仅记日志，
 * 不阻断对账主流程（对账结果已落 t_reconcile_record）。</p>
 */
@Component
public class NotificationFeignFallbackFactory implements FallbackFactory<NotificationFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(NotificationFeignFallbackFactory.class);

    @Override
    public NotificationFeignClient create(Throwable cause) {
        log.warn("notification-service 降级触发（通知非关键路径，静默降级）: {}", cause.getMessage());
        return new NotificationFeignClient() {
            @Override
            public ApiResponse<Void> send(NotificationSendRequest request) {
                log.warn("通知发送降级跳过: title={}", request == null ? "unknown" : request.title());
                return ApiResponse.ok(null);
            }
        };
    }
}
