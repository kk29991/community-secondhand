package com.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.BusinessException;
import com.mall.entity.Category;
import com.mall.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品分类服务
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    /**
     * 查询全部分类(平铺)
     *
     * @param onlyEnabled 是否只返回显示中的分类
     */
    public List<Category> listAll(boolean onlyEnabled) {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(onlyEnabled, Category::getStatus, 1)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    /**
     * 分类树 (前台导航使用)
     */
    public List<Category> tree(boolean onlyEnabled) {
        List<Category> all = listAll(onlyEnabled);
        Map<Long, Category> byId = all.stream()
                .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a, LinkedHashMap::new));
        List<Category> roots = new ArrayList<>();
        for (Category c : all) {
            Long parentId = c.getParentId() == null ? 0L : c.getParentId();
            if (parentId == 0L) {
                roots.add(c);
                continue;
            }
            Category parent = byId.get(parentId);
            if (parent == null) {
                // 父分类被隐藏时, 子分类不再展示
                continue;
            }
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(c);
        }
        return roots;
    }

    /**
     * 查询分类自身及其所有子分类ID (用于按一级分类筛选商品)
     */
    public List<Long> selfAndChildrenIds(Long categoryId) {
        if (categoryId == null) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        result.add(categoryId);
        List<Category> children = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(Category::getParentId, categoryId));
        children.forEach(c -> result.add(c.getId()));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Category save(Category category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new BusinessException("分类名称不能为空");
        }
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        if (category.getId() == null) {
            categoryMapper.insert(category);
        } else {
            if (category.getId().equals(category.getParentId())) {
                throw new BusinessException("父分类不能是自己");
            }
            categoryMapper.updateById(category);
        }
        return category;
    }

    /**
     * 删除分类: 存在子分类或已关联商品时禁止删除
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long childCount = categoryMapper.selectCount(
                Wrappers.<Category>lambdaQuery().eq(Category::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException("该分类下存在子分类, 请先删除子分类");
        }
        categoryMapper.deleteById(id);
    }

    /** 名称映射, 供其他服务填充展示字段 */
    public Map<Long, String> idNameMap() {
        return listAll(false).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }
}
