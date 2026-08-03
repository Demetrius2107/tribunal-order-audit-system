package com.demetrius.tribunal.financesettlement.common.exception;

import lombok.Getter;

/**
 * 金融结算系统业务异常（错误码约定见 PRD 附录 8.1：FIN-001 ~ FIN-010）。
 *
 * <p>由金融结算系统全局异常处理器统一捕获转成 {@link ApiResponse}。</p>
 */
@Getter
public class BizException extends RuntimeException {

    private final String code;

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this("FIN-999", message);
    }
}
