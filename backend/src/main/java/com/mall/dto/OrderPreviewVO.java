package com.mall.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结算预览结果 (按店铺分组)
 */
@Data
public class OrderPreviewVO {

    /** 按店铺拆分的订单组 */
    private List<Group> groups;

    /** 全部合计金额 */
    private BigDecimal totalAmount;

    /** 是否来自购物车(用于下单后清理购物车) */
    private Boolean fromCart;

    @Data
    public static class Group {
        private Long merchantId;
        private String storeName;
        private List<Item> items;
        private BigDecimal amount;
    }

    @Data
    public static class Item {
        private Long cartId;
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal totalPrice;
        private Boolean invalid;
        private Integer stock;
    }
}
