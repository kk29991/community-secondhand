package com.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 注册请求 */
@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需为 3-20 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
    private String password;

    @Size(max = 20, message = "昵称最多 20 个字符")
    private String nickname;

    private String phone;

    private String email;

    /** 注册类型: USER / MERCHANT, 默认 USER */
    private String role = "USER";

    // ---------- 商家注册专用 ----------

    private String storeName;

    private String contactName;

    private String contactPhone;
}
