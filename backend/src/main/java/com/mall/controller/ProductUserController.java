package com.mall.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.BusinessException;
import com.mall.common.Result;
import com.mall.common.UserContext;
import com.mall.dto.ReviewDTO;
import com.mall.entity.OrderItem;
import com.mall.entity.Product;
import com.mall.entity.ProductCollect;
import com.mall.entity.ProductReview;
import com.mall.mapper.ProductCollectMapper;
import com.mall.mapper.ProductMapper;
import com.mall.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品收藏 + 商品评价 (买家侧, 需登录)
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class ProductUserController {

    private final ProductCollectMapper collectMapper;
    private final ProductMapper productMapper;
    private final ReviewService reviewService;

    // ---------------------------------------------------------
    // 商品收藏
    // ---------------------------------------------------------

    /** 收藏 / 取消收藏 (幂等切换) */
    @PostMapping("/collect/{productId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> toggleCollect(@PathVariable Long productId) {
        Long userId = UserContext.requireId();
        if (productMapper.selectById(productId) == null) {
            throw BusinessException.notFound("商品不存在");
        }
        ProductCollect existing = collectMapper.selectOne(Wrappers.<ProductCollect>lambdaQuery()
                .eq(ProductCollect::getUserId, userId)
                .eq(ProductCollect::getProductId, productId));
        if (existing != null) {
            collectMapper.deleteById(existing.getId());
            return Result.success("已取消收藏", false);
        }
        ProductCollect collect = new ProductCollect();
        collect.setUserId(userId);
        collect.setProductId(productId);
        collectMapper.insert(collect);
        return Result.success("收藏成功", true);
    }

    /** 我的收藏列表 */
    @GetMapping("/collect/list")
    public Result<List<ProductCollect>> myCollects() {
        Long userId = UserContext.requireId();
        List<ProductCollect> collects = collectMapper.selectList(Wrappers.<ProductCollect>lambdaQuery()
                .eq(ProductCollect::getUserId, userId)
                .orderByDesc(ProductCollect::getCreateTime));
        if (collects.isEmpty()) {
            return Result.success(collects);
        }
        List<Long> productIds = collects.stream().map(ProductCollect::getProductId).collect(Collectors.toList());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        for (ProductCollect collect : collects) {
            Product product = productMap.get(collect.getProductId());
            if (product != null) {
                // 列表页只需基础字段
                product.setDetail(null);
                product.setImages(null);
            }
            collect.setProduct(product);
        }
        return Result.success(new ArrayList<>(collects));
    }

    /** 取消收藏 */
    @DeleteMapping("/collect/{productId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> removeCollect(@PathVariable Long productId) {
        Long userId = UserContext.requireId();
        collectMapper.delete(Wrappers.<ProductCollect>lambdaQuery()
                .eq(ProductCollect::getUserId, userId)
                .eq(ProductCollect::getProductId, productId));
        return Result.success("已取消收藏", null);
    }

    // ---------------------------------------------------------
    // 商品评价
    // ---------------------------------------------------------

    /** 提交评价 */
    @PostMapping("/reviews")
    public Result<Void> submitReview(@Valid @RequestBody ReviewDTO dto) {
        reviewService.submit(UserContext.requireId(), dto);
        return Result.success("评价成功", null);
    }

    /** 待评价的订单明细 */
    @GetMapping("/reviews/pending")
    public Result<List<OrderItem>> pendingReviews() {
        return Result.success(reviewService.pendingReviewItems(UserContext.requireId()));
    }

    /** 我的评价 */
    @GetMapping("/reviews/list")
    public Result<IPage<ProductReview>> myReviews(@RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(reviewService.page(null, null, UserContext.requireId(), null, null, pageNum, pageSize));
    }
}
