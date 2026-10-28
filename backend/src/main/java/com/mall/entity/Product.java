package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 商品 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    private Long categoryId;

    private String name;

    /** 副标题 / 卖点 */
    private String subTitle;

    private String mainImage;

    /** 轮播图, JSON 数组字符串 */
    private String images;

    /** 商品详情富文本 */
    private String detail;

    private BigDecimal price;

    /** 原价 / 划线价 */
    private BigDecimal originalPrice;

    private Integer stock;

    private Integer sales;

    private String unit;

    /** 关联商家标签ID, 逗号分隔 */
    private String tagIds;

    /** 0下架 1上架 */
    private Integer status;

    @TableLogic
    @TableField(select = false)
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ---------- 非数据库字段: 列表展示用 ----------

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String merchantName;

    @TableField(exist = false)
    private String storeLogo;

    /** 解析后的轮播图列表 */
    @TableField(exist = false)
    private List<String> imageList;

    /** 解析后的标签列表 */
    @TableField(exist = false)
    private List<MerchantTag> tags;

    /** 当前登录用户是否已收藏 */
    @TableField(exist = false)
    private Boolean collected;

    /** 平均评分 */
    @TableField(exist = false)
    private Double avgScore;

    /** 评价数量 */
    @TableField(exist = false)
    private Integer reviewCount;
}
