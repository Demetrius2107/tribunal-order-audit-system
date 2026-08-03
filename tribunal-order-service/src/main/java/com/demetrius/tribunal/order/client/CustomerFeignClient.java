package com.demetrius.tribunal.order.client;

import com.demetrius.tribunal.common.dto.CustomerCreditDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * customer-service 的 Feign 客户端（跨服务调用）。
 *
 * <p>审单时通过该客户端远程获取客户信用信息，在 order-service 本地做「可用信用 ≥ 应付金额」校验。
 * 服务发现（Nacos/Eureka）是微服务进阶内容，骨架先用 url 直连（本地双服务联调）。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>接入 Nacos 注册中心：@FeignClient 只写 name，由注册中心路由（参照通用做法</li>
 *   <li>补充信用占用/释放接口（POST /credit/occupy），审单通过后远程扣减信用</li>
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
}
