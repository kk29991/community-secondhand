package com.mall.common;

import lombok.Getter;

/**
 * 业务异常: 由全局异常处理器统一转换为 Result
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /** 未登录 / token 失效 */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message);
    }

    /** 已登录但无权限 */
    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }
}
