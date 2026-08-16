package com.demetrius.tribunal.customer.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.customer.domain.model.CreditLimit;
import com.demetrius.tribunal.customer.domain.model.Customer;
import com.demetrius.tribunal.customer.domain.repository.CustomerRepository;
import com.demetrius.tribunal.customer.infrastructure.mapper.CustomerMapper;
import com.demetrius.tribunal.customer.infrastructure.model.CustomerPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 客户仓储实现（infrastructure 层）。
 */
@Repository
public class CustomerRepositoryImpl implements CustomerRepository {

    private final CustomerMapper customerMapper;

    public CustomerRepositoryImpl(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    @Override
    public Optional<Customer> findById(String id) {
        CustomerPo po = customerMapper.selectById(id);
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public Optional<Customer> findByCustomerCode(String customerCode) {
        CustomerPo po = customerMapper.selectOne(
                new LambdaQueryWrapper<CustomerPo>().eq(CustomerPo::getCustomerCode, customerCode));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public void save(Customer customer) {
        CustomerPo po = new CustomerPo();
        po.setId(customer.getId());
        po.setCustomerCode(customer.getCustomerCode());
        po.setName(customer.getName());
        po.setCreditLimit(customer.getCreditLimit().limit());
        po.setCreditUsed(customer.getCreditLimit().used());
        po.setDiscountPoolBalance(customer.getDiscountPoolBalance());
        // 存在则更新、不存在则插入（信用占用/释放是更新已存在客户）
        if (customerMapper.selectById(customer.getId()) == null) {
            customerMapper.insert(po);
        } else {
            customerMapper.updateById(po);
        }
    }

    private Customer toDomain(CustomerPo po) {
        CreditLimit credit = new CreditLimit(po.getCreditLimit(), po.getCreditUsed());
        return new Customer(po.getId(), po.getCustomerCode(), po.getName(), credit,
                po.getDiscountPoolBalance());
    }
}
