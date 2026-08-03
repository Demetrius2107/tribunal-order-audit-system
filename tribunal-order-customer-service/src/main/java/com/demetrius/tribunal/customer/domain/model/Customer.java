package com.demetrius.tribunal.customer.domain.model;

import java.math.BigDecimal;

/**
 * 客户聚合根。
 *
 * <p>、 表。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>补充业务规则：客户状态（启用/禁用）、下单限制（CustomerLimitCreateOrderSetting）、
 *       客户-SKU 关系（哪些 SKU 该客户可下单）</li>
 *   <li>AD 账号关系（销售归属，——可选</li>
 * </ul>
 */
public class Customer {

    private final String id;

    private final String customerCode;

    private final String name;

    private final CreditLimit creditLimit;

    public Customer(String id, String customerCode, String name, CreditLimit creditLimit) {
        this.id = id;
        this.customerCode = customerCode;
        this.name = name;
        this.creditLimit = creditLimit;
    }

    /** 占用信用（下单即冻结额度，对应 F-403/N-301） */
    public Customer occupyCredit(BigDecimal amount) {
        return new Customer(id, customerCode, name, creditLimit.occupy(amount));
    }

    /** 释放信用（审单拒绝/订单取消后释放，对应 F-403） */
    public Customer releaseCredit(BigDecimal amount) {
        return new Customer(id, customerCode, name, creditLimit.release(amount));
    }

    public String getId() {
        return id;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public String getName() {
        return name;
    }

    public CreditLimit getCreditLimit() {
        return creditLimit;
    }
}
