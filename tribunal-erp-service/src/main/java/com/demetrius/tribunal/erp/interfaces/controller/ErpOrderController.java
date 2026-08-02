package com.demetrius.tribunal.erp.interfaces.controller;

import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.erp.application.dto.ErpOrderReceiveCommand;
import com.demetrius.tribunal.erp.application.dto.ErpOrderResult;
import com.demetrius.tribunal.erp.application.service.ErpOrderApplicationService;
import com.demetrius.tribunal.erp.interfaces.dto.ErpOrderReceiveRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ERP 履约接口层（REST 控制器，供 OMS 跨服务调用 + ERP 内部履约操作）。
 *
 * <p>对应需求：F-307（接收转单）、F-503（发货/签收回传）。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>履约操作鉴权（ERP 内部操作与 OMS 调用分离）</li>
 *   <li>部分发货/部分签收接口</li>
 *   <li>履约列表分页查询</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/erp/orders")
public class ErpOrderController {

    private final ErpOrderApplicationService erpOrderApplicationService;

    public ErpOrderController(ErpOrderApplicationService erpOrderApplicationService) {
        this.erpOrderApplicationService = erpOrderApplicationService;
    }

    /**
     * 接收 OMS 转单：POST /api/erp/orders
     */
    @PostMapping
    public ApiResponse<ErpOrderResult> receive(@Valid @RequestBody ErpOrderReceiveRequest request) {
        ErpOrderReceiveCommand command = new ErpOrderReceiveCommand(
                request.sourceOrderNo(),
                request.customerId(),
                request.lines().stream()
                        .map(l -> new ErpOrderReceiveCommand.ErpOrderLineItem(
                                l.skuCode(), l.skuName(), l.quantity(), l.price()))
                        .toList());
        return ApiResponse.ok(erpOrderApplicationService.receiveOrder(command));
    }

    /**
     * 查询履约单：GET /api/erp/orders/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<ErpOrderResult> get(@PathVariable String id) {
        return ApiResponse.ok(erpOrderApplicationService.getOrder(id));
    }

    /**
     * 发货：POST /api/erp/orders/{id}/ship
     */
    @PostMapping("/{id}/ship")
    public ApiResponse<ErpOrderResult> ship(@PathVariable String id) {
        return ApiResponse.ok(erpOrderApplicationService.ship(id));
    }

    /**
     * 签收：POST /api/erp/orders/{id}/sign
     */
    @PostMapping("/{id}/sign")
    public ApiResponse<ErpOrderResult> sign(@PathVariable String id) {
        return ApiResponse.ok(erpOrderApplicationService.sign(id));
    }

    /**
     * 关闭：POST /api/erp/orders/{id}/close
     */
    @PostMapping("/{id}/close")
    public ApiResponse<ErpOrderResult> close(@PathVariable String id) {
        return ApiResponse.ok(erpOrderApplicationService.close(id));
    }

    /**
     * 取消：POST /api/erp/orders/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ApiResponse<ErpOrderResult> cancel(@PathVariable String id) {
        return ApiResponse.ok(erpOrderApplicationService.cancel(id));
    }

    /**
     * 心跳接口（运维探活）。
     */
    @GetMapping("/heartbeat")
    public ApiResponse<String> heartbeat() {
        return ApiResponse.ok("UP");
    }
}
