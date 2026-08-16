package com.demetrius.tribunal.common.dto;

import java.math.BigDecimal;

/**
 * 客户信用信息 DTO（跨服务 Feign 返回体）。
 *
 * <p>用途：order-service 审单时通过 CustomerFeignClient 调用 customer-service 获取客户信用，
 * 在本地做「可用信用 ≥ 应付金额」校验。DTO 放 common 避免 order-service 依赖 customer 领域对象。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>思考：信用扣减/释放是 customer 领域的动作，应通过 customer-service 的接口完成
 *       （如 POST /api/customers/{id}/credit/occupy），而不是在 order-service 本地改数</li>
 * </ul>
 */
public record CustomerCreditDto(
        String customerId,
        String customerCode,
        BigDecimal creditLimit,
        BigDecimal creditUsed,
        BigDecimal discountPoolBalance) {

    public CustomerCreditDto(String customerId, String customerCode,
                             BigDecimal creditLimit, BigDecimal creditUsed) {
        this(customerId, customerCode, creditLimit, creditUsed, BigDecimal.ZERO);
    }

    /** 可用信用 = 总额度 - 已占用 */
    public BigDecimal available() {
        return creditLimit.subtract(creditUsed);
    }

    /** 可用信用是否足够支付指定金额 */
    public boolean hasEnoughFor(BigDecimal amount) {
        return available().compareTo(amount) >= 0;
    }
}
