package com.demetrius.tribunal.marketing.domain.model;

import java.math.BigDecimal;

/**
 * 价格规则聚合根。
 *
 * <p>对应需求：F-102（价格体系：客户价/客户组价/区域价，优先级取价）。</p>
 *
 * <p>价格来源为上游推送的主数据，本模块提供取价服务（按客户→客户组→区域优先级）。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>价格有效期（生效/失效时间）</li>
 *   <li>价格变动流水与历史版本</li>
 *   <li>渠道价映射（渠道 → 价格档）</li>
 * </ul>
 */
public class PriceRule {

    private final PriceRuleId id;

    private final String skuCode;

    /** 价格档位：CUSTOMER（客户价）/ CUSTOMER_GROUP（客户组价）/ AREA（区域价） */
    private final String priceLevel;

    /** 价格对象编码（客户编码/客户组编码/区域编码） */
    private final String priceTarget;

    private final BigDecimal price;

    private final String currency;

    public PriceRule(PriceRuleId id, String skuCode, String priceLevel,
                     String priceTarget, BigDecimal price, String currency) {
        if (skuCode == null || skuCode.isBlank()) {
            throw new IllegalArgumentException("SKU编码不能为空");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("价格不能为负");
        }
        this.id = id;
        this.skuCode = skuCode;
        this.priceLevel = priceLevel;
        this.priceTarget = priceTarget;
        this.price = price;
        this.currency = currency;
    }

    public PriceRuleId getId() {
        return id;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public String getPriceLevel() {
        return priceLevel;
    }

    public String getPriceTarget() {
        return priceTarget;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }
}
