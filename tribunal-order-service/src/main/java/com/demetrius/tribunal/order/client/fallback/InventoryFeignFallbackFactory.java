package com.demetrius.tribunal.order.client.fallback;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.client.InventoryFeignClient;
import com.demetrius.tribunal.order.client.InventoryItemResult;
import com.demetrius.tribunal.order.client.WarehouseStockResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * InventoryFeignClient 降级回退（M5 Resilience4j）。
 *
 * <p>库存操作是强一致要求（预占不足会导致超卖），不可静默降级成功——
 * 全部抛 {@link BizException} 让调用方明确失败并走补偿/对账链路。</p>
 */
@Component
public class InventoryFeignFallbackFactory implements FallbackFactory<InventoryFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(InventoryFeignFallbackFactory.class);

    @Override
    public InventoryFeignClient create(Throwable cause) {
        log.error("inventory-service 降级触发（库存强一致不可降级，抛出异常）: {}", cause.getMessage());
        return new InventoryFeignClient() {
            @Override
            public ApiResponse<InventoryItemResult> getBySkuCode(String skuCode) {
                throw new BizException("503004", "库存服务不可用: " + skuCode);
            }

            @Override
            public ApiResponse<InventoryItemResult> reserve(String skuCode, BigDecimal quantity) {
                throw new BizException("503005", "库存服务不可用，预占失败: " + skuCode);
            }

            @Override
            public ApiResponse<InventoryItemResult> release(String skuCode, BigDecimal quantity) {
                throw new BizException("503006", "库存服务不可用，释放失败: " + skuCode);
            }

            @Override
            public ApiResponse<InventoryItemResult> returnStock(String skuCode, BigDecimal quantity) {
                throw new BizException("503007", "库存服务不可用，退货入库失败: " + skuCode);
            }

            @Override
            public ApiResponse<List<WarehouseStockResult>> getWarehouseStock(String skuCodes) {
                throw new BizException("503008", "库存服务不可用，仓库库存查询失败: " + skuCodes);
            }
        };
    }
}
