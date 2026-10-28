package com.mall.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建订单
 * <p>
 * 两种下单来源:
 * 1. 购物车结算: cartIds 非空, 按商家自动拆单
 * 2. 立即购买: 传 productId + quantity
 */
@Data
public class OrderCreateDTO {

    @NotNull(message = "请选择收货地址")
    private Long addressId;

    /** 购物车项ID列表 */
    private List<Long> cartIds;

    /** 立即购买: 商品ID */
    private Long productId;

    /** 立即购买: 数量 */
    private Integer quantity;

    /** 支付方式: 1余额 2支付宝 3微信 */
    private Integer payType = 1;

    private String remark;

    /** 是否直接支付(模拟支付), 默认 true 以便跑通闭环 */
    private Boolean payImmediately = Boolean.TRUE;
}
