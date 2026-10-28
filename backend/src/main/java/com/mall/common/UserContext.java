package com.mall.common;

/**
 * 基于 ThreadLocal 的登录上下文, 由 AuthInterceptor 写入与清理
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /** 可能为 null (未登录接口) */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 获取当前登录身份, 未登录则抛 401 */
    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw BusinessException.unauthorized("请先登录");
        }
        return user;
    }

    public static Long requireId() {
        return require().getId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
