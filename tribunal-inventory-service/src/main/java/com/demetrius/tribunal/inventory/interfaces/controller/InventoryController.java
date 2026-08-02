package com.demetrius.tribunal.inventory.interfaces.controller;

import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.inventory.application.dto.InventoryItemResult;
import com.demetrius.tribunal.inventory.application.service.InventoryApplicationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 库存物料接口层（REST 控制器，供订单服务 Feign 调用 + 主数据维护）。
 *
 * <p>对应需求：F-501（库存查询）、F-502（库存预占/释放）。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>物料分页列表查询</li>
 *   <li>推送上游接口（物料/库存变化主动推送订单服务）</li>
 *   <li>库存变动流水查询</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryApplicationService inventoryApplicationService;

    public InventoryController(InventoryApplicationService inventoryApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
    }

    /**
     * 库存查询：GET /api/inventory/items/{skuCode}
     */
    @GetMapping("/items/{skuCode}")
    public ApiResponse<InventoryItemResult> getBySkuCode(@PathVariable String skuCode) {
        return ApiResponse.ok(InventoryItemResult.from(inventoryApplicationService.getBySkuCode(skuCode)));
    }

    /**
     * 预占库存：POST /api/inventory/items/{skuCode}/reserve?quantity=10
     */
    @PostMapping("/items/{skuCode}/reserve")
    public ApiResponse<InventoryItemResult> reserve(@PathVariable String skuCode,
                                                    @RequestParam @NotNull BigDecimal quantity) {
        return ApiResponse.ok(InventoryItemResult.from(inventoryApplicationService.reserve(skuCode, quantity)));
    }

    /**
     * 释放预占：POST /api/inventory/items/{skuCode}/release?quantity=10
     */
    @PostMapping("/items/{skuCode}/release")
    public ApiResponse<InventoryItemResult> release(@PathVariable String skuCode,
                                                    @RequestParam @NotNull BigDecimal quantity) {
        return ApiResponse.ok(InventoryItemResult.from(inventoryApplicationService.release(skuCode, quantity)));
    }

    /**
     * 物料入库/库存更新：POST /api/inventory/items
     */
    @PostMapping("/items")
    public ApiResponse<InventoryItemResult> upsert(@RequestBody UpsertRequest request) {
        return ApiResponse.ok(InventoryItemResult.from(inventoryApplicationService.upsert(
                request.skuCode(), request.skuName(), request.unit(), request.totalQuantity())));
    }

    /**
     * 心跳接口（运维探活）。
     */
    @GetMapping("/heartbeat")
    public ApiResponse<String> heartbeat() {
        return ApiResponse.ok("UP");
    }

    /** 入库请求体（TODO：抽到 interfaces/dto 包） */
    public record UpsertRequest(
            @NotBlank String skuCode,
            String skuName,
            String unit,
            @NotNull BigDecimal totalQuantity) {
    }
}
