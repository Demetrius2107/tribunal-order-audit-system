package com.demetrius.tribunal.billing;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * tribunal-billing-service 启动类（下游 金融账单系统）
 */
@SpringBootApplication
@EnableFeignClients
@MapperScan("com.demetrius.tribunal.billing.infrastructure.mapper")
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}
