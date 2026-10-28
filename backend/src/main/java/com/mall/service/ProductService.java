package com.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.dto.ProductFormDTO;
import com.mall.dto.ProductQueryDTO;
import com.mall.entity.MerchantTag;
import com.mall.entity.Product;
import com.mall.entity.ProductCollect;
import com.mall.mapper.MerchantTagMapper;
import com.mall.mapper.ProductCollectMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.ProductReviewMapper;
import com.mall.common.UserContext;
import com.mall.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品服务: 浏览 / 详情 / 商家维护 / 库存
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final MerchantTagMapper merchantTagMapper;
    private final ProductCollectMapper collectMapper;
    private final ProductReviewMapper reviewMapper;
    private final CategoryService categoryService;

    /**
     * 商品分页查询 (联表 + 标签/图片富化)
     */
    public IPage<Product> pageProducts(ProductQueryDTO query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 12 : query.getPageSize();
        Page<Product> page = new Page<>(pageNum, pageSize);

        // 分类条件二选一: 包含子分类时用 IN 集合, 否则用单分类等值, 避免条件互相冲突
        Long categoryId = null;
        List<Long> categoryIds = null;
        if (query.getCategoryId() != null) {
            if (Boolean.TRUE.equals(query.getIncludeChildren())) {
                categoryIds = categoryService.selfAndChildrenIds(query.getCategoryId());
            } else {
                categoryId = query.getCategoryId();
            }
        }

        IPage<Product> result = productMapper.selectProductPage(
                page,
                query.getMerchantId(),
                categoryId,
                categoryIds,
                query.getStatus(),
                query.getKeyword() == null ? null : query.getKeyword().trim(),
                query.getTagId(),
                query.getSort());

        enrichList(result.getRecords());
        return result;
    }

    /**
     * 商品详情
     */
    public Product detail(Long id) {
        Product product = productMapper.selectProductDetail(id);
        if (product == null) {
            throw BusinessException.notFound("商品不存在或已下架");
        }
        enrich(product);

        Map<String, Object> summary = reviewMapper.selectScoreSummary(id);
        if (summary != null) {
            Object avg = summary.get("avgScore");
            Object cnt = summary.get("reviewCount");
            product.setAvgScore(avg == null ? 0D : Double.parseDouble(avg.toString()));
            product.setReviewCount(cnt == null ? 0 : Integer.parseInt(cnt.toString()));
        }

        // 已登录用户标记收藏状态
        var loginUser = UserContext.get();
        if (loginUser != null && loginUser.isUser()) {
            Long count = collectMapper.selectCount(Wrappers.<ProductCollect>lambdaQuery()
                    .eq(ProductCollect::getUserId, loginUser.getId())
                    .eq(ProductCollect::getProductId, id));
            product.setCollected(count != null && count > 0);
        } else {
            product.setCollected(false);
        }
        return product;
    }

    /**
     * 热门商品
     */
    public List<Product> hotProducts(int limit) {
        List<Product> list = productMapper.selectHotProducts(limit);
        enrichList(list);
        return list;
    }

    // ---------------------------------------------------------
    // 商家端维护
    // ---------------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    public Product save(Long merchantId, ProductFormDTO form) {
        Product product;
        if (form.getId() == null) {
            product = new Product();
            product.setMerchantId(merchantId);
            product.setSales(0);
        } else {
            product = productMapper.selectById(form.getId());
            if (product == null) {
                throw BusinessException.notFound("商品不存在");
            }
            if (!merchantId.equals(product.getMerchantId())) {
                throw BusinessException.forbidden("无权操作其他商家的商品");
            }
        }
        product.setCategoryId(form.getCategoryId());
        product.setName(form.getName());
        product.setSubTitle(form.getSubTitle());
        product.setMainImage(form.getMainImage());
        product.setImages(JsonUtil.toJson(form.getImages()));
        product.setDetail(form.getDetail());
        product.setPrice(form.getPrice());
        product.setOriginalPrice(form.getOriginalPrice());
        product.setStock(form.getStock());
        product.setUnit(form.getUnit() == null ? "件" : form.getUnit());
        product.setTagIds(JsonUtil.joinLongs(form.getTagIds()));
        product.setStatus(form.getStatus() == null ? 1 : form.getStatus());

        if (form.getId() == null) {
            productMapper.insert(product);
        } else {
            productMapper.updateById(product);
        }
        return product;
    }

    /**
     * 上下架
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long merchantId, Long productId, Integer status) {
        Product product = requireOwned(merchantId, productId);
        product.setStatus(status);
        productMapper.updateById(product);
    }

    /**
     * 删除商品 (逻辑删除)
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long merchantId, Long productId) {
        requireOwned(merchantId, productId);
        productMapper.deleteById(productId);
    }

    /**
     * 按 ID 获取商品 (不存在则抛 404), 不做归属校验, 供平台管理员使用
     */
    public Product byId(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw BusinessException.notFound("商品不存在");
        }
        return product;
    }

    /**
     * 仅更新商品基础字段 (平台管理员改上下架等)
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateBasic(Product product) {
        productMapper.updateById(product);
    }

    /**
     * 校验商品归属当前商家
     */
    public Product requireOwned(Long merchantId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw BusinessException.notFound("商品不存在");
        }
        if (!merchantId.equals(product.getMerchantId())) {
            throw BusinessException.forbidden("无权操作其他商家的商品");
        }
        return product;
    }

    // ---------------------------------------------------------
    // 库存
    // ---------------------------------------------------------

    /**
     * 原子扣减库存, 库存不足抛异常 (调用方需处于事务中)
     */
    public void deductStock(Long productId, Integer quantity) {
        int affected = productMapper.deductStock(productId, quantity);
        if (affected == 0) {
            Product product = productMapper.selectById(productId);
            String name = product == null ? String.valueOf(productId) : product.getName();
            throw new BusinessException("商品「" + name + "」库存不足");
        }
    }

    /**
     * 回滚库存
     */
    public void restoreStock(Long productId, Integer quantity) {
        productMapper.restoreStock(productId, quantity);
    }

    // ---------------------------------------------------------
    // 富化
    // ---------------------------------------------------------

    public void enrichList(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        // 批量取标签, 避免 N+1
        List<Long> merchantIds = products.stream()
                .map(Product::getMerchantId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Map<Long, MerchantTag>> tagCache;
        if (merchantIds.isEmpty()) {
            tagCache = Collections.emptyMap();
        } else {
            List<MerchantTag> tags = merchantTagMapper.selectList(Wrappers.<MerchantTag>lambdaQuery()
                    .in(MerchantTag::getMerchantId, merchantIds));
            tagCache = tags.stream().collect(Collectors.groupingBy(MerchantTag::getMerchantId,
                    Collectors.toMap(MerchantTag::getId, Function.identity(), (a, b) -> a)));
        }
        for (Product p : products) {
            enrichImages(p);
            p.setTags(resolveTags(p, tagCache));
        }
    }

    public void enrich(Product product) {
        if (product == null) {
            return;
        }
        enrichImages(product);
        Map<Long, Map<Long, MerchantTag>> tagCache = Collections.emptyMap();
        if (product.getMerchantId() != null) {
            List<MerchantTag> tags = merchantTagMapper.selectList(Wrappers.<MerchantTag>lambdaQuery()
                    .eq(MerchantTag::getMerchantId, product.getMerchantId()));
            tagCache = Map.of(product.getMerchantId(), tags.stream()
                    .collect(Collectors.toMap(MerchantTag::getId, Function.identity(), (a, b) -> a)));
        }
        product.setTags(resolveTags(product, tagCache));
    }

    private void enrichImages(Product product) {
        List<String> images = JsonUtil.toStringList(product.getImages());
        if (images.isEmpty() && product.getMainImage() != null) {
            images = new ArrayList<>(List.of(product.getMainImage()));
        }
        product.setImageList(images);
    }

    private List<MerchantTag> resolveTags(Product product, Map<Long, Map<Long, MerchantTag>> tagCache) {
        List<Long> tagIds = JsonUtil.toLongList(product.getTagIds());
        if (tagIds.isEmpty() || product.getMerchantId() == null) {
            return Collections.emptyList();
        }
        Map<Long, MerchantTag> byId = tagCache.get(product.getMerchantId());
        if (byId == null) {
            return Collections.emptyList();
        }
        return tagIds.stream().map(byId::get).filter(java.util.Objects::nonNull).collect(Collectors.toList());
    }
}
