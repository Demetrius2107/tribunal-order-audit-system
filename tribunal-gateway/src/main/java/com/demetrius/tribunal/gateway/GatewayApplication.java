package com.demetrius.tribunal.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Spring Cloud Gateway 网关启动类（M5）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>统一入口：所有外部请求经由网关转发到后端微服务</li>
 *   <li>路由：按路径前缀路由到对应服务（通过 Nacos 服务名解析）</li>
 *   <li>鉴权前置：解析 JWT，将 userId/role 注入下游请求头</li>
 *   <li>限流/熔断：Resilience4j 全局限流</li>
 * </ul>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
