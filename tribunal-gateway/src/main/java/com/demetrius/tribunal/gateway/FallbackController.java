package com.demetrius.tribunal.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网关熔断降级端点（M5 CircuitBreaker filter 的 fallbackUri）。
 *
 * <p>下游服务不可用触发熔断时，网关返回统一的友好降级响应（503 + 提示），
 * 而不是把连接错误透传给调用方。</p>
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @GetMapping
    public Mono<Map<String, Object>> fallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        log.warn("网关熔断降级: 请求 {} 转发失败，返回降级响应", path);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "503000");
        body.put("message", "服务暂不可用，请稍后重试");
        body.put("path", path);
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return Mono.just(body);
    }
}
