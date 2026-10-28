package com.mall.config;

import com.mall.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web 配置: 拦截器注册 + 上传文件静态映射
 * <p>
 * 关于 {@code /api} 前缀: 由 {@code server.servlet.context-path} 统一提供,
 * 而不是用 {@code configurePathMatch} 的 addPathPrefix。
 * <p>
 * 原因: addPathPrefix 只作用于 {@code @Controller}, 而接口文档(doc.html)、
 * swagger-ui、springdoc 的 /v3/api-docs 都是独立的 HandlerMapping / 静态资源,
 * 不会带上该前缀, 导致「文档页面能打开但取不到接口数据」。
 * 改用 context-path 后, controller、过滤链、文档、静态资源全部天然统一在 /api 下。
 * <p>
 * 注意: 配置了 context-path 后, 拦截器的 addPathPatterns 是**相对于 context-path** 的,
 * 因此这里的路径不写 /api。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Value("${mall.upload.path:./uploads}")
    private String uploadPath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                // 仅放行真正无需登录的接口。
                // 注意: 不能整体排除 /auth/**, 因为 /auth/profile 与 /auth/password
                // 依赖拦截器解析 JWT 写入登录上下文, 一旦被排除就会永远 401。
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/auth/logout",
                        "/public/**",
                        "/uploads/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadPath).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
