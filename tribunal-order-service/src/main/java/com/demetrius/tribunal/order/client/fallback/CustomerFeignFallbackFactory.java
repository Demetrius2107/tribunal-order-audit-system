package com.demetrius.tribunal.order.client.fallback;

import com.demetrius.tribunal.common.dto.CustomerCreditDto;
import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.order.client.CustomerFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * CustomerFeignClient 降级回退（M5 Resilience4j）。
 *
 * <p>信用操作是强一致要求，不可降级跳过，全部抛 BizException 让调用方明确失败。</p>
 */
@Component
public class CustomerFeignFallbackFactory implements FallbackFactory<CustomerFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(CustomerFeignFallbackFactory.class);

    @Override
    public CustomerFeignClient create(Throwable cause) {
        log.error("customer-service 降级触发（信用操作不可降级，抛出异常）: {}", cause.getMessage());
        return new CustomerFeignClient() {
            @Override
            public CustomerCreditDto getCustomerCredit(String customerId) {
                throw new BizException("503001", "客户服务不可用: " + customerId);
            }

            @Override
            public ApiResponse<CustomerCreditDto> occupyCredit(String customerId,
                                                               CreditOperationRequest request) {
                throw new BizException("503002", "客户服务不可用，信用占用失败: " + customerId);
            }

            @Override
            public ApiResponse<CustomerCreditDto> releaseCredit(String customerId,
                                                                CreditOperationRequest request) {
                throw new BizException("503003", "客户服务不可用，信用释放失败: " + customerId);
            }
        };
    }
}
