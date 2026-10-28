package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 购物车项 */
@Data
@TableName("cart")
public class Cart {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long productId;

    private Integer quantity;

    /** 0未勾选 1已勾选 */
    private Integer checked;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ---------- 非数据库字段: 购物车列表展示 ----------

    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String productImage;

    @TableField(exist = false)
    private BigDecimal price;

    @TableField(exist = false)
    private Integer stock;

    /** 商品是否已下架/删除, 前端据此置灰 */
    @TableField(exist = false)
    private Boolean invalid;

    /** 所属商家ID, 用于按店铺分组结算 */
    @TableField(exist = false)
    private Long merchantId;

    @TableField(exist = false)
    private String merchantName;

    /** 小计 = price * quantity */
    @TableField(exist = false)
    private BigDecimal totalPrice;
}
