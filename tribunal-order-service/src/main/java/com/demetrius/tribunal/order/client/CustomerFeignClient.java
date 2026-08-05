package com.demetrius.tribunal.order.client;

import com.demetrius.tribunal.common.dto.CustomerCreditDto;
import com.demetrius.tribunal.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

/**
 * customer-service 的 Feign 客户端（跨服务调用）。
 *
 * <p>审单时通过该客户端远程获取客户信用信息，在 order-service 本地做「可用信用 ≥ 应付金额」校验；
 * 下单占用信用、审单拒绝/取消释放信用均走 customer-service 接口（信用是 customer 领域的动作，F-403）。
 * 服务发现（Nacos/Eureka）是微服务进阶内容，骨架先用 url 直连（本地多服务联调）。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>接入 Nacos 注册中心：@FeignClient 只写 name，由注册中心路由（参照通用做法</li>
 *   <li>思考：跨服务失败处理（熔断/降级/超时）——骨架未引入 Sentinel/Resilience4j</li>
 * </ul>
 */
@FeignClient(name = "tribunal-order-customer-service",
        url = "${customer.service.url:http://localhost:8081}")
public interface CustomerFeignClient {

    /**
     * 查询客户信用（对应 customer-service 的 GET /api/customers/{id}/credit）。
     */
    @GetMapping("/api/customers/{id}/credit")
    CustomerCreditDto getCustomerCredit(@PathVariable("id") String customerId);

    /**
     * 占用信用（对应 customer-service 的 POST /api/customers/{id}/credit/occupy，下单即冻结，F-403/N-301）。
     */
    @PostMapping("/api/customers/{id}/credit/occupy")
    ApiResponse<CustomerCreditDto> occupyCredit(@PathVariable("id") String customerId,
                                                @RequestBody CreditOperationRequest request);

    /**
     * 释放信用（对应 customer-service 的 POST /api/customers/{id}/credit/release，审单拒绝/取消释放，F-403）。
     */
    @PostMapping("/api/customers/{id}/credit/release")
    ApiResponse<CustomerCreditDto> releaseCredit(@PathVariable("id") String customerId,
                                                 @RequestBody CreditOperationRequest request);

    /**
     * 信用操作请求体（金额）。
     */
    record CreditOperationRequest(BigDecimal amount) {
    }
}
