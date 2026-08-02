package com.demetrius.tribunal.erp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * tribunal-erp-service 启动类（下游 ERP 履约系统）
 */
@SpringBootApplication
@EnableFeignClients
@MapperScan("com.demetrius.tribunal.erp.infrastructure.mapper")
public class ErpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpServiceApplication.class, args);
    }
}
