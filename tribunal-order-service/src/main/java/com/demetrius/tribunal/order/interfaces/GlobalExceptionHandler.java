package com.demetrius.tribunal.order.interfaces;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * order-service 全局异常处理器。
 *
 * <p>统一把异常转成 {@link ApiResponse}，接口层不再散落 try-catch。</p>
 *
 * <p>参照通用做法：统一异常拦截。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>补充 Feign 调用失败的降级处理（FeignException / 超时）——里程碑 5</li>
 *   <li>补充未知异常（Exception）的日志记录（参照通用做法：异常日志落 ES）</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常（规则校验失败等），返回 400 + 错误码。
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /**
     * 参数校验失败（@Valid 触发），返回 400 + 具体字段错误。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("400000", message));
    }

    /**
     * 兜底：未知异常（TODO：记录日志到 ES）。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("500000", "系统异常: " + e.getMessage()));
    }
}
