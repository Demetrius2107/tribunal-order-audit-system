package com.demetrius.tribunal.customer.domain.model;

import java.math.BigDecimal;

/**
 * 信用额度值对象。
 *
 * <p>。</p>
 *
 * <p>值对象特点：不可变、无 ID、按值相等。金额计算放在值对象内部，避免到处散落 BigDecimal 运算。</p>
 */
public record CreditLimit(BigDecimal limit, BigDecimal used) {

    public CreditLimit {
        if (limit == null || limit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("信用额度不能为负");
        }
        used = used == null ? BigDecimal.ZERO : used;
    }

    /** 可用信用 = 总额度 - 已占用 */
    public BigDecimal getAvailable() {
        return limit.subtract(used);
    }

    /** 判断可用信用是否足够支付指定金额 */
    public boolean hasEnoughFor(BigDecimal amount) {
        return getAvailable().compareTo(amount) >= 0;
    }

    /** 占用信用（下单后冻结额度，TODO：参照通用做法 */
    public CreditLimit occupy(BigDecimal amount) {
        return new CreditLimit(limit, used.add(amount));
    }

    /** 释放信用（取消/签收后释放，TODO） */
    public CreditLimit release(BigDecimal amount) {
        return new CreditLimit(limit, used.subtract(amount));
    }
}
