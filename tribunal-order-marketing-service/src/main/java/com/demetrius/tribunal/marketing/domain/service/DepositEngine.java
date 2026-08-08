package com.demetrius.tribunal.marketing.domain.service;

import com.demetrius.tribunal.marketing.domain.model.DepositResult;
import com.demetrius.tribunal.marketing.domain.model.DepositRule;
import com.demetrius.tribunal.marketing.domain.model.SkuItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 押金计算引擎（M4 领域服务）。
 *
 * <p>对每条订单明细，按 SKU 匹配押金规则，计算需额外加收的押金总额
 * （{@code includedInPrice} = false 的规则才参与加收）。</p>
 *
 * <p>押金参与订单应付金额汇总：{@code payableAmount += totalDeposit}。</p>
 */
public class DepositEngine {

    /**
     * 计算押金。
     *
     * @param skus  订单明细快照
     * @param rules 押金规则列表（按 skuCode 索引）
     * @return 押金结果（总额 + 按 SKU 明细）
     */
    public DepositResult calculate(List<SkuItem> skus, List<DepositRule> rules) {
        if (skus == null || skus.isEmpty() || rules == null || rules.isEmpty()) {
            return DepositResult.empty();
        }
        // 按 skuCode 分组取第一条 active 规则（一个 SKU 原则上只配一种包装押金）
        Map<String, DepositRule> ruleIndex = rules.stream()
                .filter(DepositRule::isActive)
                .collect(Collectors.toMap(
                        DepositRule::getSkuCode,
                        Function.identity(),
                        (a, b) -> a));

        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();

        for (SkuItem sku : skus) {
            DepositRule rule = ruleIndex.get(sku.skuCode());
            if (rule == null) {
                continue;
            }
            if (rule.isIncludedInPrice()) {
                // 押金已含在售价中，不再额外加收
                continue;
            }
            BigDecimal deposit = rule.getUnitDeposit()
                    .multiply(sku.quantity())
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(deposit);
            breakdown.merge(sku.skuCode(), deposit, BigDecimal::add);
        }

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return DepositResult.empty();
        }
        return new DepositResult(total, Map.copyOf(breakdown));
    }
}
