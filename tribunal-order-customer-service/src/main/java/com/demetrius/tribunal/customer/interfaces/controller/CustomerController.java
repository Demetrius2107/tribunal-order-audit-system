package com.demetrius.tribunal.customer.interfaces.controller;

import com.demetrius.tribunal.common.dto.CustomerCreditDto;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.customer.application.service.CustomerApplicationService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 客户接口层（REST 控制器）。
 *
 * <p>供 order-service 通过 Feign 跨服务调用：查询信用 / 占用信用 / 释放信用。</p>
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
     * 占用信用（order-service 下单时调用，对应 F-403/N-301）。
     */
    @PostMapping("/{id}/credit/occupy")
    public ApiResponse<CustomerCreditDto> occupyCredit(@PathVariable String id,
                                                       @RequestBody CreditOperationRequest request) {
        return ApiResponse.ok(customerApplicationService.occupyCredit(id, request.amount()));
    }

    /**
     * 释放信用（order-service 审单拒绝/订单取消时调用，对应 F-403）。
     */
    @PostMapping("/{id}/credit/release")
    public ApiResponse<CustomerCreditDto> releaseCredit(@PathVariable String id,
                                                        @RequestBody CreditOperationRequest request) {
        return ApiResponse.ok(customerApplicationService.releaseCredit(id, request.amount()));
    }

    /**
     * 信用操作请求体。
     */
    public record CreditOperationRequest(
            @NotNull(message = "金额不能为空")
            @DecimalMin(value = "0.01", message = "金额必须大于0")
            BigDecimal amount) {
    }

    /**
     * 心跳接口（运维探活）。
     */
    @GetMapping("/heartbeat")
    public ApiResponse<String> heartbeat() {
        return ApiResponse.ok("UP");
    }
}
