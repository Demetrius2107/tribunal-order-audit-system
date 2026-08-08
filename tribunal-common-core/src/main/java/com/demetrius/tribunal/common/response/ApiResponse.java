package com.demetrius.tribunal.common.response;

import lombok.Data;
import org.slf4j.MDC;

/**
 * 统一响应体。
 *
 * <p>每次响应自动从 MDC 提取 traceId，实现全链路追踪。</p>
 */
@Data
public class ApiResponse<T> {

    private boolean success;

    private String code;

    private String message;

    private T data;

    /** 链路追踪 ID（由 TraceIdFilter 注入 MDC） */
    private String traceId;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setCode("000000");
        resp.setMessage("success");
        resp.setData(data);
        resp.setTraceId(MDC.get("traceId"));
        return resp;
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setSuccess(false);
        resp.setCode(code);
        resp.setMessage(message);
        resp.setTraceId(MDC.get("traceId"));
        return resp;
    }
}
