package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

    /**
     * 订单分页查询 (联表带出店铺名)
     */
    @Select("""
            <script>
            SELECT o.*, m.store_name AS store_name
            FROM orders o
            LEFT JOIN merchant m ON m.id = o.merchant_id
            WHERE o.del_flag = 0
            <if test='userId != null'> AND o.user_id = #{userId} </if>
            <if test='merchantId != null'> AND o.merchant_id = #{merchantId} </if>
            <if test='status != null'> AND o.status = #{status} </if>
            <if test='keyword != null and keyword != ""'>
                AND (o.order_no LIKE CONCAT('%', #{keyword}, '%')
                     OR o.receiver_name LIKE CONCAT('%', #{keyword}, '%')
                     OR o.receiver_phone LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY o.create_time DESC
            </script>
            """)
    IPage<Orders> selectOrderPage(@Param("page") Page<Orders> page,
                                  @Param("userId") Long userId,
                                  @Param("merchantId") Long merchantId,
                                  @Param("status") Integer status,
                                  @Param("keyword") String keyword);

    /**
     * 按状态统计订单数量
     */
    @Select("""
            <script>
            SELECT COUNT(*) FROM orders
            WHERE del_flag = 0
            <if test='userId != null'> AND user_id = #{userId} </if>
            <if test='merchantId != null'> AND merchant_id = #{merchantId} </if>
            <if test='status != null'> AND status = #{status} </if>
            </script>
            """)
    long countByStatus(@Param("userId") Long userId,
                       @Param("merchantId") Long merchantId,
                       @Param("status") Integer status);

    /**
     * 近 N 天销售额趋势 (按日聚合), status 为已完成/待收货视为有效成交
     */
    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS day, IFNULL(SUM(pay_amount), 0) AS amount, COUNT(*) AS cnt
            FROM orders
            WHERE del_flag = 0 AND status IN (2, 3) AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
            GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
            ORDER BY day ASC
            """)
    List<java.util.Map<String, Object>> selectSalesTrend(@Param("days") int days);

    /**
     * 商品销量排行 (按订单明细聚合)
     */
    @Select("""
            SELECT oi.product_id AS productId, oi.product_name AS productName,
                   SUM(oi.quantity) AS totalQuantity, SUM(oi.total_price) AS totalAmount
            FROM order_item oi
            INNER JOIN orders o ON o.id = oi.order_id
            WHERE o.del_flag = 0 AND o.status IN (2, 3)
            GROUP BY oi.product_id, oi.product_name
            ORDER BY totalQuantity DESC
            LIMIT #{limit}
            """)
    List<java.util.Map<String, Object>> selectProductRanking(@Param("limit") int limit);

    /**
     * 商家维度的商品销量排行
     */
    @Select("""
            SELECT oi.product_id AS productId, oi.product_name AS productName,
                   SUM(oi.quantity) AS totalQuantity, SUM(oi.total_price) AS totalAmount
            FROM order_item oi
            INNER JOIN orders o ON o.id = oi.order_id
            WHERE o.del_flag = 0 AND o.status IN (2, 3) AND o.merchant_id = #{merchantId}
            GROUP BY oi.product_id, oi.product_name
            ORDER BY totalQuantity DESC
            LIMIT #{limit}
            """)
    List<java.util.Map<String, Object>> selectMerchantProductRanking(@Param("merchantId") Long merchantId,
                                                                     @Param("limit") int limit);

    /**
     * 商家销售概览: 成交额 / 订单数
     */
    @Select("""
            SELECT IFNULL(SUM(pay_amount), 0) AS totalAmount, COUNT(*) AS totalCount
            FROM orders
            WHERE del_flag = 0 AND merchant_id = #{merchantId} AND status IN (2, 3)
            """)
    java.util.Map<String, Object> selectMerchantSalesSummary(@Param("merchantId") Long merchantId);

    /**
     * 平台销售概览 (全站), 注意不能传 null 给 merchant_id 做等值比较
     */
    @Select("""
            SELECT IFNULL(SUM(pay_amount), 0) AS totalAmount, COUNT(*) AS totalCount
            FROM orders
            WHERE del_flag = 0 AND status IN (2, 3)
            """)
    java.util.Map<String, Object> selectPlatformSalesSummary();
}
