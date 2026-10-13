package com.ailearn.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 无权限 / 签名无效 / 链接过期:返回真正的 HTTP 403,
 * 便于浏览器直接访问资源时得到正确的状态码(而不是 200 + 错误 JSON)。
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
