package com.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.entity.Admin;
import com.mall.entity.Category;
import com.mall.entity.Merchant;
import com.mall.entity.Product;
import com.mall.entity.User;
import com.mall.mapper.AdminMapper;
import com.mall.mapper.CategoryMapper;
import com.mall.mapper.MerchantMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.UserMapper;
import com.mall.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台管理员服务: 用户 / 商家 / 首页统计
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final AdminMapper adminMapper;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final OrderMapper orderMapper;

    // ---------------------------------------------------------
    // 用户管理
    // ---------------------------------------------------------

    public IPage<User> pageUsers(String keyword, Integer status, int pageNum, int pageSize) {
        Page<User> page = new Page<>(Math.max(pageNum, 1), pageSize < 1 ? 10 : pageSize);
        return userMapper.selectPage(page, Wrappers.<User>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(User::getUsername, keyword)
                        .or().like(User::getNickname, keyword)
                        .or().like(User::getPhone, keyword))
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getCreateTime));
    }

    /**
     * 启用/禁用用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        user.setStatus(status == null || status == 0 ? 0 : 1);
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw BusinessException.notFound("用户不存在");
        }
        userMapper.deleteById(userId);
    }

    // ---------------------------------------------------------
    // 商家管理
    // ---------------------------------------------------------

    public IPage<Merchant> pageMerchants(String keyword, Integer status, int pageNum, int pageSize) {
        Page<Merchant> page = new Page<>(Math.max(pageNum, 1), pageSize < 1 ? 10 : pageSize);
        return merchantMapper.selectPage(page, Wrappers.<Merchant>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Merchant::getStoreName, keyword)
                        .or().like(Merchant::getUsername, keyword)
                        .or().like(Merchant::getContactPhone, keyword))
                .eq(status != null, Merchant::getStatus, status)
                .orderByDesc(Merchant::getCreateTime));
    }

    /**
     * 审核 / 启用 / 禁用商家
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMerchantStatus(Long merchantId, Integer status) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw BusinessException.notFound("商家不存在");
        }
        merchant.setStatus(status == null ? 1 : status);
        merchantMapper.updateById(merchant);
    }

    /**
     * 删除商家: 存在在售商品时禁止删除
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMerchant(Long merchantId) {
        Long productCount = productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getMerchantId, merchantId));
        if (productCount != null && productCount > 0) {
            throw new BusinessException("该商家下仍有 " + productCount + " 个商品, 请先下架或删除商品");
        }
        merchantMapper.deleteById(merchantId);
    }

    // ---------------------------------------------------------
    // 管理员账号
    // ---------------------------------------------------------

    public IPage<Admin> pageAdmins(int pageNum, int pageSize) {
        Page<Admin> page = new Page<>(Math.max(pageNum, 1), pageSize < 1 ? 10 : pageSize);
        return adminMapper.selectPage(page, Wrappers.<Admin>lambdaQuery().orderByDesc(Admin::getCreateTime));
    }

    // ---------------------------------------------------------
    // 数据概览 (首页)
    // ---------------------------------------------------------

    /**
     * 平台统计概览
     */
    public Map<String, Object> overview() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userCount", userMapper.selectCount(Wrappers.<User>lambdaQuery()));
        map.put("merchantCount", merchantMapper.selectCount(Wrappers.<Merchant>lambdaQuery()));
        map.put("productCount", productMapper.selectCount(Wrappers.<Product>lambdaQuery()));
        map.put("onSaleCount", productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getStatus, 1)));
        map.put("categoryCount", categoryMapper.selectCount(Wrappers.<Category>lambdaQuery()));
        map.put("orderCount", orderMapper.countByStatus(null, null, null));
        map.put("unpaidCount", orderMapper.countByStatus(null, null, 0));
        map.put("undeliveredCount", orderMapper.countByStatus(null, null, 1));
        map.put("unreceivedCount", orderMapper.countByStatus(null, null, 2));
        map.put("finishedCount", orderMapper.countByStatus(null, null, 3));
        // 累计成交额(待收货 + 已完成)
        java.util.Map<String, Object> sales = orderMapper.selectPlatformSalesSummary();
        map.put("totalSales", sales == null ? 0 : sales.get("totalAmount"));
        map.put("finishedSalesCount", sales == null ? 0 : sales.get("totalCount"));
        return map;
    }

    /**
     * 销售统计: 趋势 + 商品排行
     */
    public Map<String, Object> salesStatistics(int days, int topN) {
        Map<String, Object> map = new LinkedHashMap<>();
        List<Map<String, Object>> trend = orderMapper.selectSalesTrend(days);
        map.put("trend", trend);
        map.put("productRanking", orderMapper.selectProductRanking(topN));
        map.put("totalSales", trend.stream()
                .mapToDouble(t -> Double.parseDouble(String.valueOf(t.getOrDefault("amount", 0))))
                .sum());
        map.put("totalOrders", trend.stream()
                .mapToLong(t -> Long.parseLong(String.valueOf(t.getOrDefault("cnt", 0))))
                .sum());
        return map;
    }
}
