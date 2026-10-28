package com.mall.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.common.BusinessException;
import com.mall.common.Result;
import com.mall.common.UserContext;
import com.mall.dto.ProductQueryDTO;
import com.mall.entity.Admin;
import com.mall.entity.Announcement;
import com.mall.entity.Banner;
import com.mall.entity.Category;
import com.mall.entity.Merchant;
import com.mall.entity.Orders;
import com.mall.entity.Product;
import com.mall.entity.ProductReview;
import com.mall.entity.User;
import com.mall.service.AdminService;
import com.mall.service.CategoryService;
import com.mall.service.ContentService;
import com.mall.service.OrderService;
import com.mall.service.ProductService;
import com.mall.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 超级管理员后台接口
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final OrderService orderService;
    private final ReviewService reviewService;
    private final ContentService contentService;

    /** 仅管理员可访问 */
    private void requireAdmin() {
        if (!UserContext.require().isAdmin()) {
            throw BusinessException.forbidden("该操作仅超级管理员可用");
        }
    }

    // ---------------------------------------------------------
    // 数据概览
    // ---------------------------------------------------------

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        requireAdmin();
        return Result.success(adminService.overview());
    }

    /** 销售统计: 趋势 + 商品排行 */
    @GetMapping("/statistics/sales")
    public Result<Map<String, Object>> salesStatistics(@RequestParam(defaultValue = "30") int days,
                                                       @RequestParam(defaultValue = "10") int topN) {
        requireAdmin();
        return Result.success(adminService.salesStatistics(days, topN));
    }

    // ---------------------------------------------------------
    // 用户管理
    // ---------------------------------------------------------

    @GetMapping("/users")
    public Result<IPage<User>> users(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Integer status,
                                     @RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        requireAdmin();
        return Result.success(adminService.pageUsers(keyword, status, pageNum, pageSize));
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        requireAdmin();
        adminService.updateUserStatus(id, status);
        return Result.success(status != null && status == 1 ? "已启用" : "已禁用", null);
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        requireAdmin();
        adminService.deleteUser(id);
        return Result.success("用户已删除", null);
    }

    // ---------------------------------------------------------
    // 商家管理
    // ---------------------------------------------------------

    @GetMapping("/merchants")
    public Result<IPage<Merchant>> merchants(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize) {
        requireAdmin();
        return Result.success(adminService.pageMerchants(keyword, status, pageNum, pageSize));
    }

    @PutMapping("/merchants/{id}/status")
    public Result<Void> updateMerchantStatus(@PathVariable Long id, @RequestParam Integer status) {
        requireAdmin();
        adminService.updateMerchantStatus(id, status);
        return Result.success("商家状态已更新", null);
    }

    @DeleteMapping("/merchants/{id}")
    public Result<Void> deleteMerchant(@PathVariable Long id) {
        requireAdmin();
        adminService.deleteMerchant(id);
        return Result.success("商家已删除", null);
    }

    /** 管理员账号列表 */
    @GetMapping("/admins")
    public Result<IPage<Admin>> admins(@RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize) {
        requireAdmin();
        return Result.success(adminService.pageAdmins(pageNum, pageSize));
    }

    // ---------------------------------------------------------
    // 商品分类管理
    // ---------------------------------------------------------

    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        return Result.success(categoryService.listAll(false));
    }

    @GetMapping("/categories/tree")
    public Result<List<Category>> categoryTree() {
        return Result.success(categoryService.tree(false));
    }

    @PostMapping("/categories")
    public Result<Category> saveCategory(@RequestBody Category category) {
        requireAdmin();
        return Result.success("保存成功", categoryService.save(category));
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        requireAdmin();
        categoryService.delete(id);
        return Result.success("分类已删除", null);
    }

    // ---------------------------------------------------------
    // 商品管理 (平台视角)
    // ---------------------------------------------------------

    @GetMapping("/products")
    public Result<IPage<Product>> products(ProductQueryDTO query) {
        requireAdmin();
        return Result.success(productService.pageProducts(query));
    }

    @PutMapping("/products/{id}/status")
    public Result<Void> updateProductStatus(@PathVariable Long id, @RequestParam Integer status) {
        requireAdmin();
        Product product = productService.byId(id);
        product.setStatus(status == null || status == 0 ? 0 : 1);
        productService.updateBasic(product);
        return Result.success(product.getStatus() == 1 ? "商品已上架" : "商品已下架", null);
    }

    // ---------------------------------------------------------
    // 订单管理
    // ---------------------------------------------------------

    @GetMapping("/orders")
    public Result<IPage<Orders>> orders(@RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "10") int pageSize) {
        requireAdmin();
        return Result.success(orderService.page(null, null, status, keyword, pageNum, pageSize));
    }

    @GetMapping("/orders/{id}")
    public Result<Orders> orderDetail(@PathVariable Long id) {
        requireAdmin();
        return Result.success(orderService.detail(id));
    }

    // ---------------------------------------------------------
    // 评价管理
    // ---------------------------------------------------------

    @GetMapping("/reviews")
    public Result<IPage<ProductReview>> reviews(@RequestParam(required = false) Integer status,
                                                @RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize) {
        requireAdmin();
        return Result.success(reviewService.page(null, null, null, null, status, pageNum, pageSize));
    }

    @PutMapping("/reviews/{id}/status")
    public Result<Void> updateReviewStatus(@PathVariable Long id, @RequestParam Integer status) {
        requireAdmin();
        reviewService.updateStatus(id, status);
        return Result.success("评价状态已更新", null);
    }

    // ---------------------------------------------------------
    // 公告管理
    // ---------------------------------------------------------

    @GetMapping("/announcements")
    public Result<IPage<Announcement>> announcements(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(contentService.pageAnnouncements(keyword, status, pageNum, pageSize));
    }

    @PostMapping("/announcements")
    public Result<Announcement> saveAnnouncement(@RequestBody Announcement announcement) {
        requireAdmin();
        return Result.success("保存成功",
                contentService.saveAnnouncement(announcement, UserContext.requireId()));
    }

    @DeleteMapping("/announcements/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        requireAdmin();
        contentService.deleteAnnouncement(id);
        return Result.success("公告已删除", null);
    }

    // ---------------------------------------------------------
    // 轮播图管理
    // ---------------------------------------------------------

    @GetMapping("/banners")
    public Result<IPage<Banner>> banners(@RequestParam(required = false) Integer position,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(contentService.pageBanners(position, status, pageNum, pageSize));
    }

    @PostMapping("/banners")
    public Result<Banner> saveBanner(@RequestBody Banner banner) {
        requireAdmin();
        return Result.success("保存成功", contentService.saveBanner(banner));
    }

    @DeleteMapping("/banners/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        requireAdmin();
        contentService.deleteBanner(id);
        return Result.success("轮播图已删除", null);
    }
}
