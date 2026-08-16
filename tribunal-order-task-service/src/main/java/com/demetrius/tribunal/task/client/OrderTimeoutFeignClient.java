package com.demetrius.tribunal.task.client;

import com.demetrius.tribunal.common.dto.TimeoutCloseResult;
import com.demetrius.tribunal.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * order-service 的 Feign 客户端（超时关单调度）。
 *
 * <p>用途：task-service 定时扫描超时订单，调用 order-service 的超时关单接口
 * （数据在 order-service，业务逻辑由订单侧执行并保证幂等）。</p>
 *
 * <p>M5 熔断/降级：order-service 不可用时 fallback 返回关闭 0 单，
 * 任务正常记录 TaskLog，由下次调度/对账任务兜底。</p>
 */
@FeignClient(name = "tribunal-order-service",
        fallbackFactory = OrderTimeoutFeignFallbackFactory.class)
public interface OrderTimeoutFeignClient {

    /**
     * 超时关单：POST /api/orders/timeout-close?minutes=30
     */
    @PostMapping("/api/orders/timeout-close")
    ApiResponse<TimeoutCloseResult> timeoutClose(@RequestParam("minutes") int minutes);
}
