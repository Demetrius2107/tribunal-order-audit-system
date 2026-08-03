package com.demetrius.tribunal.fulfillment.interfaces.controller;

import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.fulfillment.application.dto.FulfillmentReceiveCommand;
import com.demetrius.tribunal.fulfillment.application.dto.FulfillmentResult;
import com.demetrius.tribunal.fulfillment.application.service.FulfillmentApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 履约执行接口层（REST，供订单/账单服务触发 + 履约内部操作）。
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>发送工厂指令接口</li>
 *   <li>履约列表分页查询</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/fulfillments")
public class FulfillmentController {

    private final FulfillmentApplicationService fulfillmentApplicationService;

    public FulfillmentController(FulfillmentApplicationService fulfillmentApplicationService) {
        this.fulfillmentApplicationService = fulfillmentApplicationService;
    }

    /**
     * 创建履约单：POST /api/fulfillments
     */
    @PostMapping
    public ApiResponse<FulfillmentResult> create(@Valid @RequestBody FulfillmentReceiveCommand command) {
        return ApiResponse.ok(fulfillmentApplicationService.create(command));
    }

    /**
     * 查询履约单：GET /api/fulfillments/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<FulfillmentResult> get(@PathVariable String id) {
        return ApiResponse.ok(fulfillmentApplicationService.get(id));
    }

    /**
     * 发货：POST /api/fulfillments/{id}/ship
     */
    @PostMapping("/{id}/ship")
    public ApiResponse<FulfillmentResult> ship(@PathVariable String id) {
        return ApiResponse.ok(fulfillmentApplicationService.ship(id));
    }

    /**
     * 签收：POST /api/fulfillments/{id}/sign
     */
    @PostMapping("/{id}/sign")
    public ApiResponse<FulfillmentResult> sign(@PathVariable String id) {
        return ApiResponse.ok(fulfillmentApplicationService.sign(id));
    }

    /**
     * 取消：POST /api/fulfillments/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ApiResponse<FulfillmentResult> cancel(@PathVariable String id) {
        return ApiResponse.ok(fulfillmentApplicationService.cancel(id));
    }

    /**
     * 心跳接口（运维探活）。
     */
    @GetMapping("/heartbeat")
    public ApiResponse<String> heartbeat() {
        return ApiResponse.ok("UP");
    }
}
