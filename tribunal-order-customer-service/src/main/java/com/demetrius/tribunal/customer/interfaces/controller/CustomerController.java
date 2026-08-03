package com.demetrius.tribunal.customer.interfaces.controller;

import com.demetrius.tribunal.common.dto.CustomerCreditDto;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.customer.application.service.CustomerApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户接口层（REST 控制器）。
 *
 * <p>供 order-service 通过 Feign 跨服务调用：GET /api/customers/{id}/credit</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>信用占用/释放接口（参照通用做法</li>
 *   <li>客户增删改查接口（参照通用做法</li>
 *   <li>全局异常处理（@RestControllerAdvice 捕获 BizException）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerApplicationService customerApplicationService;

    public CustomerController(CustomerApplicationService customerApplicationService) {
        this.customerApplicationService = customerApplicationService;
    }

    /**
     * 查询客户信用（Feign 跨服务调用入口）。
     */
    @GetMapping("/{id}/credit")
    public ApiResponse<CustomerCreditDto> getCredit(@PathVariable String id) {
        return ApiResponse.ok(customerApplicationService.getCustomerCredit(id));
    }

    /**
     * 心跳接口（运维探活，。
     */
    @GetMapping("/heartbeat")
    public ApiResponse<String> heartbeat() {
        return ApiResponse.ok("UP");
    }
}
