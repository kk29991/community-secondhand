package com.mall.controller;

import com.mall.common.Result;
import com.mall.common.UserContext;
import com.mall.dto.CartAddDTO;
import com.mall.dto.CartUpdateDTO;
import com.mall.entity.Cart;
import com.mall.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车接口 (需登录)
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** 我的购物车 */
    @GetMapping("/list")
    public Result<List<Cart>> list() {
        return Result.success(cartService.list(UserContext.requireId()));
    }

    /** 购物车商品件数 */
    @GetMapping("/count")
    public Result<Integer> count() {
        return Result.success(cartService.count(UserContext.requireId()));
    }

    /** 加入购物车 */
    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody CartAddDTO dto) {
        cartService.add(UserContext.requireId(), dto);
        return Result.success("已加入购物车", null);
    }

    /** 修改数量 */
    @PutMapping("/quantity")
    public Result<Void> updateQuantity(@Valid @RequestBody CartUpdateDTO dto) {
        cartService.updateQuantity(UserContext.requireId(), dto);
        return Result.success();
    }

    /** 勾选 / 取消勾选 */
    @PutMapping("/checked")
    public Result<Void> updateChecked(@RequestParam Long id, @RequestParam Integer checked) {
        cartService.updateChecked(UserContext.requireId(), id, checked);
        return Result.success();
    }

    /** 全选 / 全不选 */
    @PutMapping("/checked/all")
    public Result<Void> updateAllChecked(@RequestParam Integer checked) {
        cartService.updateAllChecked(UserContext.requireId(), checked);
        return Result.success();
    }

    /** 删除购物车项 */
    @DeleteMapping("/remove")
    public Result<Void> remove(@RequestBody List<Long> cartIds) {
        cartService.remove(UserContext.requireId(), cartIds);
        return Result.success("已移除", null);
    }

    /** 清空购物车 */
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        cartService.clear(UserContext.requireId());
        return Result.success("购物车已清空", null);
    }
}
