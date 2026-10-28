package com.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.dto.ReviewDTO;
import com.mall.entity.OrderItem;
import com.mall.entity.Orders;
import com.mall.entity.Product;
import com.mall.entity.ProductReview;
import com.mall.entity.User;
import com.mall.mapper.OrderItemMapper;
import com.mall.mapper.OrderMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.ProductReviewMapper;
import com.mall.mapper.UserMapper;
import com.mall.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品评价服务: 买家评价 / 商家回复
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ProductReviewMapper reviewMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    /**
     * 提交评价 (仅已完成订单且未评价过的明细)
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long userId, ReviewDTO dto) {
        OrderItem item = orderItemMapper.selectById(dto.getOrderItemId());
        if (item == null) {
            throw BusinessException.notFound("订单明细不存在");
        }
        Orders order = orderMapper.selectById(item.getOrderId());
        if (order == null || !userId.equals(order.getUserId())) {
            throw BusinessException.forbidden("无权评价该订单");
        }
        if (order.getStatus() != Orders.STATUS_FINISHED) {
            throw new BusinessException("订单完成后才能评价");
        }
        if (item.getCommented() != null && item.getCommented() == 1) {
            throw new BusinessException("该商品已评价过了");
        }

        ProductReview review = new ProductReview();
        review.setProductId(item.getProductId());
        review.setOrderId(order.getId());
        review.setOrderItemId(item.getId());
        review.setUserId(userId);
        review.setMerchantId(order.getMerchantId());
        review.setScore(dto.getScore());
        review.setContent(dto.getContent());
        review.setImages(JsonUtil.toJson(dto.getImages()));
        review.setAnonymous(dto.getAnonymous() == null ? 0 : dto.getAnonymous());
        review.setStatus(1);
        reviewMapper.insert(review);

        item.setCommented(1);
        orderItemMapper.updateById(item);
    }

    /**
     * 商家回复评价
     */
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long merchantId, Long reviewId, String content) {
        ProductReview review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw BusinessException.notFound("评价不存在");
        }
        if (!merchantId.equals(review.getMerchantId())) {
            throw BusinessException.forbidden("无权回复其他店铺的评价");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException("回复内容不能为空");
        }
        review.setReplyContent(content);
        review.setReplyTime(LocalDateTime.now());
        reviewMapper.updateById(review);
    }

    /**
     * 删除回复
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteReply(Long merchantId, Long reviewId) {
        ProductReview review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw BusinessException.notFound("评价不存在");
        }
        if (!merchantId.equals(review.getMerchantId())) {
            throw BusinessException.forbidden("无权操作其他店铺的评价");
        }
        review.setReplyContent(null);
        review.setReplyTime(null);
        reviewMapper.updateById(review);
    }

    /**
     * 隐藏/显示评价 (管理员)
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long reviewId, Integer status) {
        ProductReview review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw BusinessException.notFound("评价不存在");
        }
        review.setStatus(status == null || status == 0 ? 0 : 1);
        reviewMapper.updateById(review);
    }

    /**
     * 评价分页查询
     */
    public IPage<ProductReview> page(Long productId, Long merchantId, Long userId,
                                     Boolean hasReply, Integer status, int pageNum, int pageSize) {
        Page<ProductReview> page = new Page<>(Math.max(pageNum, 1), pageSize < 1 ? 10 : pageSize);
        IPage<ProductReview> result = reviewMapper.selectReviewPage(page, productId, merchantId,
                userId, hasReply, status);
        for (ProductReview review : result.getRecords()) {
            review.setImageList(JsonUtil.toStringList(review.getImages()));
            if (review.getAnonymous() != null && review.getAnonymous() == 1) {
                review.setDisplayName("匿名用户");
                review.setAvatar(null);
            } else {
                review.setDisplayName(review.getNickname() == null ? "社区用户" : review.getNickname());
            }
        }
        return result;
    }

    /**
     * 待评价的订单明细 (我的评价页)
     */
    public List<OrderItem> pendingReviewItems(Long userId) {
        List<OrderItem> items = orderItemMapper.selectPendingReviewItems(userId);
        if (items.isEmpty()) {
            return items;
        }
        List<Long> productIds = items.stream().map(OrderItem::getProductId).distinct().collect(Collectors.toList());
        var products = productMapper.selectBatchIds(productIds);
        var nameMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));
        for (OrderItem item : items) {
            Product product = nameMap.get(item.getProductId());
            if (product != null) {
                item.setProductName(product.getName());
                item.setProductImage(product.getMainImage());
            }
        }
        return items;
    }

    /**
     * 商品评价列表 (前台商品详情页)
     */
    public IPage<ProductReview> pageByProduct(Long productId, Boolean hasReply, int pageNum, int pageSize) {
        return page(productId, null, null, hasReply, 1, pageNum, pageSize);
    }

    /** 商家评价概览: 平均分 / 总数 / 待回复数 */
    public java.util.Map<String, Object> merchantSummary(Long merchantId) {
        var all = reviewMapper.selectList(Wrappers.<ProductReview>lambdaQuery()
                .eq(ProductReview::getMerchantId, merchantId)
                .eq(ProductReview::getStatus, 1));
        double avg = all.stream().mapToInt(r -> r.getScore() == null ? 0 : r.getScore()).average().orElse(0);
        long pendingReply = all.stream()
                .filter(r -> r.getReplyContent() == null || r.getReplyContent().isBlank())
                .count();
        return java.util.Map.of(
                "avgScore", Math.round(avg * 10) / 10.0,
                "total", all.size(),
                "pendingReply", pendingReply
        );
    }
}
