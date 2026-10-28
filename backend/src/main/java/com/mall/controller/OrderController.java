package com.mall.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.common.Result;
import com.mall.common.UserContext;
import com.mall.dto.OrderCreateDTO;
import com.mall.dto.OrderPreviewVO;
import com.mall.entity.Orders;
import com.mall.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * 订单接口 (买家侧, 需登录): 结算预览 / 下单 / 支付 / 取消 / 收货
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 结算预览: 按店铺分组 */
    @GetMapping("/preview")
    public Result<OrderPreviewVO> preview(@RequestParam(required = false) List<Long> cartIds,
                                          @RequestParam(required = false) Long productId,
                                          @RequestParam(required = false) Integer quantity) {
        return Result.success(orderService.preview(UserContext.requireId(), cartIds, productId, quantity));
    }

    /** 提交订单 */
    @PostMapping
    public Result<List<Orders>> create(@Valid @RequestBody OrderCreateDTO dto) {
        List<Orders> orders = orderService.create(UserContext.requireId(), dto);
        return Result.success("下单成功", orders);
    }

    /** 支付订单 (模拟) */
    @PutMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id, @RequestParam(required = false) Integer payType) {
        orderService.pay(UserContext.requireId(), id, payType);
        return Result.success("支付成功", null);
    }

    /** 取消订单 */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        orderService.cancel(UserContext.requireId(), id, reason);
        return Result.success("订单已取消", null);
    }

    /** 确认收货 */
    @PutMapping("/{id}/receive")
    public Result<Void> receive(@PathVariable Long id) {
        orderService.receive(UserContext.requireId(), id);
        return Result.success("已确认收货", null);
    }

    /** 申请退款 */
    @PutMapping("/{id}/refund")
    public Result<Void> refund(@PathVariable Long id, @RequestParam(required = false) String reason) {
        orderService.refund(UserContext.requireId(), id, reason);
        return Result.success("退款申请已受理", null);
    }

    /** 我的订单 */
    @GetMapping("/list")
    public Result<IPage<Orders>> list(@RequestParam(required = false) Integer status,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "1") int pageNum,
                                      @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(orderService.page(UserContext.requireId(), null, status, keyword, pageNum, pageSize));
    }

    /** 订单详情 */
    @GetMapping("/{id}")
    public Result<Orders> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    /** 各状态订单数量 */
    @GetMapping("/counts")
    public Result<Map<String, Long>> counts() {
        return Result.success(orderService.statusCount(UserContext.requireId(), null));
    }
}
