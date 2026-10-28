package com.mall.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.common.Result;
import com.mall.common.UserContext;
import com.mall.dto.ProductFormDTO;
import com.mall.dto.ProductQueryDTO;
import com.mall.entity.MerchantTag;
import com.mall.entity.OrderItem;
import com.mall.entity.Orders;
import com.mall.entity.Product;
import com.mall.entity.ProductReview;
import com.mall.service.MerchantCenterService;
import com.mall.service.MerchantTagService;
import com.mall.service.OrderService;
import com.mall.service.ProductService;
import com.mall.service.ReviewService;
import jakarta.validation.Valid;
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
 * 商家后台接口 (需商家身份): 看板 / 商品管理 / 订单处理 / 回复评价 / 标签管理
 */
@RestController
@RequestMapping("/admin/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantCenterService merchantCenterService;
    private final ProductService productService;
    private final OrderService orderService;
    private final ReviewService reviewService;
    private final MerchantTagService merchantTagService;

    private Long merchantId() {
        return UserContext.require().requireMerchantId();
    }

    // ---------------------------------------------------------
    // 数据看板
    // ---------------------------------------------------------

    /** 经营看板 */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(merchantCenterService.dashboard(merchantId()));
    }

    /** 销售统计 */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(@RequestParam(defaultValue = "30") int days,
                                                  @RequestParam(defaultValue = "10") int topN) {
        return Result.success(merchantCenterService.salesStatistics(merchantId(), days, topN));
    }

    // ---------------------------------------------------------
    // 商品管理
    // ---------------------------------------------------------

    /** 我的商品列表 */
    @GetMapping("/products")
    public Result<IPage<Product>> products(ProductQueryDTO query) {
        query.setMerchantId(merchantId());
        query.setIncludeChildren(Boolean.TRUE);
        return Result.success(productService.pageProducts(query));
    }

    /** 商品详情 (编辑回显) */
    @GetMapping("/products/{id}")
    public Result<Product> productDetail(@PathVariable Long id) {
        Product product = productService.requireOwned(merchantId(), id);
        productService.enrich(product);
        return Result.success(product);
    }

    /** 新增 / 编辑商品 */
    @PostMapping("/products")
    public Result<Product> saveProduct(@Valid @RequestBody ProductFormDTO form) {
        return Result.success("保存成功", productService.save(merchantId(), form));
    }

    /** 上架 / 下架 */
    @PutMapping("/products/{id}/status")
    public Result<Void> updateProductStatus(@PathVariable Long id, @RequestParam Integer status) {
        productService.updateStatus(merchantId(), id, status);
        return Result.success(status != null && status == 1 ? "商品已上架" : "商品已下架", null);
    }

    /** 删除商品 */
    @DeleteMapping("/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(merchantId(), id);
        return Result.success("商品已删除", null);
    }

    // ---------------------------------------------------------
    // 订单处理
    // ---------------------------------------------------------

    /** 店铺订单列表 */
    @GetMapping("/orders")
    public Result<IPage<Orders>> orders(@RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(orderService.page(null, merchantId(), status, keyword, pageNum, pageSize));
    }

    /** 订单详情 */
    @GetMapping("/orders/{id}")
    public Result<Orders> orderDetail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    /** 发货 */
    @PutMapping("/orders/{id}/deliver")
    public Result<Void> deliver(@PathVariable Long id, @RequestParam(required = false) String expressNo) {
        orderService.deliver(merchantId(), id, expressNo);
        return Result.success("发货成功", null);
    }

    /** 订单状态数量 */
    @GetMapping("/orders/counts")
    public Result<Map<String, Long>> orderCounts() {
        return Result.success(orderService.statusCount(null, merchantId()));
    }

    // ---------------------------------------------------------
    // 回复评价
    // ---------------------------------------------------------

    /** 店铺评价列表 */
    @GetMapping("/reviews")
    public Result<IPage<ProductReview>> reviews(@RequestParam(required = false) Boolean hasReply,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(reviewService.page(null, merchantId(), null, hasReply, status, pageNum, pageSize));
    }

    /** 回复评价 */
    @PutMapping("/reviews/{id}/reply")
    public Result<Void> replyReview(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reviewService.reply(merchantId(), id, body.get("content"));
        return Result.success("回复成功", null);
    }

    /** 删除回复 */
    @DeleteMapping("/reviews/{id}/reply")
    public Result<Void> deleteReply(@PathVariable Long id) {
        reviewService.deleteReply(merchantId(), id);
        return Result.success("回复已删除", null);
    }

    // ---------------------------------------------------------
    // 商家标签
    // ---------------------------------------------------------

    /** 我的标签 */
    @GetMapping("/tags")
    public Result<List<MerchantTag>> tags() {
        return Result.success(merchantTagService.listByMerchant(merchantId()));
    }

    /** 新增 / 编辑标签 */
    @PostMapping("/tags")
    public Result<MerchantTag> saveTag(@RequestBody MerchantTag tag) {
        return Result.success("保存成功", merchantTagService.save(merchantId(), tag));
    }

    /** 删除标签 */
    @DeleteMapping("/tags/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        merchantTagService.delete(merchantId(), id);
        return Result.success("标签已删除", null);
    }
}
