package com.demetrius.tribunal.inventory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * tribunal-inventory-service 启动类（库存物料模块）
 */
@SpringBootApplication
@MapperScan("com.demetrius.tribunal.inventory.infrastructure.mapper")
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
