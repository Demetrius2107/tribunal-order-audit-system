package com.demetrius.tribunal.common.exception;

import lombok.Getter;

/**
 * 业务异常（。
 *
 * <p>业务规则校验失败时抛出，由全局异常处理器（@RestControllerAdvice）统一捕获转成
 * {@link com.demetrius.tribunal.common.response.ApiResponse}。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>建立错误码枚举（订单不存在/信用不足/状态非法等），替代裸字符串 code</li>
 *   <li>在 order-service / customer-service 各建全局异常处理器（@RestControllerAdvice）</li>
 * </ul>
 */
@Getter
public class BizException extends RuntimeException {

    private final String code;

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this("500000", message);
    }
}
