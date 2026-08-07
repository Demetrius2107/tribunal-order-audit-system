package com.demetrius.tribunal.common.response;

import lombok.Data;

/**
 * 统一响应体（。
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>补充分页响应结构（参照通用做法</li>
 *   <li>补充错误码枚举（，用 code 而非字符串描述</li>
 * </ul>
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
