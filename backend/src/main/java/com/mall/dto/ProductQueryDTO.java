package com.mall.dto;

import lombok.Data;

/** 商品查询条件 */
@Data
public class ProductQueryDTO {

    /** 页码, 从 1 开始 */
    private Integer pageNum = 1;

    private Integer pageSize = 12;

    /** 分类ID */
    private Long categoryId;

    /** 是否包含子分类商品 */
    private Boolean includeChildren = Boolean.TRUE;

    /** 商家ID */
    private Long merchantId;

    /** 上架状态, 后台管理使用 */
    private Integer status;

    /** 关键词 */
    private String keyword;

    /** 标签ID */
    private Long tagId;

    /** 排序: default / priceAsc / priceDesc / sales */
    private String sort = "default";
}
