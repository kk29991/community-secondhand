package com.mall.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 修改购物车项 */
@Data
public class CartUpdateDTO {

    @NotNull(message = "购物车项ID不能为空")
    private Long id;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;
}
