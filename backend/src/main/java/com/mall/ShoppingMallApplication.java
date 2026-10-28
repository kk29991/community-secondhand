package com.mall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 社区二手交易平台 - 后端启动类
 */
@SpringBootApplication
@MapperScan("com.mall.mapper")
public class ShoppingMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoppingMallApplication.class, args);
        System.out.println("""
                =====================================================
                  社区二手交易平台后端启动成功
                  接口文档: http://localhost:9090/api/swagger-ui/index.html
                =====================================================""");
    }
}
