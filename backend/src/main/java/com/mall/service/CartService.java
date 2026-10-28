package com.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.BusinessException;
import com.mall.dto.CartAddDTO;
import com.mall.dto.CartUpdateDTO;
import com.mall.entity.Cart;
import com.mall.entity.Product;
import com.mall.mapper.CartMapper;
import com.mall.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车服务
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;

    /**
     * 我的购物车
     */
    public List<Cart> list(Long userId) {
        List<Cart> items = cartMapper.selectCartList(userId);
        for (Cart item : items) {
            if (Boolean.TRUE.equals(item.getInvalid())) {
                item.setTotalPrice(BigDecimal.ZERO);
                continue;
            }
            BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            item.setTotalPrice(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return items;
    }

    /**
     * 加入购物车: 已存在则累加数量
     */
    @Transactional(rollbackFor = Exception.class)
    public void add(Long userId, CartAddDTO dto) {
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw BusinessException.notFound("商品不存在");
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException("商品已下架");
        }
        Integer quantity = dto.getQuantity() == null ? 1 : dto.getQuantity();

        Cart existing = cartMapper.selectOne(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, dto.getProductId()));

        int finalQuantity = (existing == null ? 0 : existing.getQuantity()) + quantity;
        if (product.getStock() != null && finalQuantity > product.getStock()) {
            throw new BusinessException("库存不足, 当前仅剩 " + product.getStock() + " 件");
        }

        if (existing == null) {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(dto.getProductId());
            cart.setQuantity(quantity);
            cart.setChecked(1);
            cartMapper.insert(cart);
        } else {
            existing.setQuantity(finalQuantity);
            cartMapper.updateById(existing);
        }
    }

    /**
     * 修改数量
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(Long userId, CartUpdateDTO dto) {
        Cart cart = requireOwned(userId, dto.getId());
        Product product = productMapper.selectById(cart.getProductId());
        if (product != null && product.getStock() != null && dto.getQuantity() > product.getStock()) {
            throw new BusinessException("库存不足, 当前仅剩 " + product.getStock() + " 件");
        }
        cart.setQuantity(dto.getQuantity());
        cartMapper.updateById(cart);
    }

    /**
     * 勾选 / 取消勾选
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateChecked(Long userId, Long cartId, Integer checked) {
        Cart cart = requireOwned(userId, cartId);
        cart.setChecked(checked == null || checked == 0 ? 0 : 1);
        cartMapper.updateById(cart);
    }

    /**
     * 全选 / 全不选
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAllChecked(Long userId, Integer checked) {
        Cart update = new Cart();
        update.setChecked(checked == null || checked == 0 ? 0 : 1);
        cartMapper.update(update, Wrappers.<Cart>lambdaUpdate().eq(Cart::getUserId, userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, List<Long> cartIds) {
        if (cartIds == null || cartIds.isEmpty()) {
            return;
        }
        cartMapper.delete(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId)
                .in(Cart::getId, cartIds));
    }

    /**
     * 清空购物车
     */
    @Transactional(rollbackFor = Exception.class)
    public void clear(Long userId) {
        cartMapper.delete(Wrappers.<Cart>lambdaQuery().eq(Cart::getUserId, userId));
    }

    /**
     * 统计购物车商品件数 (用于顶部角标)
     */
    public int count(Long userId) {
        List<Cart> items = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId));
        return items.stream().mapToInt(c -> c.getQuantity() == null ? 0 : c.getQuantity()).sum();
    }

    private Cart requireOwned(Long userId, Long cartId) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null) {
            throw BusinessException.notFound("购物车项不存在");
        }
        if (!userId.equals(cart.getUserId())) {
            throw BusinessException.forbidden("无权操作他人的购物车");
        }
        return cart;
    }
}
