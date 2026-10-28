package com.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.common.UserContext;
import com.mall.dto.OrderCreateDTO;
import com.mall.dto.OrderPreviewVO;
import com.mall.entity.Address;
import com.mall.entity.Cart;
import com.mall.entity.OrderItem;
import com.mall.entity.Orders;
import com.mall.entity.Product;
import com.mall.mapper.AddressMapper;
import com.mall.mapper.CartMapper;
import com.mall.mapper.OrderItemMapper;
import com.mall.mapper.OrderMapper;
import com.mall.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单服务: 结算预览 / 下单 / 支付 / 取消 / 商家发货 / 确认收货
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final AddressMapper addressMapper;
    private final CartMapper cartMapper;
    private final ProductService productService;

    /** 运费规则: 单笔订单满 99 元免运费, 否则 10 元 */
    private static final BigDecimal FREE_FREIGHT_THRESHOLD = new BigDecimal("99");
    private static final BigDecimal FREIGHT = new BigDecimal("10.00");

    // ---------------------------------------------------------
    // 结算预览
    // ---------------------------------------------------------

    public OrderPreviewVO preview(Long userId, List<Long> cartIds, Long productId, Integer quantity) {
        List<Cart> cartItems = new ArrayList<>();
        boolean fromCart = true;

        if (cartIds != null && !cartIds.isEmpty()) {
            cartItems = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                    .eq(Cart::getUserId, userId)
                    .in(Cart::getId, cartIds));
            if (cartItems.isEmpty()) {
                throw new BusinessException("请先选择要结算的商品");
            }
        } else if (productId != null) {
            fromCart = false;
            Product product = productMapper.selectById(productId);
            if (product == null) {
                throw BusinessException.notFound("商品不存在");
            }
            Cart temp = new Cart();
            temp.setProductId(productId);
            temp.setQuantity(quantity == null || quantity < 1 ? 1 : quantity);
            cartItems.add(temp);
        } else {
            throw new BusinessException("请先选择要结算的商品");
        }

        // 读取商品信息并校验
        List<OrderPreviewVO.Item> items = new ArrayList<>();
        for (Cart cart : cartItems) {
            Product product = productMapper.selectById(cart.getProductId());
            OrderPreviewVO.Item item = new OrderPreviewVO.Item();
            item.setCartId(cart.getId());
            item.setProductId(cart.getProductId());
            item.setQuantity(cart.getQuantity());
            if (product == null) {
                item.setProductName("商品已删除");
                item.setInvalid(true);
                item.setPrice(BigDecimal.ZERO);
                item.setTotalPrice(BigDecimal.ZERO);
                items.add(item);
                continue;
            }
            item.setProductName(product.getName());
            item.setProductImage(product.getMainImage());
            item.setPrice(product.getPrice());
            item.setStock(product.getStock());
            item.setInvalid(product.getStatus() == null || product.getStatus() != 1);
            item.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            items.add(item);
        }

        // 过滤掉失效商品
        List<OrderPreviewVO.Item> validItems = items.stream()
                .filter(i -> !Boolean.TRUE.equals(i.getInvalid()))
                .collect(Collectors.toList());
        if (validItems.isEmpty()) {
            throw new BusinessException("所选商品均已下架, 无法结算");
        }

        // 按商家分组
        Map<Long, List<OrderPreviewVO.Item>> byMerchant = new LinkedHashMap<>();
        for (OrderPreviewVO.Item item : validItems) {
            Product product = productMapper.selectById(item.getProductId());
            byMerchant.computeIfAbsent(product.getMerchantId(), k -> new ArrayList<>()).add(item);
        }

        List<OrderPreviewVO.Group> groups = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, List<OrderPreviewVO.Item>> entry : byMerchant.entrySet()) {
            OrderPreviewVO.Group group = new OrderPreviewVO.Group();
            group.setMerchantId(entry.getKey());
            group.setItems(entry.getValue());
            BigDecimal amount = entry.getValue().stream()
                    .map(OrderPreviewVO.Item::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            group.setAmount(amount);
            total = total.add(amount);
            groups.add(group);
        }

        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setGroups(groups);
        vo.setTotalAmount(total);
        vo.setFromCart(fromCart);
        return vo;
    }

    // ---------------------------------------------------------
    // 下单
    // ---------------------------------------------------------

    /**
     * 创建订单。
     * <p>
     * 同一批购物车商品按商家拆成多个订单; 下单即扣减库存(数据库原子操作防超卖)。
     *
     * @return 生成的订单列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Orders> create(Long userId, OrderCreateDTO dto) {
        Address address = addressMapper.selectById(dto.getAddressId());
        if (address == null || !userId.equals(address.getUserId())) {
            throw BusinessException.notFound("收货地址不存在");
        }

        OrderPreviewVO preview = preview(userId, dto.getCartIds(), dto.getProductId(), dto.getQuantity());

        List<Orders> created = new ArrayList<>();
        for (OrderPreviewVO.Group group : preview.getGroups()) {
            Orders order = new Orders();
            order.setOrderNo(generateOrderNo());
            order.setUserId(userId);
            order.setMerchantId(group.getMerchantId());
            order.setTotalAmount(group.getAmount());
            order.setFreight(group.getAmount().compareTo(FREE_FREIGHT_THRESHOLD) >= 0 ? BigDecimal.ZERO : FREIGHT);
            order.setPayAmount(group.getAmount().add(order.getFreight()));
            order.setStatus(Orders.STATUS_UNPAID);
            order.setPayType(dto.getPayType() == null ? 1 : dto.getPayType());
            order.setReceiverName(address.getReceiverName());
            order.setReceiverPhone(address.getReceiverPhone());
            order.setReceiverAddress(address.fullAddress());
            order.setRemark(dto.getRemark());
            orderMapper.insert(order);

            for (OrderPreviewVO.Item item : group.getItems()) {
                // 扣库存(原子操作, 不足则抛异常回滚整单)
                productService.deductStock(item.getProductId(), item.getQuantity());

                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(order.getId());
                orderItem.setProductId(item.getProductId());
                orderItem.setProductName(item.getProductName());
                orderItem.setProductImage(item.getProductImage());
                orderItem.setPrice(item.getPrice());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setTotalPrice(item.getTotalPrice());
                orderItem.setCommented(0);
                orderItemMapper.insert(orderItem);
            }

            // 模拟支付: 直接置为待发货
            if (Boolean.TRUE.equals(dto.getPayImmediately())) {
                order.setStatus(Orders.STATUS_UNDELIVERED);
                order.setPayTime(LocalDateTime.now());
                orderMapper.updateById(order);
            }
            created.add(order);
        }

        // 清理已结算的购物车项
        if (Boolean.TRUE.equals(preview.getFromCart()) && dto.getCartIds() != null) {
            cartMapper.delete(Wrappers.<Cart>lambdaQuery()
                    .eq(Cart::getUserId, userId)
                    .in(Cart::getId, dto.getCartIds()));
        }
        return created;
    }

    /**
     * 支付订单 (模拟)
     */
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long userId, Long orderId, Integer payType) {
        Orders order = requireOwned(userId, orderId);
        if (order.getStatus() != Orders.STATUS_UNPAID) {
            throw new BusinessException("该订单当前状态不可支付");
        }
        order.setStatus(Orders.STATUS_UNDELIVERED);
        order.setPayType(payType == null ? order.getPayType() : payType);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /**
     * 取消订单 (仅待付款可取消), 同时回滚库存
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long orderId, String reason) {
        Orders order = requireOwned(userId, orderId);
        if (order.getStatus() != Orders.STATUS_UNPAID) {
            throw new BusinessException("仅待付款订单可以取消");
        }
        rollbackStock(orderId);
        order.setStatus(Orders.STATUS_CANCELED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason == null || reason.isBlank() ? "用户主动取消" : reason);
        orderMapper.updateById(order);
    }

    /**
     * 确认收货
     */
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long userId, Long orderId) {
        Orders order = requireOwned(userId, orderId);
        if (order.getStatus() != Orders.STATUS_UNRECEIVED) {
            throw new BusinessException("该订单当前状态不可确认收货");
        }
        order.setStatus(Orders.STATUS_FINISHED);
        order.setReceiveTime(LocalDateTime.now());
        order.setFinishTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /**
     * 申请退款 (待发货/待收货, 简化处理为直接退款并回滚库存)
     */
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long userId, Long orderId, String reason) {
        Orders order = requireOwned(userId, orderId);
        if (order.getStatus() != Orders.STATUS_UNDELIVERED && order.getStatus() != Orders.STATUS_UNRECEIVED) {
            throw new BusinessException("该订单当前状态不可退款");
        }
        rollbackStock(orderId);
        order.setStatus(Orders.STATUS_REFUNDED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason == null || reason.isBlank() ? "用户申请退款" : reason);
        orderMapper.updateById(order);
    }

    // ---------------------------------------------------------
    // 商家端
    // ---------------------------------------------------------

    /**
     * 商家发货
     */
    @Transactional(rollbackFor = Exception.class)
    public void deliver(Long merchantId, Long orderId, String expressNo) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!merchantId.equals(order.getMerchantId())) {
            throw BusinessException.forbidden("无权操作该订单");
        }
        if (order.getStatus() != Orders.STATUS_UNDELIVERED) {
            throw new BusinessException("该订单当前状态不可发货");
        }
        order.setStatus(Orders.STATUS_UNRECEIVED);
        order.setDeliveryTime(LocalDateTime.now());
        order.setExpressNo(expressNo);
        orderMapper.updateById(order);
    }

    // ---------------------------------------------------------
    // 查询
    // ---------------------------------------------------------

    public IPage<Orders> page(Long userId, Long merchantId, Integer status,
                              String keyword, int pageNum, int pageSize) {
        Page<Orders> page = new Page<>(Math.max(pageNum, 1), pageSize < 1 ? 10 : pageSize);
        IPage<Orders> result = orderMapper.selectOrderPage(page, userId, merchantId, status,
                keyword == null ? null : keyword.trim());
        fillItems(result.getRecords());
        return result;
    }

    /**
     * 订单详情 (含明细)
     */
    public Orders detail(Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        var loginUser = UserContext.require();
        boolean isOwner = loginUser.isUser() && loginUser.getId().equals(order.getUserId());
        boolean isMerchant = loginUser.isMerchant() && loginUser.getId().equals(order.getMerchantId());
        boolean isAdmin = loginUser.isAdmin();
        if (!isOwner && !isMerchant && !isAdmin) {
            throw BusinessException.forbidden("无权查看该订单");
        }
        fillItems(List.of(order));
        return order;
    }

    /**
     * 各状态订单数量 (我的订单角标)
     */
    public Map<String, Long> statusCount(Long userId, Long merchantId) {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("unpaid", orderMapper.countByStatus(userId, merchantId, Orders.STATUS_UNPAID));
        map.put("undelivered", orderMapper.countByStatus(userId, merchantId, Orders.STATUS_UNDELIVERED));
        map.put("unreceived", orderMapper.countByStatus(userId, merchantId, Orders.STATUS_UNRECEIVED));
        map.put("finished", orderMapper.countByStatus(userId, merchantId, Orders.STATUS_FINISHED));
        map.put("all", orderMapper.countByStatus(userId, merchantId, null));
        return map;
    }

    // ---------------------------------------------------------
    // 私有工具
    // ---------------------------------------------------------

    private void fillItems(List<Orders> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        // 入参可能是不可变集合(List.of / Arrays.asList), 而下方需要排序, 这里统一转为可变列表
        List<Orders> mutableOrders = new ArrayList<>(orders);
        List<Long> orderIds = mutableOrders.stream().map(Orders::getId).collect(Collectors.toList());
        List<OrderItem> allItems = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .in(OrderItem::getOrderId, orderIds));
        Map<Long, List<OrderItem>> grouped = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        for (Orders order : mutableOrders) {
            order.setItems(grouped.getOrDefault(order.getId(), new ArrayList<>()));
            // 触发 statusText 计算并加入序列化结果
            order.setStatusText(order.getStatusText());
            order.setRemark(order.getRemark());
        }
        // 订单按创建时间倒序
        mutableOrders.sort(Comparator.comparing(Orders::getCreateTime,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
    }

    private void rollbackStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            productService.restoreStock(item.getProductId(), item.getQuantity());
        }
    }

    private Orders requireOwned(Long userId, Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw BusinessException.forbidden("无权操作该订单");
        }
        return order;
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return timestamp + random;
    }

    /** 金额保留两位小数 */
    public static BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }
}
