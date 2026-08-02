package com.demetrius.tribunal.order.interfaces.controller;

import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.application.service.ErpStatusCallbackApplicationService;
import com.demetrius.tribunal.order.interfaces.dto.ErpStatusCallbackRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ERP 状态回传接收接口（供 erp-service Feign 调用）。
 *
 * <p>对应需求：F-308（状态回传）、N-304（回传幂等）。</p>
 */
@RestController
@RequestMapping("/api/orders")
public class ErpStatusCallbackController {

    private final ErpStatusCallbackApplicationService callbackApplicationService;

    public ErpStatusCallbackController(ErpStatusCallbackApplicationService callbackApplicationService) {
        this.callbackApplicationService = callbackApplicationService;
    }

    /**
     * 接收 ERP 履约状态回传：POST /api/orders/status-callback
     */
    @PostMapping("/status-callback")
    public ApiResponse<Void> statusCallback(@Valid @RequestBody ErpStatusCallbackRequest request) {
        callbackApplicationService.handleCallback(
                request.sourceOrderNo(), request.erpStatus(), request.erpOrderId());
        return ApiResponse.ok(null);
    }
}
