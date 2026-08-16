package com.demetrius.tribunal.customer.application.service;

import com.demetrius.tribunal.common.dto.CustomerCreditDto;
import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.customer.domain.model.Customer;
import com.demetrius.tribunal.customer.domain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 客户应用服务（客户/信用领域微服务的用例编排）。
 *
 * <p>职责：把客户领域能力暴露成 REST 接口供其他服务（order-service）跨服务调用。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>客户创建/维护接口（参照通用做法</li>
 * </ul>
 */
@Service
public class CustomerApplicationService {

    private final CustomerRepository customerRepository;

    public CustomerApplicationService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * 查询客户信用信息（供 order-service 审单时校验信用）。
     */
    @Transactional(readOnly = true)
    public CustomerCreditDto getCustomerCredit(String customerId) {
        Customer customer = findCustomer(customerId);
        return toDto(customer);
    }

    /**
     * 占用信用（下单即冻结额度，order-service 下单时调用，对应 F-403/N-301）。
     */
    @Transactional
    public CustomerCreditDto occupyCredit(String customerId, BigDecimal amount) {
        Customer customer = findCustomer(customerId);
        Customer occupied = customer.occupyCredit(amount);
        customerRepository.save(occupied);
        return toDto(occupied);
    }

    /**
     * 释放信用（审单拒绝/订单取消后释放，order-service 调用，对应 F-403）。
     */
    @Transactional
    public CustomerCreditDto releaseCredit(String customerId, BigDecimal amount) {
        Customer customer = findCustomer(customerId);
        Customer released = customer.releaseCredit(amount);
        customerRepository.save(released);
        return toDto(released);
    }

    /**
     * 促销返还入折扣池（F-203：营销活动返还 → 增加客户折扣池余额）。
     */
    @Transactional
    public CustomerCreditDto addDiscountPool(String customerId, BigDecimal amount) {
        Customer customer = findCustomer(customerId);
        Customer credited = customer.addDiscountPool(amount);
        customerRepository.save(credited);
        return toDto(credited);
    }

    private Customer findCustomer(String customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new BizException("100001", "客户不存在: " + customerId));
    }

    private CustomerCreditDto toDto(Customer customer) {
        return new CustomerCreditDto(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getCreditLimit().limit(),
                customer.getCreditLimit().used(),
                customer.getDiscountPoolBalance());
    }
}
