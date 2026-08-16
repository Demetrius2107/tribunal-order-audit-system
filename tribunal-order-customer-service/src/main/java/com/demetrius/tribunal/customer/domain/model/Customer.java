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

    /** 折扣池余额（促销返还，可抵扣应付，F-203） */
    private final BigDecimal discountPoolBalance;

    public Customer(String id, String customerCode, String name, CreditLimit creditLimit) {
        this(id, customerCode, name, creditLimit, BigDecimal.ZERO);
    }

    public Customer(String id, String customerCode, String name,
                    CreditLimit creditLimit, BigDecimal discountPoolBalance) {
        this.id = id;
        this.customerCode = customerCode;
        this.name = name;
        this.creditLimit = creditLimit;
        this.discountPoolBalance = discountPoolBalance == null ? BigDecimal.ZERO : discountPoolBalance;
    }

    /** 占用信用（下单即冻结额度，对应 F-403/N-301） */
    public Customer occupyCredit(BigDecimal amount) {
        return new Customer(id, customerCode, name, creditLimit.occupy(amount), discountPoolBalance);
    }

    /** 释放信用（审单拒绝/订单取消后释放，对应 F-403） */
    public Customer releaseCredit(BigDecimal amount) {
        return new Customer(id, customerCode, name, creditLimit.release(amount), discountPoolBalance);
    }

    /** 增加折扣池余额（促销返还入账，F-203） */
    public Customer addDiscountPool(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("返还金额必须大于0");
        }
        return new Customer(id, customerCode, name, creditLimit,
                discountPoolBalance.add(amount));
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

    public BigDecimal getDiscountPoolBalance() {
        return discountPoolBalance;
    }
}
