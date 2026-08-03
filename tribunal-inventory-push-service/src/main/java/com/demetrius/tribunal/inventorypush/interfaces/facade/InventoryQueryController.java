package com.demetrius.tribunal.inventorypush.interfaces.facade;

import com.demetrius.tribunal.inventorypush.application.service.InventoryQueryApplicationService;
import com.demetrius.tribunal.inventorypush.common.dto.InventorySkuView;
import com.demetrius.tribunal.inventorypush.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存查询接口（下游被动查询，PRD 4.3 GET /api/v1/inventory/query）。
 */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryQueryController {

    private final InventoryQueryApplicationService queryApplicationService;

    public InventoryQueryController(InventoryQueryApplicationService queryApplicationService) {
        this.queryApplicationService = queryApplicationService;
    }

    /**
     * 查询 SKU 实时库存（skuId + warehouseId + ownerId 维度）。
     */
    @GetMapping("/query")
    public ApiResponse<InventorySkuView> query(@RequestParam String skuId,
                                               @RequestParam String warehouseId,
                                               @RequestParam(required = false, defaultValue = "OWNER_SELF") String ownerId) {
        return ApiResponse.ok(queryApplicationService.query(skuId, warehouseId, ownerId));
    }

    /**
     * 心跳接口（运维探活）。
     */
    @GetMapping("/heartbeat")
    public ApiResponse<String> heartbeat() {
        return ApiResponse.ok("UP");
    }
}
