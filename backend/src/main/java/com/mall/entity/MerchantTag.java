package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 商家标签 (如: 服务体验优秀 / 细节处理得当) */
@Data
@TableName("merchant_tag")
public class MerchantTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    private String name;

    private String color;

    private Integer sort;

    @TableLogic
    @TableField(select = false)
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
