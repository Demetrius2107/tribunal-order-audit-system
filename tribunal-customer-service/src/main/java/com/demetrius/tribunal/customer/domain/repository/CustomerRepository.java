package com.demetrius.tribunal.customer.domain.repository;

import com.demetrius.tribunal.customer.domain.model.Customer;

import java.util.Optional;

/**
 * 客户仓储接口（domain 定义，infrastructure 实现）。
 */
public interface CustomerRepository {

    Optional<Customer> findById(String id);

    Optional<Customer> findByCustomerCode(String customerCode);

    void save(Customer customer);
}
