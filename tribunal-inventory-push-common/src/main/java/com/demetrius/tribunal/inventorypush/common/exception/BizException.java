package com.demetrius.tribunal.inventorypush.common.exception;

import lombok.Getter;

/**
 * 库存推送系统业务异常（错误码约定见 PRD 附录 8.1：INV-001 ~ INV-008）。
 *
 * <p>由库存推送系统全局异常处理器统一捕获转成 {@link ApiResponse}。</p>
 */
@Getter
public class BizException extends RuntimeException {

    private final String code;

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this("INV-999", message);
    }
}
