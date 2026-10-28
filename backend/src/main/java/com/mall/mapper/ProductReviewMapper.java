package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {

    /**
     * 评价分页查询 (联表带出用户与商品信息)
     */
    @Select("""
            <script>
            SELECT r.*, u.nickname AS nickname, u.avatar AS avatar,
                   p.name AS product_name, p.main_image AS product_image
            FROM product_review r
            LEFT JOIN `user` u ON u.id = r.user_id
            LEFT JOIN product p ON p.id = r.product_id
            WHERE r.del_flag = 0
            <if test='productId != null'> AND r.product_id = #{productId} </if>
            <if test='merchantId != null'> AND r.merchant_id = #{merchantId} </if>
            <if test='userId != null'> AND r.user_id = #{userId} </if>
            <if test='hasReply != null and hasReply == true'> AND r.reply_content IS NOT NULL AND r.reply_content != '' </if>
            <if test='hasReply != null and hasReply == false'> AND (r.reply_content IS NULL OR r.reply_content = '') </if>
            <if test='status != null'> AND r.status = #{status} </if>
            ORDER BY r.create_time DESC
            </script>
            """)
    IPage<ProductReview> selectReviewPage(@Param("page") Page<ProductReview> page,
                                          @Param("productId") Long productId,
                                          @Param("merchantId") Long merchantId,
                                          @Param("userId") Long userId,
                                          @Param("hasReply") Boolean hasReply,
                                          @Param("status") Integer status);

    /**
     * 商品平均评分与评价数
     */
    @Select("""
            SELECT IFNULL(AVG(score), 0) AS avgScore, COUNT(*) AS reviewCount
            FROM product_review
            WHERE del_flag = 0 AND status = 1 AND product_id = #{productId}
            """)
    java.util.Map<String, Object> selectScoreSummary(@Param("productId") Long productId);
}
