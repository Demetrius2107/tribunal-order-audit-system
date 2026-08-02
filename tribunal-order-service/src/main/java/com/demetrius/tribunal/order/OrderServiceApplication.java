package com.demetrius.tribunal.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * tribunal-order-service 启动类（订单/审单领域微服务）
 */
@SpringBootApplication
@EnableFeignClients
@MapperScan("com.demetrius.tribunal.order.infrastructure.mapper")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
