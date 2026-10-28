package com.mall.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.Result;
import com.mall.dto.ProductQueryDTO;
import com.mall.entity.Announcement;
import com.mall.entity.Banner;
import com.mall.entity.Category;
import com.mall.entity.Merchant;
import com.mall.entity.MerchantTag;
import com.mall.entity.Product;
import com.mall.entity.ProductReview;
import com.mall.mapper.MerchantMapper;
import com.mall.service.CategoryService;
import com.mall.service.ContentService;
import com.mall.service.MerchantTagService;
import com.mall.service.ProductService;
import com.mall.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 前台公开接口 (无需登录): 分类 / 商品 / 轮播 / 公告
 */
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicController {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final ContentService contentService;
    private final ReviewService reviewService;
    private final MerchantTagService merchantTagService;
    private final MerchantMapper merchantMapper;

    /** 分类树 */
    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        return Result.success(categoryService.tree(true));
    }

    /** 分类平铺列表 */
    @GetMapping("/categories/flat")
    public Result<List<Category>> categoriesFlat() {
        return Result.success(categoryService.listAll(true));
    }

    /** 商品分页 (前台只返回上架商品) */
    @GetMapping("/products")
    public Result<IPage<Product>> products(ProductQueryDTO query) {
        query.setStatus(1);
        return Result.success(productService.pageProducts(query));
    }

    /** 商品详情 */
    @GetMapping("/products/{id}")
    public Result<Product> productDetail(@PathVariable Long id) {
        return Result.success(productService.detail(id));
    }

    /** 热门商品 */
    @GetMapping("/products/hot")
    public Result<List<Product>> hotProducts(@RequestParam(defaultValue = "8") int limit) {
        return Result.success(productService.hotProducts(limit));
    }

    /** 商品评价 */
    @GetMapping("/products/{id}/reviews")
    public Result<IPage<ProductReview>> productReviews(@PathVariable Long id,
                                                       @RequestParam(required = false) Boolean hasReply,
                                                       @RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(reviewService.page(id, null, null, hasReply, 1, pageNum, pageSize));
    }

    /** 首页轮播图 */
    @GetMapping("/banners")
    public Result<List<Banner>> banners(@RequestParam(required = false) Integer position) {
        return Result.success(contentService.listActiveBanners(position));
    }

    /** 公告列表 */
    @GetMapping("/announcements")
    public Result<List<Announcement>> announcements(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(contentService.listPublished(limit));
    }

    /** 公告详情 */
    @GetMapping("/announcements/{id}")
    public Result<Announcement> announcement(@PathVariable Long id) {
        return Result.success(contentService.getAnnouncement(id));
    }

    /** 店铺标签 */
    @GetMapping("/merchants/{merchantId}/tags")
    public Result<List<MerchantTag>> merchantTags(@PathVariable Long merchantId) {
        return Result.success(merchantTagService.listByMerchant(merchantId));
    }

    /**
     * 演示用商家账号列表(仅返回用户名与店铺名, 不含密码)。
     * <p>
     * 供登录页展示测试账号, 数据直接来自数据库, 避免把账号名硬编码在前端
     * 而导致商家改名/新增后登录页提示与实际不一致。
     */
    @GetMapping("/demo-merchants")
    public Result<List<Map<String, Object>>> demoMerchants() {
        List<Map<String, Object>> list = merchantMapper.selectList(
                        Wrappers.<Merchant>lambdaQuery()
                                .eq(Merchant::getStatus, 1)
                                .orderByAsc(Merchant::getId))
                .stream()
                .map(m -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("username", m.getUsername());
                    item.put("storeName", m.getStoreName());
                    return item;
                })
                .toList();
        return Result.success(list);
    }
}
