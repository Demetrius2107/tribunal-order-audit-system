package com.demetrius.tribunal.order.domain.service;

import com.demetrius.tribunal.common.dto.CustomerCreditDto;
import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderSku;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 审单领域服务（★业务规则集中地）。
 *
 * <p>对照旧项目：{@code SalesmanController.reviewOrder}、{@code OrderServiceImpl.orderReview}
 * （875 行审单逻辑）、信用检查（creditProcessing / checkTheAmountPayable）。</p>
 *
 * <p>为什么需要领域服务：当一条业务规则涉及多个聚合/服务（订单 + 客户信用）时，
 * 放任何一个聚合里都不合适，就放到领域服务里。</p>
 *
 * <p>微服务说明：客户信用数据在 customer-service，这里通过 common 中的
 * {@link CustomerCreditDto}（Feign 远程查询结果）做校验，order-service 不直接依赖
 * customer 领域对象——跨服务边界用 DTO 而非领域对象。</p>
 *
 * <p>TODO（学习任务）——对照旧项目逐条实现校验：</p>
 * <ul>
 *   <li>① 状态校验：由 Order 聚合内部的状态机保证（confirm() 已做）</li>
 *   <li>② 信用校验：客户可用信用 ≥ 订单应付金额，否则拒绝（对照 ErrorCode 信用不足）</li>
 *   <li>③ 整托校验：SKU 数量必须满足整托倍数（对照 wholePalletCheck，行业特有）</li>
 *   <li>④ 促销/折扣规则：审单时是否重新计算金额（对照促销计算引擎）</li>
 *   <li>⑤ 审单权限：谁有权限审单（对照旧项目权限体系）</li>
 * </ul>
 */
public class OrderReviewDomainService {

    /**
     * 审单前校验（校验通过才允许 Order.confirm() / reject()）。
     *
     * @param order    订单聚合
     * @param credit   客户信用（Feign 从 customer-service 查询）
     * @param operator 操作人（TODO：实现权限校验时使用）
     */
    public void validateForReview(Order order, CustomerCreditDto credit, String operator) {
        // 金额校验：应付金额必须大于 0
        validateAmount(order);
        // 信用校验
        BigDecimal payable = order.getPayableAmount();
        if (!credit.hasEnoughFor(payable)) {
            throw new BizException("200001",
                    "信用额度不足: 可用信用 " + credit.available() + ", 应付金额 " + payable);
        }
        // 整托校验（骨架默认空规格=不校验；真实数据来自 SKU 主数据服务，见 validateWholePallet 说明）
        validateWholePallet(order, Map.of());
        // ④ 促销折扣重算（TODO）
        // ⑤ 权限校验（TODO）
    }

    /**
     * 金额校验：订单应付金额必须大于 0。
     */
    private void validateAmount(Order order) {
        if (order.getPayableAmount() == null
                || order.getPayableAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("200002", "订单应付金额必须大于0");
        }
    }

    /**
     * 整托校验：SKU 数量必须能被该 SKU 的整托规格整除。
     *
     * <p>对照旧项目 {@code wholePalletCheck}（啤酒行业特有规则：按托盘售卖，数量必须是整托倍数）。</p>
     *
     * <p>说明：整托规格（每个 SKU 一托多少瓶）在真实系统里来自 SKU 主数据服务，
     * 这里通过 {@code palletSizeBySku} 参数传入，由应用服务负责组装
     * （TODO：后续可新增 sku-service 通过 Feign 查询）。</p>
     *
     * @param order          订单聚合
     * @param palletSizeBySku SKU编码 → 整托规格（数量），不存在的 SKU 不做整托校验
     */
    public void validateWholePallet(Order order, Map<String, BigDecimal> palletSizeBySku) {
        for (OrderSku sku : order.getSkus()) {
            BigDecimal palletSize = palletSizeBySku.get(sku.getSkuCode());
            if (palletSize == null || palletSize.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // 该 SKU 未配置整托规格，跳过
            }
            if (sku.getQuantity().remainder(palletSize).compareTo(BigDecimal.ZERO) != 0) {
                throw new BizException("200003",
                        "SKU " + sku.getSkuCode() + " 数量 " + sku.getQuantity()
                                + " 不是整托规格 " + palletSize + " 的倍数");
            }
        }
    }
}
