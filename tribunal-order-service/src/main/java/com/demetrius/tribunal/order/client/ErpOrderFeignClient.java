package com.demetrius.tribunal.order.client;

import com.demetrius.tribunal.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * erp-service 的 Feign 客户端（转单调用）。
 *
 * <p>对应需求：F-307（转单）、F-308（状态回传）。</p>
 *
 * <p>用途：审单通过后，把订单数据转给 ERP 创建履约单。
 * ERP 履约状态变更后通过回传接口驱动本地订单状态机。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>转单失败处理：重试 + 记录失败表（OrderTransferFail 思路）</li>
 *   <li>接入 Nacos 后去掉 url 直连，用服务名路由</li>
 * </ul>
 */
@FeignClient(name = "tribunal-erp-service",
        url = "${erp.service.url:http://localhost:8082}")
public interface ErpOrderFeignClient {

    /**
     * 转单：调用 ERP 的 POST /api/erp/orders 接收履约单。
     */
    @PostMapping("/api/erp/orders")
    ApiResponse<ErpTransferResult> transfer(@RequestBody ErpTransferRequest request);
}
