package com.demetrius.tribunal.inventorypush;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * tribunal-inventory-push-service 启动类（库存推送系统/上游数据集成网关）。
 */
@SpringBootApplication
@MapperScan("com.demetrius.tribunal.inventorypush.infrastructure.mapper")
public class InventoryPushServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryPushServiceApplication.class, args);
    }
}
