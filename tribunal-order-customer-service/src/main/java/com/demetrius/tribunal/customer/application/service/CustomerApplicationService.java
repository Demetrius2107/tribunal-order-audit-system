package com.demetrius.tribunal.customer.application.service;

import com.demetrius.tribunal.common.dto.CustomerCreditDto;
import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.customer.domain.model.Customer;
import com.demetrius.tribunal.customer.domain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客户应用服务（客户/信用领域微服务的用例编排）。
 *
 * <p>职责：把客户领域能力暴露成 REST 接口供其他服务（order-service）跨服务调用。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>信用占用/释放接口（POST /api/customers/{id}/credit/occupy 等）——审单通过后应由
 *       order-service 调用 customer-service 完成信用扣减，而不是本地改数</li>
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
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BizException("100001", "客户不存在: " + customerId));
        return new CustomerCreditDto(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getCreditLimit().limit(),
                customer.getCreditLimit().used());
    }
}
