package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 商品分类 */
@Data
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父分类ID, 0 为顶级 */
    private Long parentId;

    private String name;

    private String icon;

    private Integer sort;

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

    /** 子分类, 仅树形查询时填充 */
    @TableField(exist = false)
    private java.util.List<Category> children;
}
