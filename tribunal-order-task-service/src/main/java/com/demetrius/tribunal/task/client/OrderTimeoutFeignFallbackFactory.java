package com.demetrius.tribunal.task.client;

import com.demetrius.tribunal.common.dto.TimeoutCloseResult;
import com.demetrius.tribunal.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OrderTimeoutFeignClient 降级回退（M5 Resilience4j）。
 *
 * <p>超时关单为定时任务调度：order-service 不可用时返回空结果（关闭 0 单），
 * 任务仍正常记录 TaskLog，由下次调度/对账任务兜底，不因下游故障中断调度。</p>
 */
@Component
public class OrderTimeoutFeignFallbackFactory implements FallbackFactory<OrderTimeoutFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutFeignFallbackFactory.class);

    @Override
    public OrderTimeoutFeignClient create(Throwable cause) {
        log.warn("order-service 降级触发（超时关单调度降级，返回关闭 0 单）: {}", cause.getMessage());
        return new OrderTimeoutFeignClient() {
            @Override
            public ApiResponse<TimeoutCloseResult> timeoutClose(int minutes) {
                return ApiResponse.ok(TimeoutCloseResult.of(0, List.of()));
            }
        };
    }
}
