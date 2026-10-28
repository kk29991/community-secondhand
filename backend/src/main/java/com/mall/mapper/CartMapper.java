package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    /**
     * 购物车列表 (联表商品信息, 按店铺分组结算需要 merchantId)
     */
    @Select("""
            SELECT c.*, p.name AS product_name, p.main_image AS product_image, p.price AS price,
                   p.stock AS stock, p.merchant_id AS merchant_id, m.store_name AS merchant_name,
                   CASE WHEN p.id IS NULL OR p.del_flag = 1 OR p.status = 0 THEN 1 ELSE 0 END AS invalid
            FROM cart c
            LEFT JOIN product p ON p.id = c.product_id
            LEFT JOIN merchant m ON m.id = p.merchant_id
            WHERE c.user_id = #{userId}
            ORDER BY c.create_time DESC
            """)
    List<Cart> selectCartList(@Param("userId") Long userId);
}
