package com.demetrius.tribunal.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * tribunal-auth-service 启动类（认证授权域）
 */
@SpringBootApplication
@MapperScan("com.demetrius.tribunal.auth.infrastructure.mapper")
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
