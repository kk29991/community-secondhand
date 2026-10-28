package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单明细 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long productId;

    /** 下单时的商品快照 */
    private String productName;

    private String productImage;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal totalPrice;

    /** 0未评价 1已评价 */
    private Integer commented;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ---------- 非数据库字段 ----------

    @TableField(exist = false)
    private Long merchantId;

    @TableField(exist = false)
    private String orderNo;

    @TableField(exist = false)
    private LocalDateTime orderCreateTime;
}
