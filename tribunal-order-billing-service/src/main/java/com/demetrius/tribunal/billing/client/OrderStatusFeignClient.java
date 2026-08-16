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
 * <p>M5 熔断/降级：状态回传为订单状态机推进关键链路，fallback 不静默降级，
 * 抛 {@link com.demetrius.tribunal.common.exception.BizException} 由 Kafka 异步链路/对账兜底。</p>
 */
@FeignClient(name = "tribunal-order-service",
        fallbackFactory = OrderStatusFeignFallbackFactory.class)
public interface OrderStatusFeignClient {

    /**
     * 回传履约状态：调用 订单服务 的 POST /api/orders/status-callback。
     */
    @PostMapping("/api/orders/status-callback")
    void statusCallback(@RequestBody OrderStatusCallbackRequest request);
}
