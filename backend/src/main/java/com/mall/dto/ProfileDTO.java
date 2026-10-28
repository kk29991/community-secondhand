package com.mall.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改个人资料 */
@Data
public class ProfileDTO {

    @Size(max = 20, message = "昵称最多 20 个字符")
    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    /** 0未知 1男 2女 */
    private Integer gender;
}
