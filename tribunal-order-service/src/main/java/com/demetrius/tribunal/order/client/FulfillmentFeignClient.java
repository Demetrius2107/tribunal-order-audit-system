package com.demetrius.tribunal.order.client;

import com.demetrius.tribunal.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;

/**
 * 履约执行服务（fulfillment-service）的 Feign 客户端。
 *
 * <p>对应需求：下游履约执行（出库/发货/签收/发送工厂）。</p>
 *
 * <p>用途：账单结算后创建履约单，执行发货/签收并发送工厂指令。</p>
 */
@FeignClient(name = "tribunal-order-fulfillment-service")
public interface FulfillmentFeignClient {

    /**
     * 创建履约单：POST /api/fulfillments
     */
    @PostMapping("/api/fulfillments")
    ApiResponse<FulfillmentResult> create(@RequestBody FulfillmentCreateRequest request);

    /**
     * 创建履约请求体。
     */
    record FulfillmentCreateRequest(
            String sourceOrderNo,
            String customerId,
            List<FulfillmentLineItem> lines) {

        public record FulfillmentLineItem(
                String skuCode,
                String skuName,
                BigDecimal quantity,
                BigDecimal price) {
        }
    }
}
