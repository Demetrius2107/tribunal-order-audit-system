package com.demetrius.tribunal.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 全链路追踪过滤器。
 *
 * <p>每个入站 HTTP 请求：</p>
 * <ol>
 *   <li>从请求头 {@code X-Trace-Id} 获取上游传入的 traceId；不存在则生成</li>
 *   <li>将 traceId 放入 MDC（日志自动输出 traceId 字段）</li>
 *   <li>将 traceId 写回响应头 {@code X-Trace-Id}，便于客户端关联</li>
 *   <li>请求结束后清除 MDC，防止线程池复用导致 traceId 串扰</li>
 * </ol>
 *
 * <p>配合 {@link TraceIdFeignInterceptor}，traceId 在 Feign 调用链中自动透传，
 * 实现「网关 → 订单 → 库存/营销/金融」全链路串联。</p>
 */
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 从请求头获取或生成 traceId
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }

        // 2. 放入 MDC
        MDC.put(MDC_TRACE_ID, traceId);

        // 3. 写入响应头
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 4. 清除 MDC（线程池复用安全）
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
