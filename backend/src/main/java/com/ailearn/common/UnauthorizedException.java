package com.ailearn.common;

/**
 * 未登录 / 令牌无效。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
