package com.mall.controller;

import com.mall.common.Result;
import com.mall.dto.LoginDTO;
import com.mall.dto.LoginVO;
import com.mall.dto.PasswordDTO;
import com.mall.dto.ProfileDTO;
import com.mall.dto.RegisterDTO;
import com.mall.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口: 注册 / 登录 (无需登录即可访问)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 注册 (普通用户 / 商家) */
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success("注册成功", authService.register(dto));
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    /** 退出登录 (JWT 无状态, 前端清除 token 即可) */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("已退出登录", null);
    }

    /** 当前登录者资料 */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        return Result.success(authService.currentProfile());
    }

    /** 修改资料 */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody ProfileDTO dto) {
        authService.updateProfile(dto);
        return Result.success("资料已更新", null);
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordDTO dto) {
        authService.updatePassword(dto);
        return Result.success("密码修改成功, 请重新登录", null);
    }
}
