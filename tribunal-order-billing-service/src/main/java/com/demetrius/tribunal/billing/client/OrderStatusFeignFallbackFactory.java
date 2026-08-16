package com.demetrius.tribunal.billing.client;

import com.demetrius.tribunal.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * OrderStatusFeignClient 降级回退（M5 Resilience4j）。
 *
 * <p>状态回传是订单状态机推进的关键链路（F-308），不可静默降级——失败抛异常，
 * 由 Kafka 异步链路/对账任务兜底，保证订单状态不回退。</p>
 */
@Component
public class OrderStatusFeignFallbackFactory implements FallbackFactory<OrderStatusFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusFeignFallbackFactory.class);

    @Override
    public OrderStatusFeignClient create(Throwable cause) {
        log.error("order-service 降级触发（状态回传不可降级，抛出异常）: {}", cause.getMessage());
        return new OrderStatusFeignClient() {
            @Override
            public void statusCallback(OrderStatusCallbackRequest request) {
                throw new BizException("503010",
                        "订单服务不可用，状态回传失败: " + (request == null ? "unknown" : request.sourceOrderNo()));
            }
        };
    }
}
