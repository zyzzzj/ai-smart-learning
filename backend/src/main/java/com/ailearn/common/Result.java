package com.ailearn.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结构。
 */
@Data
public class Result<T> implements Serializable {

    /** 200 成功,其它为错误码 */
    private Integer code;
    private String message;
    private T data;
    /** 服务端时间戳(便于前端排查) */
    private Long timestamp = System.currentTimeMillis();

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> error(String message) {
        return error(500, message);
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
