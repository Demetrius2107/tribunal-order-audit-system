package com.demetrius.tribunal.marketing;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * tribunal-marketing-service 启动类（营销价格域）
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.demetrius.tribunal.marketing.infrastructure.mapper")
public class MarketingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketingServiceApplication.class, args);
    }
}
