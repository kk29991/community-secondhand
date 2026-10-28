package com.mall.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 商品新增/编辑表单 */
@Data
public class ProductFormDTO {

    /** 编辑时必填 */
    private Long id;

    @NotNull(message = "请选择商品分类")
    private Long categoryId;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String subTitle;

    @NotBlank(message = "请上传商品主图")
    private String mainImage;

    /** 轮播图列表 */
    private List<String> images;

    private String detail;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.01", message = "售价必须大于 0")
    private BigDecimal price;

    private BigDecimal originalPrice;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    private String unit = "件";

    /** 关联标签ID列表 */
    private List<Long> tagIds;

    /** 0下架 1上架 */
    private Integer status = 1;
}
