package com.demetrius.tribunal.order.interfaces.controller;

import com.demetrius.tribunal.common.auth.RequirePermission;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.application.dto.MergeOrderResult;
import com.demetrius.tribunal.order.application.service.MergeOrderApplicationService;
import com.demetrius.tribunal.order.interfaces.dto.MergeOrderCreateRequest;
import com.demetrius.tribunal.order.interfaces.dto.MergeOrderShipRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合单接口层（REST 控制器）。
 *
 * <p>提供合单全流程 REST API：</p>
 * <ul>
 *   <li>POST   /api/merge-orders              - 创建合单</li>
 *   <li>POST   /api/merge-orders/{id}/pack     - 打包</li>
 *   <li>POST   /api/merge-orders/{id}/ship     - 发货</li>
 *   <li>POST   /api/merge-orders/{id}/deliver  - 送达</li>
 *   <li>POST   /api/merge-orders/{id}/cancel   - 取消</li>
 *   <li>POST   /api/merge-orders/{id}/shipping-fee - 设置合单运费</li>
 *   <li>GET    /api/merge-orders/{id}          - 查询合单详情</li>
 *   <li>GET    /api/merge-orders?customerId=   - 按客户查询合单列表</li>
 *   <li>GET    /api/merge-orders/by-order/{orderId} - 查询某订单参与的合单</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/merge-orders")
public class MergeOrderController {

    private final MergeOrderApplicationService mergeOrderApplicationService;

    public MergeOrderController(MergeOrderApplicationService mergeOrderApplicationService) {
        this.mergeOrderApplicationService = mergeOrderApplicationService;
    }

    /**
     * 创建合单：POST /api/merge-orders（需权限 merge:create）
     */
    @PostMapping
    @RequirePermission("merge:create")
    public ApiResponse<MergeOrderResult> create(@Valid @RequestBody MergeOrderCreateRequest request) {
        return ApiResponse.ok(mergeOrderApplicationService.create(request.orderIds()));
    }

    /**
     * 打包：POST /api/merge-orders/{id}/pack（需权限 merge:operate）
     */
    @PostMapping("/{id}/pack")
    @RequirePermission("merge:operate")
    public ApiResponse<MergeOrderResult> pack(@PathVariable String id) {
        return ApiResponse.ok(mergeOrderApplicationService.pack(id));
    }

    /**
     * 发货：POST /api/merge-orders/{id}/ship（需权限 merge:operate）
     */
    @PostMapping("/{id}/ship")
    @RequirePermission("merge:operate")
    public ApiResponse<MergeOrderResult> ship(@PathVariable String id,
                                               @Valid @RequestBody MergeOrderShipRequest request) {
        return ApiResponse.ok(mergeOrderApplicationService.ship(id, request.trackingNo()));
    }

    /**
     * 送达：POST /api/merge-orders/{id}/deliver（需权限 merge:operate）
     */
    @PostMapping("/{id}/deliver")
    @RequirePermission("merge:operate")
    public ApiResponse<MergeOrderResult> deliver(@PathVariable String id) {
        return ApiResponse.ok(mergeOrderApplicationService.deliver(id));
    }

    /**
     * 取消：POST /api/merge-orders/{id}/cancel（需权限 merge:operate）
     */
    @PostMapping("/{id}/cancel")
    @RequirePermission("merge:operate")
    public ApiResponse<MergeOrderResult> cancel(@PathVariable String id) {
        return ApiResponse.ok(mergeOrderApplicationService.cancel(id));
    }

    /**
     * 设置合单运费：POST /api/merge-orders/{id}/shipping-fee?fee=15.00（需权限 merge:operate）
     */
    @PostMapping("/{id}/shipping-fee")
    @RequirePermission("merge:operate")
    public ApiResponse<MergeOrderResult> applyShippingFee(@PathVariable String id,
                                                           @RequestParam BigDecimal fee) {
        return ApiResponse.ok(mergeOrderApplicationService.applyShippingFee(id, fee));
    }

    /**
     * 查询合单详情：GET /api/merge-orders/{id}（需权限 merge:view）
     */
    @GetMapping("/{id}")
    @RequirePermission("merge:view")
    public ApiResponse<MergeOrderResult> get(@PathVariable String id) {
        return ApiResponse.ok(mergeOrderApplicationService.getById(id));
    }

    /**
     * 按客户查询合单列表：GET /api/merge-orders?customerId=xxx（需权限 merge:view）
     */
    @GetMapping
    @RequirePermission("merge:view")
    public ApiResponse<List<MergeOrderResult>> listByCustomer(@RequestParam String customerId) {
        return ApiResponse.ok(mergeOrderApplicationService.listByCustomer(customerId));
    }

    /**
     * 查询某订单参与的合单：GET /api/merge-orders/by-order/{orderId}（需权限 merge:view）
     */
    @GetMapping("/by-order/{orderId}")
    @RequirePermission("merge:view")
    public ApiResponse<MergeOrderResult> getByMemberOrder(@PathVariable String orderId) {
        return ApiResponse.ok(mergeOrderApplicationService.getByMemberOrder(orderId));
    }
}
