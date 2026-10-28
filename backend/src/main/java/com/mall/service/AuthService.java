package com.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.BusinessException;
import com.mall.common.LoginUser;
import com.mall.common.RoleConstant;
import com.mall.common.UserContext;
import com.mall.dto.LoginDTO;
import com.mall.dto.LoginVO;
import com.mall.dto.PasswordDTO;
import com.mall.dto.ProfileDTO;
import com.mall.dto.RegisterDTO;
import com.mall.entity.Admin;
import com.mall.entity.Merchant;
import com.mall.entity.User;
import com.mall.mapper.AdminMapper;
import com.mall.mapper.MerchantMapper;
import com.mall.mapper.UserMapper;
import com.mall.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 认证与账号服务: 注册 / 登录 / 当前用户信息 / 改密
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 注册: 普通用户 或 商家
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterDTO dto) {
        if (RoleConstant.MERCHANT.equalsIgnoreCase(dto.getRole())) {
            return registerMerchant(dto);
        }
        return registerUser(dto);
    }

    private LoginVO registerUser(RegisterDTO dto) {
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("用户名已被注册");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isBlank()
                ? dto.getUsername() : dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setGender(0);
        user.setStatus(1);
        userMapper.insert(user);

        return buildUserLoginVO(user);
    }

    private LoginVO registerMerchant(RegisterDTO dto) {
        if (dto.getStoreName() == null || dto.getStoreName().isBlank()) {
            throw new BusinessException("商家注册需填写店铺名称");
        }
        Long exists = merchantMapper.selectCount(
                Wrappers.<Merchant>lambdaQuery().eq(Merchant::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该商家账号已被注册");
        }
        Merchant merchant = new Merchant();
        merchant.setUsername(dto.getUsername());
        merchant.setPassword(passwordEncoder.encode(dto.getPassword()));
        merchant.setStoreName(dto.getStoreName());
        merchant.setStoreDesc("这家店铺很懒, 还没有填写简介");
        merchant.setContactName(dto.getContactName() == null ? dto.getNickname() : dto.getContactName());
        merchant.setContactPhone(dto.getContactPhone() == null ? dto.getPhone() : dto.getContactPhone());
        // 注册后即可经营, 如需审核可改为 0
        merchant.setStatus(1);
        merchantMapper.insert(merchant);

        return buildMerchantLoginVO(merchant);
    }

    /**
     * 登录
     */
    public LoginVO login(LoginDTO dto) {
        String role = dto.getRole() == null ? RoleConstant.USER : dto.getRole().toUpperCase();
        return switch (role) {
            case RoleConstant.MERCHANT -> loginMerchant(dto);
            case RoleConstant.ADMIN -> loginAdmin(dto);
            default -> loginUser(dto);
        };
    }

    private LoginVO loginUser(LoginDTO dto) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用, 请联系管理员");
        }
        return buildUserLoginVO(user);
    }

    private LoginVO loginMerchant(LoginDTO dto) {
        Merchant merchant = merchantMapper.selectOne(
                Wrappers.<Merchant>lambdaQuery().eq(Merchant::getUsername, dto.getUsername()));
        if (merchant == null || !passwordEncoder.matches(dto.getPassword(), merchant.getPassword())) {
            throw new BusinessException("商家账号或密码错误");
        }
        if (merchant.getStatus() != null && merchant.getStatus() == 0) {
            throw new BusinessException("店铺已被禁用, 请联系平台管理员");
        }
        return buildMerchantLoginVO(merchant);
    }

    private LoginVO loginAdmin(LoginDTO dto) {
        Admin admin = adminMapper.selectOne(
                Wrappers.<Admin>lambdaQuery().eq(Admin::getUsername, dto.getUsername()));
        if (admin == null || !passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            throw new BusinessException("管理员账号或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException("管理员账号已被禁用");
        }
        LoginVO vo = new LoginVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setNickname(admin.getNickname());
        vo.setAvatar(admin.getAvatar());
        vo.setRole(RoleConstant.ADMIN);
        fillToken(vo, admin.getId(), admin.getUsername(), RoleConstant.ADMIN, null);
        return vo;
    }

    private LoginVO buildUserLoginVO(User user) {
        LoginVO vo = new LoginVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(RoleConstant.USER);
        fillToken(vo, user.getId(), user.getUsername(), RoleConstant.USER, null);
        return vo;
    }

    private LoginVO buildMerchantLoginVO(Merchant merchant) {
        LoginVO vo = new LoginVO();
        vo.setId(merchant.getId());
        vo.setUsername(merchant.getUsername());
        vo.setNickname(merchant.getStoreName());
        vo.setStoreName(merchant.getStoreName());
        vo.setStoreLogo(merchant.getStoreLogo());
        vo.setMerchantId(merchant.getId());
        vo.setRole(RoleConstant.MERCHANT);
        fillToken(vo, merchant.getId(), merchant.getUsername(), RoleConstant.MERCHANT, merchant.getId());
        return vo;
    }

    private void fillToken(LoginVO vo, Long id, String username, String role, Long merchantId) {
        vo.setToken(jwtUtil.createToken(id, username, role, merchantId));
        vo.setExpiresIn(jwtUtil.getExpireSeconds());
    }

    /**
     * 获取当前登录者的资料
     */
    public Map<String, Object> currentProfile() {
        LoginUser loginUser = UserContext.require();
        if (loginUser.isMerchant()) {
            Merchant m = merchantMapper.selectById(loginUser.getId());
            if (m == null) {
                throw BusinessException.notFound("商家不存在");
            }
            return Map.of(
                    "id", m.getId(),
                    "username", m.getUsername(),
                    "nickname", nullSafe(m.getStoreName()),
                    "avatar", nullSafe(m.getStoreLogo()),
                    "phone", nullSafe(m.getContactPhone()),
                    "email", "",
                    "gender", 0,
                    "role", RoleConstant.MERCHANT,
                    "storeName", nullSafe(m.getStoreName()),
                    "storeDesc", nullSafe(m.getStoreDesc())
            );
        }
        if (loginUser.isAdmin()) {
            Admin a = adminMapper.selectById(loginUser.getId());
            if (a == null) {
                throw BusinessException.notFound("管理员不存在");
            }
            return Map.of(
                    "id", a.getId(),
                    "username", a.getUsername(),
                    "nickname", nullSafe(a.getNickname()),
                    "avatar", nullSafe(a.getAvatar()),
                    "phone", "",
                    "email", "",
                    "gender", 0,
                    "role", RoleConstant.ADMIN
            );
        }
        User u = userMapper.selectById(loginUser.getId());
        if (u == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "nickname", nullSafe(u.getNickname()),
                "avatar", nullSafe(u.getAvatar()),
                "phone", nullSafe(u.getPhone()),
                "email", nullSafe(u.getEmail()),
                "gender", u.getGender() == null ? 0 : u.getGender(),
                "role", RoleConstant.USER
        );
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /**
     * 修改个人资料 (仅普通用户与商家)
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(ProfileDTO dto) {
        LoginUser loginUser = UserContext.require();
        if (loginUser.isMerchant()) {
            Merchant m = merchantMapper.selectById(loginUser.getId());
            if (m == null) {
                throw BusinessException.notFound("商家不存在");
            }
            if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
                m.setStoreName(dto.getNickname());
            }
            m.setStoreLogo(dto.getAvatar());
            m.setContactPhone(dto.getPhone());
            merchantMapper.updateById(m);
            return;
        }
        if (loginUser.isAdmin()) {
            Admin a = adminMapper.selectById(loginUser.getId());
            if (a == null) {
                throw BusinessException.notFound("管理员不存在");
            }
            a.setNickname(dto.getNickname());
            a.setAvatar(dto.getAvatar());
            adminMapper.updateById(a);
            return;
        }
        User u = userMapper.selectById(loginUser.getId());
        if (u == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            u.setNickname(dto.getNickname());
        }
        u.setAvatar(dto.getAvatar());
        u.setPhone(dto.getPhone());
        u.setEmail(dto.getEmail());
        if (dto.getGender() != null) {
            u.setGender(dto.getGender());
        }
        userMapper.updateById(u);
    }

    /**
     * 修改密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(PasswordDTO dto) {
        LoginUser loginUser = UserContext.require();
        if (loginUser.isMerchant()) {
            Merchant m = merchantMapper.selectById(loginUser.getId());
            verifyAndEncode(dto, m == null ? null : m.getPassword(), encoded -> {
                m.setPassword(encoded);
                merchantMapper.updateById(m);
            });
            return;
        }
        if (loginUser.isAdmin()) {
            Admin a = adminMapper.selectById(loginUser.getId());
            verifyAndEncode(dto, a == null ? null : a.getPassword(), encoded -> {
                a.setPassword(encoded);
                adminMapper.updateById(a);
            });
            return;
        }
        User u = userMapper.selectById(loginUser.getId());
        verifyAndEncode(dto, u == null ? null : u.getPassword(), encoded -> {
            u.setPassword(encoded);
            userMapper.updateById(u);
        });
    }

    private void verifyAndEncode(PasswordDTO dto, String currentEncoded, java.util.function.Consumer<String> updater) {
        if (currentEncoded == null) {
            throw BusinessException.notFound("账号不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), currentEncoded)) {
            throw new BusinessException("原密码不正确");
        }
        updater.accept(passwordEncoder.encode(dto.getNewPassword()));
    }

    /** 供其他服务复用: 加密明文密码 */
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
