package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 查询某个用户待评价的订单明细 (已完成且未评价)
     */
    @Select("""
            SELECT oi.*, o.merchant_id AS merchant_id, o.order_no AS order_no, o.create_time AS order_create_time
            FROM order_item oi
            INNER JOIN orders o ON o.id = oi.order_id
            WHERE o.user_id = #{userId} AND o.status = 3 AND o.del_flag = 0 AND oi.commented = 0
            ORDER BY o.create_time DESC
            """)
    List<OrderItem> selectPendingReviewItems(@Param("userId") Long userId);
}
