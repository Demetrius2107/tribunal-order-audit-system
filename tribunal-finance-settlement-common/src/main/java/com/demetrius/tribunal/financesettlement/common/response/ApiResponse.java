package com.demetrius.tribunal.financesettlement.common.response;

import lombok.Data;

/**
 * 金融结算系统统一响应体（与订单系统 tribunal-common 独立，互不依赖）。
 */
@Data
public class ApiResponse<T> {

    private boolean success;

    private String code;

    private String message;

    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setCode("000000");
        resp.setMessage("success");
        resp.setData(data);
        return resp;
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setSuccess(false);
        resp.setCode(code);
        resp.setMessage(message);
        return resp;
    }
}
