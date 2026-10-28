package com.mall.interceptor;

import com.mall.common.LoginUser;
import com.mall.common.UserContext;
import com.mall.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录鉴权拦截器
 * <p>
 * 规则:
 * 1. 无 token: 放行, 作为游客访问 (需要登录的接口由 UserContext.require() 抛 401)
 * 2. 有 token 且合法: 写入 UserContext
 * 3. 路径以 /admin 开头且角色不是 ADMIN 或 MERCHANT: 返回 403
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 关键: 入口先清空。
        // Tomcat 复用工作线程, 若上一个请求是流式(SSE)且被客户端中断, afterCompletion
        // 不保证执行, 残留的登录态会被下一个未携带 token 的请求"继承", 造成未登录也能
        // 读到数据(已实测观察到该现象)。入口清空可彻底避免跨请求污染。
        UserContext.clear();

        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length()).trim();
            if (!token.isEmpty()) {
                try {
                    LoginUser loginUser = jwtUtil.parseToken(token);
                    UserContext.set(loginUser);
                } catch (Exception e) {
                    // token 非法或过期: 不写入上下文, 按未登录处理
                    log.warn("JWT 解析失败, 按未登录处理: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    UserContext.clear();
                }
            }
        }

        // 取「去掉 context-path 后」的路径再判断。
        // 配置 context-path=/api 时, getRequestURI() 返回 /api/xxx, 而
        // getServletPath() 返回 /xxx。统一基于 servletPath 判断, 这样无论
        // context-path 如何调整, 下面的鉴权规则都不需要跟着改。
        String path = request.getServletPath();

        // 流式接口(SSE)必须在进入 handler 之前完成鉴权:
        // 一旦响应已按 text/event-stream 提交, 再抛异常会因为内容协商不一致而
        // 产生空响应体(客户端拿不到 {} 也拿不到错误), 因此这里提前拦截。
        if (path.startsWith("/ai/chat/stream")) {
            requireLogin();
        }

        // 后台接口鉴权: /admin/merchant/** 需商家身份, 其余 /admin/** 需管理员身份
        if (path.startsWith("/admin/merchant/")) {
            LoginUser loginUser = requireLogin();
            if (!loginUser.isMerchant() || loginUser.getMerchantId() == null) {
                throw com.mall.common.BusinessException.forbidden("该接口仅商家可用");
            }
        } else if (path.startsWith("/admin/")) {
            LoginUser loginUser = requireLogin();
            if (!loginUser.isAdmin()) {
                throw com.mall.common.BusinessException.forbidden("该接口仅超级管理员可用");
            }
        }
        return true;
    }

    private LoginUser requireLogin() {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw com.mall.common.BusinessException.unauthorized("请先登录");
        }
        return loginUser;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
