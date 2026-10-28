-- =============================================================
--  社区二手交易平台 - 数据库结构定义
--  目标库: MySQL 5.7.36  (显式 utf8mb4, 不依赖服务端默认字符集)
--  说明:  服务端 character_set_server 为 latin1, 因此本脚本中
--         每张表都显式声明 ENGINE / CHARSET / COLLATE。
-- =============================================================

DROP DATABASE IF EXISTS `shopping_mall`;
CREATE DATABASE `shopping_mall`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `shopping_mall`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -------------------------------------------------------------
-- 1. 普通用户表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(64)  NOT NULL                COMMENT '登录用户名',
    `password`    VARCHAR(128) NOT NULL                COMMENT 'BCrypt 加密后的密码',
    `nickname`    VARCHAR(64)           DEFAULT NULL   COMMENT '昵称',
    `avatar`      VARCHAR(512)          DEFAULT NULL   COMMENT '头像地址',
    `phone`       VARCHAR(20)           DEFAULT NULL   COMMENT '手机号',
    `email`       VARCHAR(128)          DEFAULT NULL   COMMENT '邮箱',
    `gender`      TINYINT      NOT NULL DEFAULT 0      COMMENT '性别: 0未知 1男 2女',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0禁用 1正常',
    `del_flag`    TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0未删 1已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    KEY `idx_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='普通用户表';

-- -------------------------------------------------------------
-- 2. 商家表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `merchant`;
CREATE TABLE `merchant` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '卖家ID',
    `username`      VARCHAR(64)  NOT NULL                COMMENT '登录用户名',
    `password`      VARCHAR(128) NOT NULL                COMMENT 'BCrypt 加密后的密码',
    `store_name`    VARCHAR(128) NOT NULL                COMMENT '店铺名称',
    `store_logo`    VARCHAR(512)          DEFAULT NULL   COMMENT '店铺Logo',
    `store_desc`    VARCHAR(512)          DEFAULT NULL   COMMENT '店铺简介',
    `contact_name`  VARCHAR(64)           DEFAULT NULL   COMMENT '联系人',
    `contact_phone` VARCHAR(20)           DEFAULT NULL   COMMENT '联系电话',
    `address`       VARCHAR(255)          DEFAULT NULL   COMMENT '店铺地址',
    `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0禁用/待审核 1正常 2拒绝',
    `del_flag`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0未删 1已删',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入驻时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_merchant_username` (`username`),
    KEY `idx_merchant_store_name` (`store_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商家表';

-- -------------------------------------------------------------
-- 3. 管理员表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    `username`    VARCHAR(64)  NOT NULL                COMMENT '登录用户名',
    `password`    VARCHAR(128) NOT NULL                COMMENT 'BCrypt 加密后的密码',
    `nickname`    VARCHAR(64)           DEFAULT NULL   COMMENT '昵称',
    `avatar`      VARCHAR(512)          DEFAULT NULL   COMMENT '头像地址',
    `role`        VARCHAR(32)  NOT NULL DEFAULT 'SUPER_ADMIN' COMMENT '角色标识',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0禁用 1正常',
    `del_flag`    TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0未删 1已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理员表';

-- -------------------------------------------------------------
-- 4. 商品分类表 (支持二级分类, parent_id=0 为一级)
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0      COMMENT '父分类ID, 0为顶级',
    `name`        VARCHAR(64)  NOT NULL                COMMENT '分类名称',
    `icon`        VARCHAR(512)          DEFAULT NULL   COMMENT '分类图标',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序值, 越小越靠前',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0隐藏 1显示',
    `del_flag`    TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0未删 1已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品分类表';

-- -------------------------------------------------------------
-- 5. 商家标签表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `merchant_tag`;
CREATE TABLE `merchant_tag` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `merchant_id` BIGINT       NOT NULL                COMMENT '所属卖家ID',
    `name`        VARCHAR(64)  NOT NULL                COMMENT '标签名称',
    `color`       VARCHAR(32)           DEFAULT NULL   COMMENT '标签颜色',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序值',
    `del_flag`    TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0未删 1已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tag_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商家标签表';

-- -------------------------------------------------------------
-- 6. 商品表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `merchant_id`   BIGINT        NOT NULL                COMMENT '所属卖家ID',
    `category_id`   BIGINT        NOT NULL                COMMENT '所属分类ID',
    `name`          VARCHAR(255)  NOT NULL                COMMENT '商品名称',
    `sub_title`     VARCHAR(512)           DEFAULT NULL   COMMENT '副标题/卖点',
    `main_image`    VARCHAR(512)           DEFAULT NULL   COMMENT '主图地址',
    `images`        TEXT                   DEFAULT NULL   COMMENT '轮播图地址 JSON 数组',
    `detail`        LONGTEXT               DEFAULT NULL   COMMENT '商品详情(富文本)',
    `price`         DECIMAL(10,2) NOT NULL                COMMENT '售价',
    `original_price` DECIMAL(10,2)         DEFAULT NULL   COMMENT '原价/划线价',
    `stock`         INT           NOT NULL DEFAULT 0      COMMENT '库存',
    `sales`         INT           NOT NULL DEFAULT 0      COMMENT '累计销量',
    `unit`          VARCHAR(16)            DEFAULT '件'   COMMENT '计量单位',
    `tag_ids`       VARCHAR(255)           DEFAULT NULL   COMMENT '关联商家标签ID, 逗号分隔',
    `status`        TINYINT       NOT NULL DEFAULT 1      COMMENT '状态: 0下架 1上架',
    `del_flag`      TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0未删 1已删',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_merchant` (`merchant_id`),
    KEY `idx_product_category` (`category_id`),
    KEY `idx_product_status` (`status`),
    KEY `idx_product_name` (`name`),
    KEY `idx_product_sales` (`sales`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品表';

-- -------------------------------------------------------------
-- 7. 商品收藏表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `product_collect`;
CREATE TABLE `product_collect` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `user_id`     BIGINT   NOT NULL                COMMENT '用户ID',
    `product_id`  BIGINT   NOT NULL                COMMENT '商品ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collect_user_product` (`user_id`, `product_id`),
    KEY `idx_collect_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品收藏表';

-- -------------------------------------------------------------
-- 8. 购物车表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
    `user_id`     BIGINT   NOT NULL                COMMENT '用户ID',
    `product_id`  BIGINT   NOT NULL                COMMENT '商品ID',
    `quantity`    INT      NOT NULL DEFAULT 1      COMMENT '购买数量',
    `checked`     TINYINT  NOT NULL DEFAULT 1      COMMENT '是否勾选: 0否 1是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cart_user_product` (`user_id`, `product_id`),
    KEY `idx_cart_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车表';

-- -------------------------------------------------------------
-- 9. 收货地址表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `user_id`       BIGINT       NOT NULL                COMMENT '用户ID',
    `receiver_name` VARCHAR(64)  NOT NULL                COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20) NOT NULL                COMMENT '收货人手机号',
    `province`      VARCHAR(64)           DEFAULT NULL   COMMENT '省',
    `city`          VARCHAR(64)           DEFAULT NULL   COMMENT '市',
    `district`      VARCHAR(64)           DEFAULT NULL   COMMENT '区/县',
    `detail_address` VARCHAR(255) NOT NULL               COMMENT '详细地址',
    `is_default`    TINYINT      NOT NULL DEFAULT 0      COMMENT '是否默认: 0否 1是',
    `del_flag`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0未删 1已删',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_address_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='收货地址表';

-- -------------------------------------------------------------
-- 10. 订单表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`       VARCHAR(64)   NOT NULL                COMMENT '订单编号',
    `user_id`        BIGINT        NOT NULL                COMMENT '下单用户ID',
    `merchant_id`    BIGINT        NOT NULL                COMMENT '所属卖家ID',
    `total_amount`   DECIMAL(10,2) NOT NULL                COMMENT '订单总金额',
    `pay_amount`     DECIMAL(10,2) NOT NULL                COMMENT '实付金额',
    `freight`        DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '运费',
    `status`         TINYINT       NOT NULL DEFAULT 0      COMMENT '状态: 0待付款 1待发货 2待收货 3已完成 4已取消 5已退款',
    `pay_type`       TINYINT       NOT NULL DEFAULT 1      COMMENT '支付方式: 1余额 2支付宝 3微信',
    `pay_time`       DATETIME               DEFAULT NULL   COMMENT '支付时间',
    `delivery_time`  DATETIME               DEFAULT NULL   COMMENT '发货时间',
    `receive_time`   DATETIME               DEFAULT NULL   COMMENT '收货时间',
    `finish_time`    DATETIME               DEFAULT NULL   COMMENT '完成时间',
    `cancel_time`    DATETIME               DEFAULT NULL   COMMENT '取消时间',
    `cancel_reason`  VARCHAR(255)           DEFAULT NULL   COMMENT '取消原因',
    `receiver_name`  VARCHAR(64)   NOT NULL                COMMENT '收货人姓名(下单快照)',
    `receiver_phone` VARCHAR(20)   NOT NULL                COMMENT '收货人手机号(下单快照)',
    `receiver_address` VARCHAR(512) NOT NULL               COMMENT '收货地址(下单快照)',
    `remark`         VARCHAR(512)           DEFAULT NULL   COMMENT '买家备注',
    `express_no`     VARCHAR(64)            DEFAULT NULL   COMMENT '快递单号',
    `del_flag`       TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0未删 1已删',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_orders_order_no` (`order_no`),
    KEY `idx_orders_user` (`user_id`),
    KEY `idx_orders_merchant` (`merchant_id`),
    KEY `idx_orders_status` (`status`),
    KEY `idx_orders_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';

-- -------------------------------------------------------------
-- 11. 订单明细表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `order_id`      BIGINT        NOT NULL                COMMENT '订单ID',
    `product_id`    BIGINT        NOT NULL                COMMENT '商品ID',
    `product_name`  VARCHAR(255)  NOT NULL                COMMENT '商品名称(快照)',
    `product_image` VARCHAR(512)           DEFAULT NULL   COMMENT '商品主图(快照)',
    `price`         DECIMAL(10,2) NOT NULL                COMMENT '成交单价(快照)',
    `quantity`      INT           NOT NULL                COMMENT '购买数量',
    `total_price`   DECIMAL(10,2) NOT NULL                COMMENT '小计金额',
    `commented`     TINYINT       NOT NULL DEFAULT 0      COMMENT '是否已评价: 0否 1是',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_item_order` (`order_id`),
    KEY `idx_order_item_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单明细表';

-- -------------------------------------------------------------
-- 12. 商品评价表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `product_review`;
CREATE TABLE `product_review` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `product_id`    BIGINT       NOT NULL                COMMENT '商品ID',
    `order_id`      BIGINT       NOT NULL                COMMENT '关联订单ID',
    `order_item_id` BIGINT       NOT NULL                COMMENT '关联订单明细ID',
    `user_id`       BIGINT       NOT NULL                COMMENT '评价用户ID',
    `merchant_id`   BIGINT       NOT NULL                COMMENT '所属卖家ID',
    `score`         TINYINT      NOT NULL DEFAULT 5      COMMENT '评分 1-5',
    `content`       VARCHAR(1000)         DEFAULT NULL   COMMENT '评价内容',
    `images`        TEXT                  DEFAULT NULL   COMMENT '评价图片 JSON 数组',
    `anonymous`     TINYINT      NOT NULL DEFAULT 0      COMMENT '是否匿名: 0否 1是',
    `reply_content` VARCHAR(1000)         DEFAULT NULL   COMMENT '卖家回复内容',
    `reply_time`    DATETIME              DEFAULT NULL   COMMENT '卖家回复时间',
    `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0隐藏 1显示',
    `del_flag`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0未删 1已删',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_review_order_item` (`order_item_id`),
    KEY `idx_review_product` (`product_id`),
    KEY `idx_review_merchant` (`merchant_id`),
    KEY `idx_review_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品评价表';

-- -------------------------------------------------------------
-- 13. 公告表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `title`       VARCHAR(255) NOT NULL                COMMENT '公告标题',
    `content`     TEXT                  DEFAULT NULL   COMMENT '公告内容',
    `type`        TINYINT      NOT NULL DEFAULT 1      COMMENT '类型: 1系统公告 2活动通知 3维护通知',
    `admin_id`    BIGINT                DEFAULT NULL   COMMENT '发布管理员ID',
    `is_top`      TINYINT      NOT NULL DEFAULT 0      COMMENT '是否置顶: 0否 1是',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0下线 1发布',
    `publish_time` DATETIME             DEFAULT NULL   COMMENT '发布时间',
    `del_flag`    TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0未删 1已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_announcement_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公告表';

-- -------------------------------------------------------------
-- 14. 轮播图表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '轮播图ID',
    `title`       VARCHAR(255)          DEFAULT NULL   COMMENT '标题',
    `image`       VARCHAR(512) NOT NULL                COMMENT '图片地址',
    `link_url`    VARCHAR(512)          DEFAULT NULL   COMMENT '跳转链接',
    `position`    TINYINT      NOT NULL DEFAULT 1      COMMENT '位置: 1首页轮播 2首页广告位',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序值, 越小越靠前',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0下线 1上线',
    `del_flag`    TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0未删 1已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_banner_position` (`position`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='轮播图表';

-- -------------------------------------------------------------
-- 15. AI 对话记录表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `ai_chat`;
CREATE TABLE `ai_chat` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '对话记录ID',
    `user_id`     BIGINT      NOT NULL                COMMENT '用户ID',
    `session_id`  VARCHAR(64) NOT NULL                COMMENT '会话ID, 同一次会话共用',
    `role`        VARCHAR(16) NOT NULL                COMMENT '消息角色: user/assistant',
    `content`     TEXT                 DEFAULT NULL   COMMENT '消息内容',
    `intent`      VARCHAR(64)          DEFAULT NULL   COMMENT '识别到的意图',
    `ref_product_ids` VARCHAR(255)     DEFAULT NULL   COMMENT '推荐商品ID, 逗号分隔',
    `del_flag`    TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0未删 1已删',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_ai_user` (`user_id`),
    KEY `idx_ai_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI对话记录表';

SET FOREIGN_KEY_CHECKS = 1;
