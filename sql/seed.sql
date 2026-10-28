-- =============================================================
--  社区二手交易平台 - 种子数据 (由 sql/generate-seed.js 生成)
--  生成时间: 2026-10-24T10:52:10.107Z
-- =============================================================

USE `shopping_mall`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `user`;
TRUNCATE TABLE `merchant`;
TRUNCATE TABLE `admin`;
TRUNCATE TABLE `category`;
TRUNCATE TABLE `merchant_tag`;
TRUNCATE TABLE `product`;
TRUNCATE TABLE `product_collect`;
TRUNCATE TABLE `cart`;
TRUNCATE TABLE `address`;
TRUNCATE TABLE `orders`;
TRUNCATE TABLE `order_item`;
TRUNCATE TABLE `product_review`;
TRUNCATE TABLE `announcement`;
TRUNCATE TABLE `banner`;
TRUNCATE TABLE `ai_chat`;

-- 管理员: admin / admin123
INSERT INTO `admin` (`id`,`username`,`password`,`nickname`,`avatar`,`role`,`status`) VALUES
(1, 'admin', '$2a$10$m7JBfva5.IADscF58GLK2uXdxqogZtIUmIuHcgSYJf6vId/RScefa', '超级管理员', 'http://localhost:9090/api/public/placeholder.svg?text=%E7%AE%A1%E7%90%86%E5%91%98&w=120&h=120', 'SUPER_ADMIN', 1);

-- 商家 (登录密码均为 merchant123)
INSERT INTO `merchant` (`id`,`username`,`password`,`store_name`,`store_logo`,`store_desc`,`contact_name`,`contact_phone`,`address`,`status`) VALUES
(1, 'xunxi', '$2a$10$VeMQA0s7Uip3cxecXDrUwOcqC4Dul/jl5mLAGMKO7g2UDpc.hKlJG', '阳光社区业主自提点', 'http://localhost:9090/api/public/placeholder.svg?text=%E9%98%B3%E5%85%89%E7%A4%BE%E5%8C%BA%E4%B8%9A%E4%B8%BB%E8%87%AA%E6%8F%90%E7%82%B9&w=160&h=160', '小区内自提 · 当面验货 · 邻里互助', '业主委员会', '13800000001', '阳光社区 3 号楼 1 单元 101', 1),
(2, 'digimall', '$2a$10$VeMQA0s7Uip3cxecXDrUwOcqC4Dul/jl5mLAGMKO7g2UDpc.hKlJG', '物业二手置换角', 'http://localhost:9090/api/public/placeholder.svg?text=%E7%89%A9%E4%B8%9A%E4%BA%8C%E6%89%8B%E7%BD%AE%E6%8D%A2%E8%A7%92&w=160&h=160', '物业主办 · 公益置换 · 绿色循环', '物业服务中心', '13800000002', '阳光社区 物业服务中心 1 楼', 1),
(3, 'hrbeauty', '$2a$10$VeMQA0s7Uip3cxecXDrUwOcqC4Dul/jl5mLAGMKO7g2UDpc.hKlJG', '社区周边商户联盟', 'http://localhost:9090/api/public/placeholder.svg?text=%E7%A4%BE%E5%8C%BA%E5%91%A8%E8%BE%B9%E5%95%86%E6%88%B7%E8%81%94%E7%9B%9F&w=160&h=160', '周边小店 · 可送货到门 · 当面验机', '商户联盟', '13800000003', '阳光社区 南门临街商铺 12 号', 1),
(4, 'kangmin', '$2a$10$VeMQA0s7Uip3cxecXDrUwOcqC4Dul/jl5mLAGMKO7g2UDpc.hKlJG', '业主闲置转让', 'http://localhost:9090/api/public/placeholder.svg?text=%E4%B8%9A%E4%B8%BB%E9%97%B2%E7%BD%AE%E8%BD%AC%E8%AE%A9&w=160&h=160', '搬家清仓 · 大件家电全出 · 价格可谈', '王女士', '13800000004', '阳光社区 7 号楼 2 单元 602', 1);

-- 商家标签
INSERT INTO `merchant_tag` (`id`,`merchant_id`,`name`,`color`,`sort`) VALUES
(1, 1, '小区内自提', '#52c41a', 0),
(2, 1, '支持验货', '#13c2c2', 1),
(3, 2, '物业认证', '#597ef7', 2),
(4, 2, '公益置换', '#faad14', 3),
(5, 3, '可送货到门', '#eb2f96', 4),
(6, 3, '当面验机', '#9254de', 5),
(7, 4, '搬家清仓', '#52c41a', 6),
(8, 4, '价格可谈', '#13c2c2', 7);

-- 商品分类 (一级 + 二级)
INSERT INTO `category` (`id`,`parent_id`,`name`,`icon`,`sort`,`status`) VALUES
(1, 0, '家居家具', NULL, 1, 1),
(2, 0, '家用电器', NULL, 2, 1),
(3, 0, '母婴用品', NULL, 3, 1),
(4, 0, '数码电子', NULL, 4, 1),
(5, 0, '服饰鞋包', NULL, 5, 1),
(6, 0, '图书文娱', NULL, 6, 1),
(7, 0, '其他闲置', NULL, 7, 1),
(8, 1, '沙发桌椅', NULL, 1, 1),
(9, 1, '收纳置物', NULL, 2, 1),
(10, 2, '厨房电器', NULL, 1, 1),
(11, 2, '生活电器', NULL, 2, 1),
(12, 3, '婴儿车床', NULL, 1, 1),
(13, 3, '儿童玩具', NULL, 2, 1),
(14, 4, '手机', NULL, 1, 1),
(15, 4, '电脑平板', NULL, 2, 1),
(16, 4, '影音配件', NULL, 3, 1),
(17, 5, '外套上装', NULL, 1, 1),
(18, 5, '鞋靴箱包', NULL, 2, 1),
(19, 6, '书籍教材', NULL, 1, 1),
(20, 6, '儿童读物', NULL, 2, 1),
(21, 7, '运动健身', NULL, 1, 1),
(22, 7, '宠物用品', NULL, 2, 1);

-- 商品
INSERT INTO `product` (`id`,`merchant_id`,`category_id`,`name`,`sub_title`,`main_image`,`images`,`detail`,`price`,`original_price`,`stock`,`sales`,`unit`,`tag_ids`,`status`) VALUES
(1, 1, 8, '布艺沙发 三人位 灰色', '三人位 2.1 米, 布艺可拆洗, 坐垫无塌陷, 因换风格出', 'http://localhost:9090/api/uploads/products/home-sofa/main.jpg', '["http://localhost:9090/api/uploads/products/home-sofa/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-sofa/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-sofa/gallery-3.jpg"]', '<p>三人位 2.1 米, 布艺可拆洗, 坐垫无塌陷, 因换风格出</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 450, 2800, 1, 0, '件', '1,2', 1),
(2, 1, 8, '实木餐桌 四人位 带四椅', '1.4 米实木餐桌配四把椅子, 桌面有轻微使用痕迹, 结构稳固', 'http://localhost:9090/api/uploads/products/home-sofa/main.jpg', '["http://localhost:9090/api/uploads/products/home-sofa/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-sofa/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-sofa/gallery-3.jpg"]', '<p>1.4 米实木餐桌配四把椅子, 桌面有轻微使用痕迹, 结构稳固</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 320, 1600, 1, 0, '件', '1', 1),
(3, 1, 8, '书桌 白色简约 120×60', '桌面无划痕, 抽屉顺滑, 自提需自行拆装', 'http://localhost:9090/api/uploads/products/home-kitchen/main.jpg', '["http://localhost:9090/api/uploads/products/home-kitchen/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-3.jpg"]', '<p>桌面无划痕, 抽屉顺滑, 自提需自行拆装</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 120, 499, 1, 0, '件', '1,2', 1),
(4, 1, 8, '人体工学电脑椅 网布', '可升降调节, 腰托完好, 网布无破损', 'http://localhost:9090/api/uploads/products/home-lamp/main.jpg', '["http://localhost:9090/api/uploads/products/home-lamp/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-lamp/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-lamp/gallery-3.jpg"]', '<p>可升降调节, 腰托完好, 网布无破损</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 180, 799, 1, 0, '件', '1', 1),
(5, 1, 9, '实木书架 五层', '1.8 米高五层, 承重好, 有正常使用痕迹', 'http://localhost:9090/api/uploads/products/other-book/main.jpg', '["http://localhost:9090/api/uploads/products/other-book/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-book/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-book/gallery-3.jpg"]', '<p>1.8 米高五层, 承重好, 有正常使用痕迹</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 180, 690, 1, 0, '件', '1', 1),
(6, 1, 9, '塑料收纳箱 三层带轮', '三层抽屉式, 带轮好推, 装玩具杂物都合适', 'http://localhost:9090/api/uploads/products/home-kitchen/main.jpg', '["http://localhost:9090/api/uploads/products/home-kitchen/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-3.jpg"]', '<p>三层抽屉式, 带轮好推, 装玩具杂物都合适</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 35, 99, 2, 0, '件', '1,2', 1),
(7, 1, 9, '落地衣帽架 简约款', '实木挂杆, 可挂外套和帽子, 组装完好', 'http://localhost:9090/api/uploads/products/cloth-bag/main.jpg', '["http://localhost:9090/api/uploads/products/cloth-bag/gallery-1.jpg","http://localhost:9090/api/uploads/products/cloth-bag/gallery-2.jpg","http://localhost:9090/api/uploads/products/cloth-bag/gallery-3.jpg"]', '<p>实木挂杆, 可挂外套和帽子, 组装完好</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 28, 89, 2, 0, '件', '1', 1),
(8, 1, 9, '三层木质鞋柜', '1.2 米宽三层, 木纹饰面, 柜门开合正常', 'http://localhost:9090/api/uploads/products/home-kitchen/main.jpg', '["http://localhost:9090/api/uploads/products/home-kitchen/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-3.jpg"]', '<p>1.2 米宽三层, 木纹饰面, 柜门开合正常</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 90, 320, 1, 0, '件', '1', 1),
(9, 1, 10, '美的电饭煲 4L', '4L 容量够三口之家, 内胆无涂层脱落, 附蒸笼', 'http://localhost:9090/api/uploads/products/home-kitchen/main.jpg', '["http://localhost:9090/api/uploads/products/home-kitchen/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-3.jpg"]', '<p>4L 容量够三口之家, 内胆无涂层脱落, 附蒸笼</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 80, 299, 1, 0, '件', '1,2', 1),
(10, 1, 10, '格兰仕微波炉 20L', '机械旋钮款, 加热正常, 转盘完好', 'http://localhost:9090/api/uploads/products/home-kitchen/main.jpg', '["http://localhost:9090/api/uploads/products/home-kitchen/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-3.jpg"]', '<p>机械旋钮款, 加热正常, 转盘完好</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 150, 499, 1, 0, '件', '1', 1),
(11, 1, 10, '九阳豆浆机 1.2L', '可打豆浆米糊, 刀头锋利, 已清洗消毒', 'http://localhost:9090/api/uploads/products/home-coffee/main.jpg', '["http://localhost:9090/api/uploads/products/home-coffee/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-coffee/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-coffee/gallery-3.jpg"]', '<p>可打豆浆米糊, 刀头锋利, 已清洗消毒</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 60, 259, 1, 0, '件', '1', 1),
(12, 1, 10, '美的电磁炉 2200W', '火力足, 面板无划痕, 送汤锅一只', 'http://localhost:9090/api/uploads/products/home-kitchen/main.jpg', '["http://localhost:9090/api/uploads/products/home-kitchen/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-3.jpg"]', '<p>火力足, 面板无划痕, 送汤锅一只</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 55, 199, 2, 0, '件', '1,2', 1),
(13, 1, 11, '海尔波轮洗衣机 8kg', '8 公斤容量, 洗涤脱水正常, 外观有轻微磕碰, 功能无碍', 'http://localhost:9090/api/uploads/products/home-kitchen/main.jpg', '["http://localhost:9090/api/uploads/products/home-kitchen/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-3.jpg"]', '<p>8 公斤容量, 洗涤脱水正常, 外观有轻微磕碰, 功能无碍</p><p>成色: 7成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 600, 1899, 1, 0, '件', '1', 1),
(14, 1, 11, '小米空气净化器 2S', '滤芯剩余约三成, 建议自购更换, 机器运行正常', 'http://localhost:9090/api/uploads/products/home-coffee/main.jpg', '["http://localhost:9090/api/uploads/products/home-coffee/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-coffee/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-coffee/gallery-3.jpg"]', '<p>滤芯剩余约三成, 建议自购更换, 机器运行正常</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 320, 899, 1, 0, '件', '1,2', 1),
(15, 1, 11, '电压力锅 6L 双胆', '双内胆一锅两用, 密封圈完好, 附说明书', 'http://localhost:9090/api/uploads/products/home-kitchen/main.jpg', '["http://localhost:9090/api/uploads/products/home-kitchen/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-3.jpg"]', '<p>双内胆一锅两用, 密封圈完好, 附说明书</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 120, 399, 1, 0, '件', '1', 1),
(16, 1, 11, '落地扇 静音遥控款', '三档风速可遥控, 扇叶已清洗, 夏季必备', 'http://localhost:9090/api/uploads/products/home-lamp/main.jpg', '["http://localhost:9090/api/uploads/products/home-lamp/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-lamp/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-lamp/gallery-3.jpg"]', '<p>三档风速可遥控, 扇叶已清洗, 夏季必备</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「阳光社区业主自提点」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 40, 159, 2, 0, '件', '1', 1),
(17, 2, 12, '好孩子婴儿推车 可折叠', '轻便可折叠, 一键收车, 遮阳篷完好, 已清洗消毒', 'http://localhost:9090/api/uploads/products/other-toy/main.jpg', '["http://localhost:9090/api/uploads/products/other-toy/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-3.jpg"]', '<p>轻便可折叠, 一键收车, 遮阳篷完好, 已清洗消毒</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「物业二手置换角」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 280, 899, 1, 0, '件', '3,4', 1),
(18, 2, 12, '实木婴儿床 带护栏', '1.2 米实木床, 可调高度, 床板完好, 需自提', 'http://localhost:9090/api/uploads/products/other-toy/main.jpg', '["http://localhost:9090/api/uploads/products/other-toy/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-3.jpg"]', '<p>1.2 米实木床, 可调高度, 床板完好, 需自提</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「物业二手置换角」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 320, 1299, 1, 0, '件', '3', 1),
(19, 2, 12, '儿童安全座椅 3-12 岁', 'ISOFIX 接口, 无碰撞记录, 布套已拆洗', 'http://localhost:9090/api/uploads/products/other-toy/main.jpg', '["http://localhost:9090/api/uploads/products/other-toy/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-3.jpg"]', '<p>ISOFIX 接口, 无碰撞记录, 布套已拆洗</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「物业二手置换角」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 260, 899, 1, 0, '件', '3,4', 1),
(20, 2, 13, '儿童滑板车 三轮稳固款', '轮子顺滑, 适合 3-6 岁, 把手可调高度', 'http://localhost:9090/api/uploads/products/other-toy/main.jpg', '["http://localhost:9090/api/uploads/products/other-toy/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-3.jpg"]', '<p>轮子顺滑, 适合 3-6 岁, 把手可调高度</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「物业二手置换角」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 45, 159, 2, 0, '件', '3', 1),
(21, 2, 13, '儿童积木桌 带收纳', '桌面可翻转为积木底板, 附一袋大颗粒积木', 'http://localhost:9090/api/uploads/products/other-toy/main.jpg', '["http://localhost:9090/api/uploads/products/other-toy/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-3.jpg"]', '<p>桌面可翻转为积木底板, 附一袋大颗粒积木</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「物业二手置换角」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 60, 199, 1, 0, '件', '3,4', 1),
(22, 2, 13, '大颗粒积木 一箱约 200 颗', '已清洗消毒, 配件齐全, 适合 3 岁以上', 'http://localhost:9090/api/uploads/products/other-toy/main.jpg', '["http://localhost:9090/api/uploads/products/other-toy/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-3.jpg"]', '<p>已清洗消毒, 配件齐全, 适合 3 岁以上</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「物业二手置换角」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 120, 399, 1, 0, '件', '3', 1),
(23, 2, 19, '《三体》全集 三册', '无划线无笔记, 书脊完好, 科幻必读', 'http://localhost:9090/api/uploads/products/other-book/main.jpg', '["http://localhost:9090/api/uploads/products/other-book/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-book/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-book/gallery-3.jpg"]', '<p>无划线无笔记, 书脊完好, 科幻必读</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「物业二手置换角」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 45, 108, 2, 0, '件', '3,4', 1),
(24, 2, 19, '考研英语真题 黄皮书', '有少量铅笔笔记可擦除, 内容完整', 'http://localhost:9090/api/uploads/products/other-book/main.jpg', '["http://localhost:9090/api/uploads/products/other-book/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-book/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-book/gallery-3.jpg"]', '<p>有少量铅笔笔记可擦除, 内容完整</p><p>成色: 7成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「物业二手置换角」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 35, 129, 1, 0, '件', '3', 1),
(25, 2, 20, '儿童绘本 30 册 套装', '3-6 岁适读, 无撕页, 已擦拭消毒', 'http://localhost:9090/api/uploads/products/other-book/main.jpg', '["http://localhost:9090/api/uploads/products/other-book/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-book/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-book/gallery-3.jpg"]', '<p>3-6 岁适读, 无撕页, 已擦拭消毒</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「物业二手置换角」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 50, 200, 2, 0, '件', '3,4', 1),
(26, 2, 20, '小学生课外读物 一套 8 册', '注音版, 几乎全新, 适合一二年级', 'http://localhost:9090/api/uploads/products/other-book/main.jpg', '["http://localhost:9090/api/uploads/products/other-book/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-book/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-book/gallery-3.jpg"]', '<p>注音版, 几乎全新, 适合一二年级</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「物业二手置换角」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 35, 120, 2, 0, '件', '3', 1),
(27, 2, 11, '护眼台灯 三档调光', '无频闪, USB 供电, 孩子写作业用', 'http://localhost:9090/api/uploads/products/home-lamp/main.jpg', '["http://localhost:9090/api/uploads/products/home-lamp/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-lamp/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-lamp/gallery-3.jpg"]', '<p>无频闪, USB 供电, 孩子写作业用</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「物业二手置换角」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 35, 109, 2, 0, '件', '3,4', 1),
(28, 2, 11, '家用加湿器 4L 静音', '大容量静音夜灯款, 已除垢清洁', 'http://localhost:9090/api/uploads/products/home-coffee/main.jpg', '["http://localhost:9090/api/uploads/products/home-coffee/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-coffee/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-coffee/gallery-3.jpg"]', '<p>大容量静音夜灯款, 已除垢清洁</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「物业二手置换角」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 60, 199, 1, 0, '件', '3', 1),
(29, 2, 11, '手持挂烫机', '出汽快, 水箱完好, 附防烫手套', 'http://localhost:9090/api/uploads/products/home-kitchen/main.jpg', '["http://localhost:9090/api/uploads/products/home-kitchen/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-3.jpg"]', '<p>出汽快, 水箱完好, 附防烫手套</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「物业二手置换角」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 90, 299, 1, 0, '件', '3,4', 1),
(30, 2, 11, '油汀式电暖器 11 片', '制热正常, 带加湿盒, 冬季取暖用, 较重需自提', 'http://localhost:9090/api/uploads/products/home-kitchen/main.jpg', '["http://localhost:9090/api/uploads/products/home-kitchen/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-kitchen/gallery-3.jpg"]', '<p>制热正常, 带加湿盒, 冬季取暖用, 较重需自提</p><p>成色: 7成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「物业二手置换角」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 120, 399, 1, 0, '件', '3', 1),
(31, 3, 14, 'iPhone 13 128G 星光色', '国行双卡, 电池健康 88%, 无维修记录, 可当面验机查序列号', 'http://localhost:9090/api/uploads/products/phone-flagship/main.jpg', '["http://localhost:9090/api/uploads/products/phone-flagship/gallery-1.jpg","http://localhost:9090/api/uploads/products/phone-flagship/gallery-2.jpg","http://localhost:9090/api/uploads/products/phone-flagship/gallery-3.jpg"]', '<p>国行双卡, 电池健康 88%, 无维修记录, 可当面验机查序列号</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 2100, 5999, 1, 0, '件', '5,6', 1),
(32, 3, 14, '小米 13 12+256 徕卡影像', '骁龙 8Gen2, 屏幕无划痕, 原装充电器齐全', 'http://localhost:9090/api/uploads/products/phone-flagship/main.jpg', '["http://localhost:9090/api/uploads/products/phone-flagship/gallery-1.jpg","http://localhost:9090/api/uploads/products/phone-flagship/gallery-2.jpg","http://localhost:9090/api/uploads/products/phone-flagship/gallery-3.jpg"]', '<p>骁龙 8Gen2, 屏幕无划痕, 原装充电器齐全</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 1450, 4599, 1, 0, '件', '5', 1),
(33, 3, 14, '荣耀 Play 6T 8+128', '备用机出售, 功能一切正常, 适合老人机或备用', 'http://localhost:9090/api/uploads/products/phone-budget/main.jpg', '["http://localhost:9090/api/uploads/products/phone-budget/gallery-1.jpg","http://localhost:9090/api/uploads/products/phone-budget/gallery-2.jpg","http://localhost:9090/api/uploads/products/phone-budget/gallery-3.jpg"]', '<p>备用机出售, 功能一切正常, 适合老人机或备用</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 380, 1099, 2, 0, '件', '5,6', 1),
(34, 3, 14, 'Redmi Note 12 Turbo 8+256', '无磕碰无划痕, 一直带壳贴膜, 配件齐全', 'http://localhost:9090/api/uploads/products/phone-midrange/main.jpg', '["http://localhost:9090/api/uploads/products/phone-midrange/gallery-1.jpg","http://localhost:9090/api/uploads/products/phone-midrange/gallery-2.jpg","http://localhost:9090/api/uploads/products/phone-midrange/gallery-3.jpg"]', '<p>无磕碰无划痕, 一直带壳贴膜, 配件齐全</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 899, 1799, 1, 0, '件', '5,6', 1),
(35, 3, 15, 'iPad 第 9 代 64G WiFi', '看网课记笔记好用, 带保护套和钢化膜', 'http://localhost:9090/api/uploads/products/tablet/main.jpg', '["http://localhost:9090/api/uploads/products/tablet/gallery-1.jpg","http://localhost:9090/api/uploads/products/tablet/gallery-2.jpg","http://localhost:9090/api/uploads/products/tablet/gallery-3.jpg"]', '<p>看网课记笔记好用, 带保护套和钢化膜</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 1350, 2499, 1, 0, '件', '5,6', 1),
(36, 3, 15, '小米平板 6 8+128', '144Hz 高刷屏, 追剧办公都合适, 附磁吸壳', 'http://localhost:9090/api/uploads/products/tablet/main.jpg', '["http://localhost:9090/api/uploads/products/tablet/gallery-1.jpg","http://localhost:9090/api/uploads/products/tablet/gallery-2.jpg","http://localhost:9090/api/uploads/products/tablet/gallery-3.jpg"]', '<p>144Hz 高刷屏, 追剧办公都合适, 附磁吸壳</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 980, 1999, 1, 0, '件', '5', 1),
(37, 3, 15, '联想小新 Air14 轻薄本', 'i5-1240P / 16G+512G, 办公学习够用, 电池健康 85%', 'http://localhost:9090/api/uploads/products/laptop-ultra/main.jpg', '["http://localhost:9090/api/uploads/products/laptop-ultra/gallery-1.jpg","http://localhost:9090/api/uploads/products/laptop-ultra/gallery-2.jpg","http://localhost:9090/api/uploads/products/laptop-ultra/gallery-3.jpg"]', '<p>i5-1240P / 16G+512G, 办公学习够用, 电池健康 85%</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 2350, 4999, 1, 0, '件', '5,6', 1),
(38, 3, 15, '华硕天选 4 游戏本 RTX4060', 'R7-7735H / 16G+512G, 键盘无油光, 可现场跑分验证', 'http://localhost:9090/api/uploads/products/laptop-gaming/main.jpg', '["http://localhost:9090/api/uploads/products/laptop-gaming/gallery-1.jpg","http://localhost:9090/api/uploads/products/laptop-gaming/gallery-2.jpg","http://localhost:9090/api/uploads/products/laptop-gaming/gallery-3.jpg"]', '<p>R7-7735H / 16G+512G, 键盘无油光, 可现场跑分验证</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 4600, 8299, 1, 0, '件', '5', 1),
(39, 3, 16, '索尼 WH-1000XM4 头戴降噪', '降噪效果依旧顶级, 带原装收纳包', 'http://localhost:9090/api/uploads/products/headphones/main.jpg', '["http://localhost:9090/api/uploads/products/headphones/gallery-1.jpg","http://localhost:9090/api/uploads/products/headphones/gallery-2.jpg","http://localhost:9090/api/uploads/products/headphones/gallery-3.jpg"]', '<p>降噪效果依旧顶级, 带原装收纳包</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 1050, 2299, 1, 0, '件', '5,6', 1),
(40, 3, 16, 'AirPods 2 代 有线充版', '左耳轻微使用痕迹, 音质正常, 续航约 4 小时', 'http://localhost:9090/api/uploads/products/earbuds/main.jpg', '["http://localhost:9090/api/uploads/products/earbuds/gallery-1.jpg","http://localhost:9090/api/uploads/products/earbuds/gallery-2.jpg","http://localhost:9090/api/uploads/products/earbuds/gallery-3.jpg"]', '<p>左耳轻微使用痕迹, 音质正常, 续航约 4 小时</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 320, 999, 1, 0, '件', '5', 1),
(41, 3, 16, '佳能 EOS 200D II 单反套机', '含 18-55mm 镜头, 快门约 8000 次, 适合家用记录', 'http://localhost:9090/api/uploads/products/camera/main.jpg', '["http://localhost:9090/api/uploads/products/camera/gallery-1.jpg","http://localhost:9090/api/uploads/products/camera/gallery-2.jpg","http://localhost:9090/api/uploads/products/camera/gallery-3.jpg"]', '<p>含 18-55mm 镜头, 快门约 8000 次, 适合家用记录</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 2200, 4299, 1, 0, '件', '5,6', 1),
(42, 3, 16, 'AOC 24 英寸 1080P 显示器', '外接办公用, 无坏点, 附 HDMI 线', 'http://localhost:9090/api/uploads/products/monitor/main.jpg', '["http://localhost:9090/api/uploads/products/monitor/gallery-1.jpg","http://localhost:9090/api/uploads/products/monitor/gallery-2.jpg","http://localhost:9090/api/uploads/products/monitor/gallery-3.jpg"]', '<p>外接办公用, 无坏点, 附 HDMI 线</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 380, 899, 1, 0, '件', '5', 1),
(43, 3, 16, '罗技 K380 无线键盘', '可连三个设备, 静音手感, 附电池', 'http://localhost:9090/api/uploads/products/keyboard/main.jpg', '["http://localhost:9090/api/uploads/products/keyboard/gallery-1.jpg","http://localhost:9090/api/uploads/products/keyboard/gallery-2.jpg","http://localhost:9090/api/uploads/products/keyboard/gallery-3.jpg"]', '<p>可连三个设备, 静音手感, 附电池</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 95, 249, 2, 0, '件', '5', 1),
(44, 3, 17, 'Lee 牛仔外套 男 L 码', '正品, 穿过几次, 无破损无褪色, 已干洗', 'http://localhost:9090/api/uploads/products/cloth-jeans/main.jpg', '["http://localhost:9090/api/uploads/products/cloth-jeans/gallery-1.jpg","http://localhost:9090/api/uploads/products/cloth-jeans/gallery-2.jpg","http://localhost:9090/api/uploads/products/cloth-jeans/gallery-3.jpg"]', '<p>正品, 穿过几次, 无破损无褪色, 已干洗</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 120, 599, 1, 0, '件', '5,6', 1),
(45, 3, 17, '优衣库摇粒绒外套 女 M 码', '秋冬保暖, 无起球, 已清洗消毒', 'http://localhost:9090/api/uploads/products/cloth-sweater/main.jpg', '["http://localhost:9090/api/uploads/products/cloth-sweater/gallery-1.jpg","http://localhost:9090/api/uploads/products/cloth-sweater/gallery-2.jpg","http://localhost:9090/api/uploads/products/cloth-sweater/gallery-3.jpg"]', '<p>秋冬保暖, 无起球, 已清洗消毒</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 65, 249, 1, 0, '件', '5', 1),
(46, 3, 17, '美式复古卫衣 男女同款 L', '厚实不透, 洗过两次不掉色', 'http://localhost:9090/api/uploads/products/cloth-sweater/main.jpg', '["http://localhost:9090/api/uploads/products/cloth-sweater/gallery-1.jpg","http://localhost:9090/api/uploads/products/cloth-sweater/gallery-2.jpg","http://localhost:9090/api/uploads/products/cloth-sweater/gallery-3.jpg"]', '<p>厚实不透, 洗过两次不掉色</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 55, 199, 2, 0, '件', '5', 1),
(47, 3, 18, 'JanSport 双肩背包 大容量', '能装 15.6 寸电脑, 拉链顺滑, 无破损', 'http://localhost:9090/api/uploads/products/cloth-bag/main.jpg', '["http://localhost:9090/api/uploads/products/cloth-bag/gallery-1.jpg","http://localhost:9090/api/uploads/products/cloth-bag/gallery-2.jpg","http://localhost:9090/api/uploads/products/cloth-bag/gallery-3.jpg"]', '<p>能装 15.6 寸电脑, 拉链顺滑, 无破损</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 85, 359, 1, 0, '件', '5,6', 1),
(48, 3, 18, '帆布单肩托特包', '装书装电脑都行, 文艺百搭, 可机洗', 'http://localhost:9090/api/uploads/products/cloth-bag/main.jpg', '["http://localhost:9090/api/uploads/products/cloth-bag/gallery-1.jpg","http://localhost:9090/api/uploads/products/cloth-bag/gallery-2.jpg","http://localhost:9090/api/uploads/products/cloth-bag/gallery-3.jpg"]', '<p>装书装电脑都行, 文艺百搭, 可机洗</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「社区周边商户联盟」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 28, 99, 3, 0, '件', '5', 1),
(49, 4, 21, '捷安特 ATX 660 山地车 27.5 寸', '搬家出, 变速链条正常, 刚换后胎, 送车锁和打气筒', 'http://localhost:9090/api/uploads/products/other-bike/main.jpg', '["http://localhost:9090/api/uploads/products/other-bike/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-bike/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-bike/gallery-3.jpg"]', '<p>搬家出, 变速链条正常, 刚换后胎, 送车锁和打气筒</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「业主闲置转让」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 680, 1899, 1, 0, '件', '7,8', 1),
(50, 4, 21, '凤凰折叠自行车 20 寸', '可折叠放阳台, 骑行正常, 适合短途买菜代步', 'http://localhost:9090/api/uploads/products/other-bike/main.jpg', '["http://localhost:9090/api/uploads/products/other-bike/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-bike/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-bike/gallery-3.jpg"]', '<p>可折叠放阳台, 骑行正常, 适合短途买菜代步</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「业主闲置转让」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 260, 699, 1, 0, '件', '7,8', 1),
(51, 4, 21, '家用折叠跑步机', '折叠收纳不占地, 电机运行正常, 较重需自提', 'http://localhost:9090/api/uploads/products/other-toy/main.jpg', '["http://localhost:9090/api/uploads/products/other-toy/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-3.jpg"]', '<p>折叠收纳不占地, 电机运行正常, 较重需自提</p><p>成色: 7成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「业主闲置转让」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 600, 1999, 1, 0, '件', '7,8', 1),
(52, 4, 21, '瑜伽垫 加厚防滑 183×61cm', '用了几次, 已清洁消毒, 附收纳绑带', 'http://localhost:9090/api/uploads/products/other-toy/main.jpg', '["http://localhost:9090/api/uploads/products/other-toy/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-3.jpg"]', '<p>用了几次, 已清洁消毒, 附收纳绑带</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「业主闲置转让」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 30, 99, 2, 0, '件', '7', 1),
(53, 4, 21, '可调节哑铃 20kg 一对', '片数齐全, 自提较重, 不邮寄', 'http://localhost:9090/api/uploads/products/other-toy/main.jpg', '["http://localhost:9090/api/uploads/products/other-toy/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-3.jpg"]', '<p>片数齐全, 自提较重, 不邮寄</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「业主闲置转让」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 130, 399, 1, 0, '件', '7,8', 1),
(54, 4, 21, '尤尼克斯羽毛球拍 双拍套装', '含拍套和三个球, 网线还紧, 适合入门', 'http://localhost:9090/api/uploads/products/other-toy/main.jpg', '["http://localhost:9090/api/uploads/products/other-toy/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-3.jpg"]', '<p>含拍套和三个球, 网线还紧, 适合入门</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「业主闲置转让」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 110, 399, 1, 0, '件', '7', 1),
(55, 4, 11, '小米电热水壶 1.5L', '烧水快, 自动断电, 已除垢清洁', 'http://localhost:9090/api/uploads/products/home-coffee/main.jpg', '["http://localhost:9090/api/uploads/products/home-coffee/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-coffee/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-coffee/gallery-3.jpg"]', '<p>烧水快, 自动断电, 已除垢清洁</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「业主闲置转让」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 45, 129, 2, 0, '件', '7,8', 1),
(56, 4, 11, '小熊家用吹风机', '大功率速干, 恒温不伤发', 'http://localhost:9090/api/uploads/products/printer/main.jpg', '["http://localhost:9090/api/uploads/products/printer/gallery-1.jpg","http://localhost:9090/api/uploads/products/printer/gallery-2.jpg","http://localhost:9090/api/uploads/products/printer/gallery-3.jpg"]', '<p>大功率速干, 恒温不伤发</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「业主闲置转让」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 50, 159, 2, 0, '件', '7', 1),
(57, 4, 11, '静音小风扇 USB 充电款', '三档风速, 可夹床头, 夏天续命神器', 'http://localhost:9090/api/uploads/products/home-lamp/main.jpg', '["http://localhost:9090/api/uploads/products/home-lamp/gallery-1.jpg","http://localhost:9090/api/uploads/products/home-lamp/gallery-2.jpg","http://localhost:9090/api/uploads/products/home-lamp/gallery-3.jpg"]', '<p>三档风速, 可夹床头, 夏天续命神器</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「业主闲置转让」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 25, 79, 3, 0, '件', '7', 1),
(58, 4, 22, '猫爬架 三层 实木', '猫抓柱完好, 平台稳固, 已清洁消毒', 'http://localhost:9090/api/uploads/products/other-toy/main.jpg', '["http://localhost:9090/api/uploads/products/other-toy/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-3.jpg"]', '<p>猫抓柱完好, 平台稳固, 已清洁消毒</p><p>成色: 8成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「业主闲置转让」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 80, 299, 1, 0, '件', '7,8', 1),
(59, 4, 22, '宠物航空箱 中号', '适合 10kg 内猫狗, 门锁正常, 带饮水碗', 'http://localhost:9090/api/uploads/products/other-toy/main.jpg', '["http://localhost:9090/api/uploads/products/other-toy/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-3.jpg"]', '<p>适合 10kg 内猫狗, 门锁正常, 带饮水碗</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「业主闲置转让」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 60, 199, 2, 0, '件', '7', 1),
(60, 4, 22, '全封闭猫砂盆 带铲', '防外溅设计, 附猫砂铲, 已彻底清洗', 'http://localhost:9090/api/uploads/products/other-toy/main.jpg', '["http://localhost:9090/api/uploads/products/other-toy/gallery-1.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-2.jpg","http://localhost:9090/api/uploads/products/other-toy/gallery-3.jpg"]', '<p>防外溅设计, 附猫砂铲, 已彻底清洗</p><p>成色: 9成新 | 交易方式: 小区门口自提 / 物业中心代收</p><p>该闲置由「业主闲置转让」发布。建议当面验货确认无误后再签收, 请放心交易。</p><p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>', 45, 159, 2, 0, '件', '7', 1);

-- 普通用户 (登录密码均为 123456)
INSERT INTO `user` (`id`,`username`,`password`,`nickname`,`avatar`,`phone`,`email`,`gender`,`status`) VALUES
(1, 'zhangsan', '$2a$10$SYryJ6S24k/EE08IXFS08ub8XdFs/ErgswHyYTplVoSnPgt9WIPtW', '张三', 'http://localhost:9090/api/public/placeholder.svg?text=%E5%BC%A0%E4%B8%89&w=120&h=120', '13900000001', 'zhangsan@example.com', 1, 1),
(2, 'lisi', '$2a$10$SYryJ6S24k/EE08IXFS08ub8XdFs/ErgswHyYTplVoSnPgt9WIPtW', '李四', 'http://localhost:9090/api/public/placeholder.svg?text=%E6%9D%8E%E5%9B%9B&w=120&h=120', '13900000002', 'lisi@example.com', 2, 1),
(3, 'wangwu', '$2a$10$SYryJ6S24k/EE08IXFS08ub8XdFs/ErgswHyYTplVoSnPgt9WIPtW', '王五', 'http://localhost:9090/api/public/placeholder.svg?text=%E7%8E%8B%E4%BA%94&w=120&h=120', '13900000003', 'wangwu@example.com', 1, 1),
(4, 'zhaoliu', '$2a$10$SYryJ6S24k/EE08IXFS08ub8XdFs/ErgswHyYTplVoSnPgt9WIPtW', '赵六', 'http://localhost:9090/api/public/placeholder.svg?text=%E8%B5%B5%E5%85%AD&w=120&h=120', '13900000004', 'zhaoliu@example.com', 2, 1),
(5, 'test', '$2a$10$SYryJ6S24k/EE08IXFS08ub8XdFs/ErgswHyYTplVoSnPgt9WIPtW', '测试用户', 'http://localhost:9090/api/public/placeholder.svg?text=%E6%B5%8B%E8%AF%95%E7%94%A8%E6%88%B7&w=120&h=120', '13900000005', 'test@example.com', 0, 1);

-- 收货地址
INSERT INTO `address` (`id`,`user_id`,`receiver_name`,`receiver_phone`,`province`,`city`,`district`,`detail_address`,`is_default`) VALUES
(1, 1, '张三', '13900000001', '湖北省', '武汉市', '洪山区', '珞喻路 1037 号华中科技大学 韵苑公寓', 1),
(2, 1, '张三(公司)', '13900000001', '湖北省', '武汉市', '东湖新技术开发区', '光谷软件园 F 栋 12 层', 0),
(3, 2, '李四', '13900000002', '山东省', '济南市', '历下区', '经十路 8888 号 某某小区 5 栋 302', 1),
(4, 3, '王五', '13900000003', '广东省', '深圳市', '南山区', '科技园南路 18 号 腾讯大厦', 1),
(5, 5, '测试用户', '13900000005', '北京市', '北京市', '海淀区', '中关村大街 1 号', 1);

-- 购物车
INSERT INTO `cart` (`user_id`,`product_id`,`quantity`,`checked`) VALUES
(1, 1, 2, 1),
(1, 3, 1, 1),
(1, 13, 1, 0),
(2, 17, 1, 1),
(3, 10, 1, 1),
(5, 9, 1, 1);

-- 商品收藏
INSERT INTO `product_collect` (`user_id`,`product_id`) VALUES
(1, 1),
(1, 9),
(2, 3),
(3, 17),
(4, 10),
(5, 13);

-- 订单
INSERT INTO `orders` (`id`,`order_no`,`user_id`,`merchant_id`,`total_amount`,`pay_amount`,`freight`,`status`,`pay_type`,`pay_time`,`delivery_time`,`receive_time`,`finish_time`,`cancel_time`,`cancel_reason`,`receiver_name`,`receiver_phone`,`receiver_address`,`remark`,`express_no`,`create_time`) VALUES
(1, '20261012100001', 1, 1, 1080.00, 1080.00, 0.00, 3, 2, '2026-10-12 10:57:10', '2026-10-13 10:52:10', '2026-10-15 10:52:10', '2026-10-15 10:52:10', NULL, NULL, '张三', '13900000001', '湖北省武汉市洪山区珞喻路 1037 号华中科技大学 韵苑公寓', '请尽快发货, 谢谢', 'SF1000000137', '2026-10-12 10:52:10'),
(2, '20261015100002', 1, 2, 150.00, 150.00, 0.00, 3, 2, '2026-10-15 10:57:10', '2026-10-16 10:52:10', '2026-10-18 10:52:10', '2026-10-18 10:52:10', NULL, NULL, '张三', '13900000001', '湖北省武汉市洪山区珞喻路 1037 号华中科技大学 韵苑公寓', NULL, 'SF1000000274', '2026-10-15 10:52:10'),
(3, '20261017100003', 2, 1, 176.00, 176.00, 0.00, 3, 2, '2026-10-17 10:57:10', '2026-10-18 10:52:10', '2026-10-20 10:52:10', '2026-10-20 10:52:10', NULL, NULL, '李四', '13900000002', '山东省济南市历下区经十路 8888 号 某某小区 5 栋 302', '无', 'SF1000000411', '2026-10-17 10:52:10'),
(4, '20261020100004', 2, 3, 320.00, 320.00, 0.00, 2, 2, '2026-10-20 10:57:10', '2026-10-21 10:52:10', NULL, NULL, NULL, NULL, '李四', '13900000002', '山东省济南市历下区经十路 8888 号 某某小区 5 栋 302', '需要礼品包装', 'SF1000000548', '2026-10-20 10:52:10'),
(5, '20261021100005', 3, 2, 1280.00, 1280.00, 0.00, 3, 2, '2026-10-21 10:57:10', '2026-10-22 10:52:10', '2026-10-24 10:52:10', '2026-10-24 10:52:10', NULL, NULL, '王五', '13900000003', '广东省深圳市南山区科技园南路 18 号 腾讯大厦', NULL, 'SF1000000685', '2026-10-21 10:52:10'),
(6, '20261022100006', 4, 3, 260.00, 260.00, 0.00, 3, 2, '2026-10-22 10:57:10', '2026-10-23 10:52:10', '2026-10-25 10:52:10', '2026-10-25 10:52:10', NULL, NULL, '李四', '13900000002', '山东省济南市历下区经十路 8888 号 某某小区 5 栋 302', '发顺丰', 'SF1000000822', '2026-10-22 10:52:10'),
(7, '20261023100007', 5, 1, 500.00, 500.00, 0.00, 2, 2, '2026-10-23 10:57:10', '2026-10-24 10:52:10', NULL, NULL, NULL, NULL, '测试用户', '13900000005', '北京市北京市海淀区中关村大街 1 号', '测试订单', 'SF1000000959', '2026-10-23 10:52:10'),
(8, '20261024100008', 3, 1, 35.00, 45.00, 10.00, 0, 2, NULL, NULL, NULL, NULL, NULL, NULL, '王五', '13900000003', '广东省深圳市南山区科技园南路 18 号 腾讯大厦', '待付款订单', NULL, '2026-10-24 10:52:10');

-- 订单明细
INSERT INTO `order_item` (`id`,`order_id`,`product_id`,`product_name`,`product_image`,`price`,`quantity`,`total_price`,`commented`) VALUES
(1, 1, 1, '布艺沙发 三人位 灰色', 'http://localhost:9090/api/public/placeholder.svg?text=%E5%B8%83%E8%89%BA%E6%B2%99%E5%8F%91%20%E4%B8%89%E4%BA%BA%E4%BD%8D%20%E7%81%B0%E8%89%B2&w=200&h=200', 450, 2, 900.00, 1),
(2, 1, 5, '实木书架 五层', 'http://localhost:9090/api/public/placeholder.svg?text=%E5%AE%9E%E6%9C%A8%E4%B9%A6%E6%9E%B6%20%E4%BA%94%E5%B1%82&w=200&h=200', 180, 1, 180.00, 1),
(3, 2, 10, '格兰仕微波炉 20L', 'http://localhost:9090/api/public/placeholder.svg?text=%E6%A0%BC%E5%85%B0%E4%BB%95%E5%BE%AE%E6%B3%A2%E7%82%89%2020L&w=200&h=200', 150, 1, 150.00, 1),
(4, 3, 3, '书桌 白色简约 120×60', 'http://localhost:9090/api/public/placeholder.svg?text=%E4%B9%A6%E6%A1%8C%20%E7%99%BD%E8%89%B2%E7%AE%80%E7%BA%A6%20120%C3%9760&w=200&h=200', 120, 1, 120.00, 1),
(5, 3, 7, '落地衣帽架 简约款', 'http://localhost:9090/api/public/placeholder.svg?text=%E8%90%BD%E5%9C%B0%E8%A1%A3%E5%B8%BD%E6%9E%B6%20%E7%AE%80%E7%BA%A6%E6%AC%BE&w=200&h=200', 28, 2, 56.00, 1),
(6, 4, 18, '实木婴儿床 带护栏', 'http://localhost:9090/api/public/placeholder.svg?text=%E5%AE%9E%E6%9C%A8%E5%A9%B4%E5%84%BF%E5%BA%8A%20%E5%B8%A6%E6%8A%A4%E6%A0%8F&w=200&h=200', 320, 1, 320.00, 0),
(7, 5, 9, '美的电饭煲 4L', 'http://localhost:9090/api/public/placeholder.svg?text=%E7%BE%8E%E7%9A%84%E7%94%B5%E9%A5%AD%E7%85%B2%204L&w=200&h=200', 80, 1, 80.00, 1),
(8, 5, 13, '海尔波轮洗衣机 8kg', 'http://localhost:9090/api/public/placeholder.svg?text=%E6%B5%B7%E5%B0%94%E6%B3%A2%E8%BD%AE%E6%B4%97%E8%A1%A3%E6%9C%BA%208kg&w=200&h=200', 600, 2, 1200.00, 1),
(9, 6, 19, '儿童安全座椅 3-12 岁', 'http://localhost:9090/api/public/placeholder.svg?text=%E5%84%BF%E7%AB%A5%E5%AE%89%E5%85%A8%E5%BA%A7%E6%A4%85%203-12%20%E5%B2%81&w=200&h=200', 260, 1, 260.00, 1),
(10, 7, 2, '实木餐桌 四人位 带四椅', 'http://localhost:9090/api/public/placeholder.svg?text=%E5%AE%9E%E6%9C%A8%E9%A4%90%E6%A1%8C%20%E5%9B%9B%E4%BA%BA%E4%BD%8D%20%E5%B8%A6%E5%9B%9B%E6%A4%85&w=200&h=200', 320, 1, 320.00, 0),
(11, 7, 4, '人体工学电脑椅 网布', 'http://localhost:9090/api/public/placeholder.svg?text=%E4%BA%BA%E4%BD%93%E5%B7%A5%E5%AD%A6%E7%94%B5%E8%84%91%E6%A4%85%20%E7%BD%91%E5%B8%83&w=200&h=200', 180, 1, 180.00, 0),
(12, 8, 6, '塑料收纳箱 三层带轮', 'http://localhost:9090/api/public/placeholder.svg?text=%E5%A1%91%E6%96%99%E6%94%B6%E7%BA%B3%E7%AE%B1%20%E4%B8%89%E5%B1%82%E5%B8%A6%E8%BD%AE&w=200&h=200', 35, 1, 35.00, 0);

-- 根据订单明细回填商品销量, 保证看板数据自洽
UPDATE `product` SET `sales` = 2 WHERE `id` = 1;
UPDATE `product` SET `sales` = 1 WHERE `id` = 2;
UPDATE `product` SET `sales` = 1 WHERE `id` = 3;
UPDATE `product` SET `sales` = 1 WHERE `id` = 4;
UPDATE `product` SET `sales` = 1 WHERE `id` = 5;
UPDATE `product` SET `sales` = 2 WHERE `id` = 7;
UPDATE `product` SET `sales` = 1 WHERE `id` = 9;
UPDATE `product` SET `sales` = 1 WHERE `id` = 10;
UPDATE `product` SET `sales` = 2 WHERE `id` = 13;
UPDATE `product` SET `sales` = 1 WHERE `id` = 18;
UPDATE `product` SET `sales` = 1 WHERE `id` = 19;

-- 商品评价 (部分含商家回复)
INSERT INTO `product_review` (`product_id`,`order_id`,`order_item_id`,`user_id`,`merchant_id`,`score`,`content`,`images`,`anonymous`,`reply_content`,`reply_time`,`status`,`create_time`) VALUES
(1, 1, 1, 1, 1, 5, '东西和描述一致, 成色比想象中还好, 邻居人也很客气!', '["http://localhost:9090/api/public/placeholder.svg?text=%E4%B9%B0%E5%AE%B6%E7%A7%80&w=300&h=300"]', 0, '感谢您的好评, 欢迎再次光临!', '2026-10-13 16:52:10', 1, '2026-10-13 10:52:10'),
(5, 1, 2, 1, 1, 5, '在小区门口当面交易的, 验完货没问题才付款, 很放心。', NULL, 0, '感谢您的好评, 欢迎再次光临!', '2026-10-13 16:52:10', 1, '2026-10-13 10:52:10'),
(10, 2, 3, 1, 2, 4, '价格实在, 物业中心代收的, 省了搬运的麻烦, 好评!', NULL, 0, NULL, NULL, 1, '2026-10-16 10:52:10'),
(3, 3, 4, 2, 1, 5, '有点小瑕疵但卖家提前说明了, 能接受, 性价比很高。', '["http://localhost:9090/api/public/placeholder.svg?text=%E4%B9%B0%E5%AE%B6%E7%A7%80&w=300&h=300"]', 0, NULL, NULL, 1, '2026-10-18 10:52:10'),
(7, 3, 5, 2, 1, 4, '卖家把东西收拾得很干净, 搬回家直接用, 没毛病。', NULL, 0, NULL, NULL, 1, '2026-10-18 10:52:10'),
(9, 5, 7, 3, 2, 5, '同一个小区的, 走路几分钟就取到了, 比买新的划算太多。', NULL, 0, NULL, NULL, 1, '2026-10-22 10:52:10'),
(13, 5, 8, 3, 2, 5, '东西和描述一致, 成色比想象中还好, 邻居人也很客气!', '["http://localhost:9090/api/public/placeholder.svg?text=%E4%B9%B0%E5%AE%B6%E7%A7%80&w=300&h=300"]', 0, NULL, NULL, 1, '2026-10-22 10:52:10'),
(19, 6, 9, 4, 3, 4, '在小区门口当面交易的, 验完货没问题才付款, 很放心。', NULL, 0, NULL, NULL, 1, '2026-10-23 10:52:10');

-- 公告
INSERT INTO `announcement` (`title`,`content`,`type`,`admin_id`,`is_top`,`status`,`publish_time`,`create_time`) VALUES
('关于平台每日维护的通知', '为保障交易数据安全, 平台每日凌晨 2 点至 5 点进行例行维护。维护期间将暂停访问, 敬请谅解!', 3, 1, 1, 1, '2026-10-23 10:52:10', '2026-10-23 10:52:10'),
('社区闲置置换节活动开启', '活动时间: 每月第一个周末。业主可集中转让家居、家电、母婴与图书类闲置, 活动期间免收平台服务费, 支持小区门口当面交易。', 2, 1, 1, 1, '2026-10-21 10:52:10', '2026-10-21 10:52:10'),
('新增 AI 智能导购功能上线公告', '平台已上线 AI 导购助手。告诉它你的预算和需求(例如"500 元以内的二手沙发"), 它会结合在售闲置为你推荐, 并可一键直达商品页。', 1, 1, 0, 1, '2026-10-19 10:52:10', '2026-10-19 10:52:10'),
('闲置发布规范与验货提醒', '发布闲置请如实填写成色与瑕疵, 禁止隐瞒维修史。建议在小区门口或物业服务中心等公共区域当面交易并当场验货, 家电数码请当场通电试机。', 1, 1, 0, 1, '2026-10-16 10:52:10', '2026-10-16 10:52:10'),
('关于加强账号安全的提醒', '请勿将账号密码告知他人, 建议定期修改密码。交易时切勿脱离平台私下转账, 谨防以"保证金""解冻费"为由的诈骗。', 1, 1, 0, 1, '2026-10-09 10:52:10', '2026-10-09 10:52:10');

-- 轮播图
INSERT INTO `banner` (`title`,`image`,`link_url`,`position`,`sort`,`status`) VALUES
('社区闲置置换节 低价好物', 'http://localhost:9090/api/public/placeholder.svg?text=%E7%A4%BE%E5%8C%BA%E9%97%B2%E7%BD%AE%E7%BD%AE%E6%8D%A2%E8%8A%82%20%E4%BD%8E%E4%BB%B7%E5%A5%BD%E7%89%A9&w=900&h=360', '/front/goodsDetail?pid=1', 1, 1, 1),
('家具家电专场 搬家清仓', 'http://localhost:9090/api/public/placeholder.svg?text=%E5%AE%B6%E5%85%B7%E5%AE%B6%E7%94%B5%E4%B8%93%E5%9C%BA%20%E6%90%AC%E5%AE%B6%E6%B8%85%E4%BB%93&w=900&h=360', '/front/goodsDetail?pid=13', 1, 2, 1),
('母婴用品 物业公益置换', 'http://localhost:9090/api/public/placeholder.svg?text=%E6%AF%8D%E5%A9%B4%E7%94%A8%E5%93%81%20%E7%89%A9%E4%B8%9A%E5%85%AC%E7%9B%8A%E7%BD%AE%E6%8D%A2&w=900&h=360', '/front/goodsDetail?pid=17', 1, 3, 1),
('数码专区 当面验机', 'http://localhost:9090/api/public/placeholder.svg?text=%E6%95%B0%E7%A0%81%E4%B8%93%E5%8C%BA%20%E5%BD%93%E9%9D%A2%E9%AA%8C%E6%9C%BA&w=900&h=360', '/front/search?keyword=手机', 2, 1, 1);

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================
-- 测试账号
--   超级管理员 : admin    / admin123
--   商家(4个)  : xunxi    / merchant123   (阳光社区业主自提点)
--                digimall / merchant123   (物业二手置换角)
--                hrbeauty / merchant123   (社区周边商户联盟)
--                kangmin  / merchant123   (业主闲置转让)
--   普通用户   : zhangsan / 123456
--                lisi     / 123456
--                test     / 123456
-- =============================================================