package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 商品评价 */
@Data
@TableName("product_review")
public class ProductReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Long orderId;

    private Long orderItemId;

    private Long userId;

    private Long merchantId;

    /** 1-5 分 */
    private Integer score;

    private String content;

    /** 评价图片, JSON 数组字符串 */
    private String images;

    /** 0否 1是 */
    private Integer anonymous;

    /** 商家回复内容 */
    private String replyContent;

    private LocalDateTime replyTime;

    /** 0隐藏 1显示 */
    private Integer status;

    @TableLogic
    @TableField(select = false)
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ---------- 非数据库字段 ----------

    @TableField(exist = false)
    private String nickname;

    @TableField(exist = false)
    private String avatar;

    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String productImage;

    /** 匿名时展示的脱敏昵称 */
    @TableField(exist = false)
    private String displayName;

    @TableField(exist = false)
    private List<String> imageList;
}
