package com.demetrius.tribunal.order.interfaces.controller;

import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.application.service.BillStatusCallbackApplicationService;
import com.demetrius.tribunal.order.interfaces.dto.BillStatusCallbackRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账单状态回传接收接口（供 billing-service Feign 调用）。
 *
 * <p>对应需求：F-308（状态回传）、N-304（回传幂等）。</p>
 */
@RestController
@RequestMapping("/api/orders")
public class BillStatusCallbackController {

    private final BillStatusCallbackApplicationService callbackApplicationService;

    public BillStatusCallbackController(BillStatusCallbackApplicationService callbackApplicationService) {
        this.callbackApplicationService = callbackApplicationService;
    }

    /**
     * 接收账单状态回传：POST /api/orders/status-callback
     */
    @PostMapping("/status-callback")
    public ApiResponse<Void> statusCallback(@Valid @RequestBody BillStatusCallbackRequest request) {
        callbackApplicationService.handleCallback(
                request.sourceOrderNo(), request.billStatus(), request.billId());
        return ApiResponse.ok(null);
    }
}
