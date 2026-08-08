package com.demetrius.tribunal.order.client;

import com.demetrius.tribunal.common.response.ApiResponse;
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
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>预占失败处理：库存不足拒绝/降级（是否允许无库存下单由业务规则决定）</li>
 *   <li>接入 Nacos 后去掉 url 直连</li>
 * </ul>
 */
@FeignClient(name = "tribunal-order-inventory-service",
        url = "${inventory.service.url:http://localhost:8083}")
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
