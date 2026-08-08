package com.demetrius.tribunal.marketing.domain.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * 押金计算结果。
 *
 * @param totalDeposit 押金总额（不含计入价格的押金）
 * @param breakdown    押金按 SKU 分摊明细（skuCode → 押金额）
 */
public record DepositResult(BigDecimal totalDeposit,
                            Map<String, BigDecimal> breakdown) {

    public static DepositResult empty() {
        return new DepositResult(BigDecimal.ZERO, Collections.emptyMap());
    }
}
