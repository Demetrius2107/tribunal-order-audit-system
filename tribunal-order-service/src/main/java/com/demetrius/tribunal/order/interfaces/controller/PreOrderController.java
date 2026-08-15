package com.demetrius.tribunal.order.interfaces.controller;

import com.demetrius.tribunal.common.auth.RequirePermission;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.application.dto.PreOrderActivityResult;
import com.demetrius.tribunal.order.application.dto.PreOrderRecordResult;
import com.demetrius.tribunal.order.application.service.PreOrderApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预购接口层（F-312：提前采购，经销商预付/保证金模式）。
 *
 * <p>提供预购活动全流程 REST API：</p>
 * <ul>
 *   <li>POST   /api/pre-orders/activities - 创建预购活动（草稿）</li>
 *   <li>POST   /api/pre-orders/activities/{activityNo}/activate - 上线（可参与）</li>
 *   <li>POST   /api/pre-orders/activities/{activityNo}/end - 结束</li>
 *   <li>POST   /api/pre-orders/activities/{activityNo}/cancel - 取消</li>
 *   <li>GET    /api/pre-orders/activities/{activityNo} - 查询活动</li>
 *   <li>GET    /api/pre-orders/records?activityNo=&orderNo= - 查询预购订单记录</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/pre-orders")
public class PreOrderController {

    private final PreOrderApplicationService preOrderApplicationService;

    public PreOrderController(PreOrderApplicationService preOrderApplicationService) {
        this.preOrderApplicationService = preOrderApplicationService;
    }

    /**
     * 创建预购活动：POST /api/pre-orders/activities（需权限 preorder:create）
     */
    @PostMapping("/activities")
    @RequirePermission("preorder:create")
    public ApiResponse<PreOrderActivityResult> create(@Valid @RequestBody CreateActivityRequest request) {
        return ApiResponse.ok(preOrderApplicationService.createActivity(
                request.name(),
                request.skuCodes(),
                request.depositRate(),
                request.discountRate(),
                request.startTime(),
                request.endTime()));
    }

    /**
     * 上线预购活动：POST /api/pre-orders/activities/{activityNo}/activate（需权限 preorder:operate）
     */
    @PostMapping("/activities/{activityNo}/activate")
    @RequirePermission("preorder:operate")
    public ApiResponse<PreOrderActivityResult> activate(@PathVariable String activityNo) {
        return ApiResponse.ok(preOrderApplicationService.activate(activityNo));
    }

    /**
     * 结束预购活动：POST /api/pre-orders/activities/{activityNo}/end（需权限 preorder:operate）
     */
    @PostMapping("/activities/{activityNo}/end")
    @RequirePermission("preorder:operate")
    public ApiResponse<PreOrderActivityResult> end(@PathVariable String activityNo) {
        return ApiResponse.ok(preOrderApplicationService.end(activityNo));
    }

    /**
     * 取消预购活动：POST /api/pre-orders/activities/{activityNo}/cancel（需权限 preorder:operate）
     */
    @PostMapping("/activities/{activityNo}/cancel")
    @RequirePermission("preorder:operate")
    public ApiResponse<PreOrderActivityResult> cancel(@PathVariable String activityNo) {
        return ApiResponse.ok(preOrderApplicationService.cancel(activityNo));
    }

    /**
     * 查询预购活动：GET /api/pre-orders/activities/{activityNo}（需权限 preorder:view）
     */
    @GetMapping("/activities/{activityNo}")
    @RequirePermission("preorder:view")
    public ApiResponse<PreOrderActivityResult> getActivity(@PathVariable String activityNo) {
        return ApiResponse.ok(preOrderApplicationService.getActivity(activityNo));
    }

    /**
     * 查询预购订单记录：GET /api/pre-orders/records?activityNo=&orderNo=（需权限 preorder:view）
     */
    @GetMapping("/records")
    @RequirePermission("preorder:view")
    public ApiResponse<PreOrderRecordResult> getRecord(@RequestParam String activityNo,
                                                       @RequestParam String orderNo) {
        return ApiResponse.ok(preOrderApplicationService.getRecord(activityNo, orderNo));
    }

    /** 创建预购活动请求体。 */
    public record CreateActivityRequest(
            @NotBlank String name,
            List<String> skuCodes,
            @NotNull BigDecimal depositRate,
            @NotNull BigDecimal discountRate,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime) {
    }
}
