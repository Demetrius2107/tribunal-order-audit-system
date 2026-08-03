package com.demetrius.tribunal.customer.domain.model;

/**
 * 客户聚合根。
 *
 * <p>对照旧项目：{@code CustomerServiceImpl}（1568 行）、{@code customer} 表。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>补充业务规则：客户状态（启用/禁用）、下单限制（CustomerLimitCreateOrderSetting）、
 *       客户-SKU 关系（哪些 SKU 该客户可下单）</li>
 *   <li>信用操作：下单占用、还款释放（对照 CustomerBalanceDetail 流水）</li>
 *   <li>AD 账号关系（销售归属，对照 ActiveDirectoryAssignCustomer）——可选</li>
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
