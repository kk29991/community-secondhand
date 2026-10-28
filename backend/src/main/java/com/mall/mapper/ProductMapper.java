package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 商品分页查询 (联表带出分类名与店铺名)
     */
    @Select("""
            <script>
            SELECT p.*, c.name AS category_name, m.store_name AS merchant_name, m.store_logo AS store_logo
            FROM product p
            LEFT JOIN category c ON c.id = p.category_id
            LEFT JOIN merchant m ON m.id = p.merchant_id
            WHERE p.del_flag = 0
            <if test='merchantId != null'> AND p.merchant_id = #{merchantId} </if>
            <if test='categoryId != null'> AND p.category_id = #{categoryId} </if>
            <if test='categoryIds != null and categoryIds.size() > 0'>
                AND p.category_id IN
                <foreach collection='categoryIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach>
            </if>
            <if test='status != null'> AND p.status = #{status} </if>
            <if test='keyword != null and keyword != ""'>
                AND (p.name LIKE CONCAT('%', #{keyword}, '%') OR p.sub_title LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='tagId != null'> AND FIND_IN_SET(#{tagId}, p.tag_ids) </if>
            ORDER BY
            <if test='keyword != null and keyword != ""'>
                <!--
                  关键词检索时按相关度排序: 名称命中优先于副标题命中。
                  原因: 副标题里的关键词常属"顺带提及", 会造成明显误命中。
                  例如搜「手机」时, 打印机的副标题「黑白打印 手机无线直连」也含"手机",
                  按时间排序会把打印机排到真手机前面。这里让名称命中排在最前。
                  名称命中内部仍用时间倒序, 便于看到最新发布的闲置。
                -->
                CASE WHEN p.name LIKE CONCAT('%', #{keyword}, '%') THEN 0 ELSE 1 END ASC,
                p.create_time DESC
            </if>
            <if test='keyword == null or keyword == ""'>
                <choose>
                    <when test='sort == "priceAsc"'> p.price ASC </when>
                    <when test='sort == "priceDesc"'> p.price DESC </when>
                    <when test='sort == "sales"'> p.sales DESC </when>
                    <otherwise> p.create_time DESC </otherwise>
                </choose>
            </if>
            </script>
            """)
    IPage<Product> selectProductPage(@Param("page") Page<Product> page,
                                     @Param("merchantId") Long merchantId,
                                     @Param("categoryId") Long categoryId,
                                     @Param("categoryIds") List<Long> categoryIds,
                                     @Param("status") Integer status,
                                     @Param("keyword") String keyword,
                                     @Param("tagId") Long tagId,
                                     @Param("sort") String sort);

    /**
     * 查询商品详情 (含店铺信息), 不受上架状态限制
     */
    @Select("""
            SELECT p.*, c.name AS category_name, m.store_name AS merchant_name, m.store_logo AS store_logo
            FROM product p
            LEFT JOIN category c ON c.id = p.category_id
            LEFT JOIN merchant m ON m.id = p.merchant_id
            WHERE p.id = #{id} AND p.del_flag = 0
            """)
    Product selectProductDetail(@Param("id") Long id);

    /**
     * 销量前 N 的商品 (首页热门)
     */
    @Select("""
            SELECT p.*, m.store_name AS merchant_name
            FROM product p
            LEFT JOIN merchant m ON m.id = p.merchant_id
            WHERE p.del_flag = 0 AND p.status = 1
            ORDER BY p.sales DESC, p.id DESC
            LIMIT #{limit}
            """)
    List<Product> selectHotProducts(@Param("limit") int limit);

    /**
     * 原子扣减库存并累加销量。
     * WHERE 中带 stock >= quantity, 由数据库保证不会超卖;
     * 返回受影响行数为 0 表示库存不足, 由调用方抛出业务异常触发回滚。
     */
    @Update("""
            UPDATE product
            SET stock = stock - #{quantity}, sales = sales + #{quantity}
            WHERE id = #{id} AND del_flag = 0 AND stock >= #{quantity}
            """)
    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 取消订单时回滚库存与销量
     */
    @Update("""
            UPDATE product
            SET stock = stock + #{quantity},
                sales = CASE WHEN sales >= #{quantity} THEN sales - #{quantity} ELSE 0 END
            WHERE id = #{id}
            """)
    int restoreStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
