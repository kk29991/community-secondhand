package com.mall.dto;

import lombok.Data;

/** 登录响应 */
@Data
public class LoginVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String role;

    private Long merchantId;

    private String storeName;

    private String storeLogo;

    private String token;

    /** token 有效期(秒) */
    private Long expiresIn;
}
