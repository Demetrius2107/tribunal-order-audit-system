package com.demetrius.tribunal.order.client.fallback;

import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.client.MarketingFeignClient;
import com.demetrius.tribunal.order.client.PriceQuoteResult;
import com.demetrius.tribunal.order.client.PromotionCalculateRequest;
import com.demetrius.tribunal.order.client.PromotionCalculateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * MarketingFeignClient 降级回退（M5 Resilience4j）。
 *
 * <p>营销服务可安全降级：返回零折扣/零押金默认响应，
 * 下单流程仍可通过本地 DepositCalculator 计算押金。</p>
 */
@Component
public class MarketingFeignFallbackFactory implements FallbackFactory<MarketingFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(MarketingFeignFallbackFactory.class);

    @Override
    public MarketingFeignClient create(Throwable cause) {
        log.warn("marketing-service 降级触发，返回零折扣默认响应: {}", cause.getMessage());
        return new MarketingFeignClient() {
            @Override
            public ApiResponse<PriceQuoteResult> quotePrice(String skuCode, String customerCode,
                                                            String customerGroupId, String areaCode) {
                return ApiResponse.ok(new PriceQuoteResult(skuCode, BigDecimal.ZERO, "CNY"));
            }

            @Override
            public ApiResponse<PromotionCalculateResponse> calculate(PromotionCalculateRequest request) {
                return ApiResponse.ok(new PromotionCalculateResponse(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        List.of(), List.of(), Map.of(), Map.of()));
            }
        };
    }
}
