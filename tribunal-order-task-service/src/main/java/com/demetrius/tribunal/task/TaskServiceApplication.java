package com.demetrius.tribunal.task;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * tribunal-task-service 启动类（定时任务域）
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.demetrius.tribunal.task.infrastructure.mapper")
public class TaskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }
}
