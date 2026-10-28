package com.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.entity.Orders;
import com.mall.entity.Product;
import com.mall.mapper.MerchantMapper;
import com.mall.mapper.OrderMapper;
import com.mall.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家中心服务: 数据看板 / 销售统计
 */
@Service
@RequiredArgsConstructor
public class MerchantCenterService {

    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;
    private final ReviewService reviewService;

    /**
     * 商家数据看板
     */
    public Map<String, Object> dashboard(Long merchantId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("productCount", productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getMerchantId, merchantId)));
        map.put("onSaleCount", productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getMerchantId, merchantId)
                .eq(Product::getStatus, 1)));
        map.put("offSaleCount", productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getMerchantId, merchantId)
                .eq(Product::getStatus, 0)));

        map.put("orderCount", orderMapper.countByStatus(null, merchantId, null));
        map.put("unpaidCount", orderMapper.countByStatus(null, merchantId, Orders.STATUS_UNPAID));
        map.put("undeliveredCount", orderMapper.countByStatus(null, merchantId, Orders.STATUS_UNDELIVERED));
        map.put("unreceivedCount", orderMapper.countByStatus(null, merchantId, Orders.STATUS_UNRECEIVED));
        map.put("finishedCount", orderMapper.countByStatus(null, merchantId, Orders.STATUS_FINISHED));

        Map<String, Object> sales = orderMapper.selectMerchantSalesSummary(merchantId);
        map.put("totalSales", sales == null ? 0 : sales.get("totalAmount"));
        map.put("dealCount", sales == null ? 0 : sales.get("totalCount"));

        map.put("review", reviewService.merchantSummary(merchantId));

        var merchant = merchantMapper.selectById(merchantId);
        map.put("storeName", merchant == null ? "" : merchant.getStoreName());
        map.put("storeLogo", merchant == null ? "" : merchant.getStoreLogo());
        return map;
    }

    /**
     * 商家销售统计: 趋势 + 商品排行
     */
    public Map<String, Object> salesStatistics(Long merchantId, int days, int topN) {
        Map<String, Object> map = new LinkedHashMap<>();
        // 趋势按全站口径暂无商家维度, 这里用商家排行 + 概览替代
        List<Map<String, Object>> ranking = orderMapper.selectMerchantProductRanking(merchantId, topN);
        Map<String, Object> sales = orderMapper.selectMerchantSalesSummary(merchantId);

        map.put("totalSales", sales == null ? 0 : sales.get("totalAmount"));
        map.put("totalOrders", sales == null ? 0 : sales.get("totalCount"));
        map.put("productRanking", ranking);
        map.put("days", days);
        return map;
    }
}
