package com.mall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 登录请求 */
@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 登录身份: USER / MERCHANT / ADMIN, 默认 USER */
    private String role = "USER";
}
