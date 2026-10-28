package com.mall.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录身份 (由 JWT 解析得到)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    /** 用户ID: 普通用户ID / 商家ID / 管理员ID */
    private Long id;

    /** 登录用户名 */
    private String username;

    /** 角色: USER 普通用户, MERCHANT 商家, ADMIN 管理员 */
    private String role;

    /** 商家所属店铺ID, 仅 role=MERCHANT 时非空 */
    private Long merchantId;

    public LoginUser(Long id, String username, String role) {
        this(id, username, role, null);
    }

    public boolean isUser() {
        return RoleConstant.USER.equals(role);
    }

    public boolean isMerchant() {
        return RoleConstant.MERCHANT.equals(role);
    }

    public boolean isAdmin() {
        return RoleConstant.ADMIN.equals(role);
    }

    /**
     * 获取商家ID, 非商家身份抛 403
     */
    public Long requireMerchantId() {
        if (!isMerchant() || merchantId == null) {
            throw BusinessException.forbidden("当前身份不是商家");
        }
        return merchantId;
    }
}
