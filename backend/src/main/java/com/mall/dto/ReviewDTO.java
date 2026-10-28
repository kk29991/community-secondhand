package com.mall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 提交评价 */
@Data
public class ReviewDTO {

    @NotNull(message = "订单明细ID不能为空")
    private Long orderItemId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低 1 分")
    @Max(value = 5, message = "评分最高 5 分")
    private Integer score;

    private String content;

    private List<String> images;

    private Integer anonymous = 0;
}
