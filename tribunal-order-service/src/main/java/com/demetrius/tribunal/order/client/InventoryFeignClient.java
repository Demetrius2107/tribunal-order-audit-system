package com.demetrius.tribunal.order.client;

import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.client.fallback.InventoryFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存物料服务（inventory-service）的 Feign 客户端。
 *
 * <p>对应需求：F-501（库存查询）、F-502（库存预占/释放）。</p>
 *
 * <p>用途：下单/审单时校验可售量并预占库存；取消/签收时释放。</p>
 *
 * <p>M5 熔断/降级：库存为强一致操作，fallback 不静默降级成功，
 * 统一抛 {@link com.demetrius.tribunal.common.exception.BizException} 走补偿/对账链路。</p>
 */
@FeignClient(name = "tribunal-order-inventory-service",
        fallbackFactory = InventoryFeignFallbackFactory.class)
public interface InventoryFeignClient {

    /**
     * 库存查询：GET /api/inventory/items/{skuCode}
     */
    @GetMapping("/api/inventory/items/{skuCode}")
    ApiResponse<InventoryItemResult> getBySkuCode(@PathVariable("skuCode") String skuCode);

    /**
     * 预占库存：POST /api/inventory/items/{skuCode}/reserve?quantity=10
     */
    @PostMapping("/api/inventory/items/{skuCode}/reserve")
    ApiResponse<InventoryItemResult> reserve(@PathVariable("skuCode") String skuCode,
                                             @RequestParam("quantity") BigDecimal quantity);

    /**
     * 释放预占：POST /api/inventory/items/{skuCode}/release?quantity=10
     */
    @PostMapping("/api/inventory/items/{skuCode}/release")
    ApiResponse<InventoryItemResult> release(@PathVariable("skuCode") String skuCode,
                                             @RequestParam("quantity") BigDecimal quantity);

    /**
     * 退货入库：POST /api/inventory/items/{skuCode}/return?quantity=10
     */
    @PostMapping("/api/inventory/items/{skuCode}/return")
    ApiResponse<InventoryItemResult> returnStock(@PathVariable("skuCode") String skuCode,
                                                 @RequestParam("quantity") BigDecimal quantity);

    /**
     * M4：仓库级库存查询（寻源分仓用）。
     *
     * <p>GET /api/inventory/warehouses/stock?skuCodes=A,B,C</p>
     *
     * <p>返回各仓库对所查 SKU 的可用库存，供 {@code WarehouseRoutingService} 寻源匹配。</p>
     *
     * @param skuCodes 需查询的 SKU 编码集合（逗号分隔）
     * @return 仓库库存列表
     */
    @GetMapping("/api/inventory/warehouses/stock")
    ApiResponse<List<WarehouseStockResult>> getWarehouseStock(@RequestParam("skuCodes") String skuCodes);
}
