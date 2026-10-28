package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 商品收藏 */
@Data
@TableName("product_collect")
public class ProductCollect {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long productId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ---------- 非数据库字段 ----------

    /** 收藏列表展示的商品信息 */
    @TableField(exist = false)
    private Product product;
}
