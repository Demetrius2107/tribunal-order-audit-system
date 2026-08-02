package com.demetrius.tribunal.order.domain.service;

import com.demetrius.tribunal.common.dto.CustomerCreditDto;
import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.order.domain.model.Order;

import java.math.BigDecimal;

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
     * <p>骨架只做了信用校验的雏形，其余留 TODO。</p>
     *
     * @param order    订单聚合
     * @param credit   客户信用（Feign 从 customer-service 查询）
     * @param operator 操作人（TODO：实现权限校验时使用）
     */
    public void validateForReview(Order order, CustomerCreditDto credit, String operator) {
        // ② 信用校验（雏形）
        BigDecimal payable = order.getPayableAmount();
        if (!credit.hasEnoughFor(payable)) {
            throw new BizException("200001",
                    "信用额度不足: 可用信用 " + credit.available() + ", 应付金额 " + payable);
        }
        // ③ 整托校验（TODO：对照旧项目 wholePalletCheck 实现）
        // ④ 促销折扣重算（TODO）
        // ⑤ 权限校验（TODO）
    }
}
