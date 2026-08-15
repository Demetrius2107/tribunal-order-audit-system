package com.demetrius.tribunal.order.interfaces.controller;

import com.demetrius.tribunal.common.auth.RequirePermission;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.application.dto.CarPoolGroupResult;
import com.demetrius.tribunal.order.application.service.CarPoolApplicationService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

/**
 * 拼车接口层（F-310：多订单合并一车运输）。
 *
 * <p>提供拼车组全流程 REST API：</p>
 * <ul>
 *   <li>POST   /api/car-pools              - 发起拼车组</li>
 *   <li>POST   /api/car-pools/{groupNo}/join - 加入拼车组</li>
 *   <li>POST   /api/car-pools/{groupNo}/confirm - 确认拼车（成员锁定）</li>
 *   <li>POST   /api/car-pools/{groupNo}/close - 关闭拼车组</li>
 *   <li>POST   /api/car-pools/{groupNo}/cancel - 取消拼车组</li>
 *   <li>GET    /api/car-pools/{groupNo}    - 查询拼车组</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/car-pools")
public class CarPoolController {

    private final CarPoolApplicationService carPoolApplicationService;

    public CarPoolController(CarPoolApplicationService carPoolApplicationService) {
        this.carPoolApplicationService = carPoolApplicationService;
    }

    /**
     * 发起拼车组：POST /api/car-pools（需权限 carpool:create）
     */
    @PostMapping
    @RequirePermission("carpool:create")
    public ApiResponse<CarPoolGroupResult> create() {
        return ApiResponse.ok(carPoolApplicationService.createGroup());
    }

    /**
     * 加入拼车组：POST /api/car-pools/{groupNo}/join（需权限 carpool:operate）
     */
    @PostMapping("/{groupNo}/join")
    @RequirePermission("carpool:operate")
    public ApiResponse<CarPoolGroupResult> join(@PathVariable String groupNo,
                                                @RequestBody JoinRequest request) {
        return ApiResponse.ok(carPoolApplicationService.join(groupNo, request.orderNo()));
    }

    /**
     * 确认拼车：POST /api/car-pools/{groupNo}/confirm（需权限 carpool:operate）
     */
    @PostMapping("/{groupNo}/confirm")
    @RequirePermission("carpool:operate")
    public ApiResponse<CarPoolGroupResult> confirm(@PathVariable String groupNo) {
        return ApiResponse.ok(carPoolApplicationService.confirm(groupNo));
    }

    /**
     * 关闭拼车组：POST /api/car-pools/{groupNo}/close（需权限 carpool:operate）
     */
    @PostMapping("/{groupNo}/close")
    @RequirePermission("carpool:operate")
    public ApiResponse<CarPoolGroupResult> close(@PathVariable String groupNo) {
        return ApiResponse.ok(carPoolApplicationService.close(groupNo));
    }

    /**
     * 取消拼车组：POST /api/car-pools/{groupNo}/cancel（需权限 carpool:operate）
     */
    @PostMapping("/{groupNo}/cancel")
    @RequirePermission("carpool:operate")
    public ApiResponse<CarPoolGroupResult> cancel(@PathVariable String groupNo) {
        return ApiResponse.ok(carPoolApplicationService.cancel(groupNo));
    }

    /**
     * 查询拼车组：GET /api/car-pools/{groupNo}（需权限 carpool:view）
     */
    @GetMapping("/{groupNo}")
    @RequirePermission("carpool:view")
    public ApiResponse<CarPoolGroupResult> get(@PathVariable String groupNo) {
        return ApiResponse.ok(carPoolApplicationService.getGroup(groupNo));
    }

    /** 加入拼车组请求体。 */
    public record JoinRequest(
            @NotBlank String orderNo) {
    }
}
