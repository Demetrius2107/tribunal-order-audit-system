package com.demetrius.tribunal.notification;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * tribunal-notification-service 启动类（通知域）
 */
@SpringBootApplication
@MapperScan("com.demetrius.tribunal.notification.infrastructure.mapper")
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
