package com.demetrius.tribunal.fulfillment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * tribunal-fulfillment-service 启动类（履约执行域）
 */
@SpringBootApplication
@MapperScan("com.demetrius.tribunal.fulfillment.infrastructure.mapper")
public class FulfillmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FulfillmentServiceApplication.class, args);
    }
}
