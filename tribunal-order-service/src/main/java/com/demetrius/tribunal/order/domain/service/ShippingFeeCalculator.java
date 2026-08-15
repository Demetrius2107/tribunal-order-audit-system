package com.demetrius.tribunal.order.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 运费计算领域服务（F-103：按送货地址/SKU 计算，参与金额汇总）。
 *
 * <p>简化规则（个人项目边界，可后续扩展为重量/地区/时效阶梯）：</p>
 * <ul>
 *   <li>满额免邮：商品总额 ≥ 阈值（默认 500）→ 运费 0</li>
 *   <li>未满额：基础运费（默认 10）+ 每件加价（默认 2），封顶（默认 50）</li>
 * </ul>
 */
public class ShippingFeeCalculator {

    /** 满额免邮阈值 */
    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500.00");

    /** 基础运费 */
    public static final BigDecimal BASE_FEE = new BigDecimal("10.00");

    /** 每件商品加价 */
    public static final BigDecimal PER_ITEM_FEE = new BigDecimal("2.00");

    /** 运费封顶 */
    public static final BigDecimal MAX_FEE = new BigDecimal("50.00");

    /**
     * 计算运费。
     *
     * @param itemCount SKU 明细件数
     * @param totalAmount 商品总额
     * @return 运费金额
     */
    public BigDecimal calculate(int itemCount, BigDecimal totalAmount) {
        BigDecimal total = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        if (itemCount <= 0 || total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (total.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal fee = BASE_FEE.add(PER_ITEM_FEE.multiply(BigDecimal.valueOf(itemCount)));
        return fee.min(MAX_FEE).setScale(2, RoundingMode.HALF_UP);
    }
}
