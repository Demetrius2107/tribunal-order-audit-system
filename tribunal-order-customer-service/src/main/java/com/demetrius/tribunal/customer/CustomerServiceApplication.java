package com.demetrius.tribunal.customer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * tribunal-customer-service 启动类（客户/信用领域微服务）
 */
@SpringBootApplication
@MapperScan("com.demetrius.tribunal.customer.infrastructure.mapper")
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
