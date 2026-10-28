package com.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.BusinessException;
import com.mall.entity.MerchantTag;
import com.mall.entity.Product;
import com.mall.mapper.MerchantTagMapper;
import com.mall.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商家标签服务 (如「服务体验优秀」「细节处理得当」)
 */
@Service
@RequiredArgsConstructor
public class MerchantTagService {

    private final MerchantTagMapper tagMapper;
    private final ProductMapper productMapper;

    /**
     * 查询商家的标签
     */
    public List<MerchantTag> listByMerchant(Long merchantId) {
        return tagMapper.selectList(Wrappers.<MerchantTag>lambdaQuery()
                .eq(MerchantTag::getMerchantId, merchantId)
                .orderByAsc(MerchantTag::getSort)
                .orderByAsc(MerchantTag::getId));
    }

    /**
     * 前台: 按商家ID批量查询标签
     */
    public List<MerchantTag> listByMerchantIds(List<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return List.of();
        }
        return tagMapper.selectList(Wrappers.<MerchantTag>lambdaQuery()
                .in(MerchantTag::getMerchantId, merchantIds)
                .orderByAsc(MerchantTag::getSort));
    }

    @Transactional(rollbackFor = Exception.class)
    public MerchantTag save(Long merchantId, MerchantTag tag) {
        if (tag.getName() == null || tag.getName().isBlank()) {
            throw new BusinessException("标签名称不能为空");
        }
        tag.setMerchantId(merchantId);
        if (tag.getSort() == null) {
            tag.setSort(0);
        }
        if (tag.getId() == null) {
            tagMapper.insert(tag);
        } else {
            MerchantTag existing = tagMapper.selectById(tag.getId());
            if (existing == null || !merchantId.equals(existing.getMerchantId())) {
                throw BusinessException.forbidden("无权操作该标签");
            }
            tagMapper.updateById(tag);
        }
        return tag;
    }

    /**
     * 删除标签, 并从已关联商品的 tag_ids 中移除
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long merchantId, Long tagId) {
        MerchantTag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw BusinessException.notFound("标签不存在");
        }
        if (!merchantId.equals(tag.getMerchantId())) {
            throw BusinessException.forbidden("无权操作该标签");
        }
        tagMapper.deleteById(tagId);

        // 清理商品上对该标签的引用
        List<Product> products = productMapper.selectList(Wrappers.<Product>lambdaQuery()
                .eq(Product::getMerchantId, merchantId)
                .apply("FIND_IN_SET({0}, tag_ids) > 0", tagId));
        for (Product product : products) {
            List<Long> remaining = com.mall.util.JsonUtil.toLongList(product.getTagIds()).stream()
                    .filter(id -> !id.equals(tagId))
                    .toList();
            product.setTagIds(com.mall.util.JsonUtil.joinLongs(remaining));
            productMapper.updateById(product);
        }
    }
}
