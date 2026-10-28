/**
 * 生成 MySQL 种子数据 SQL。
 * 运行: node sql/generate-seed.js
 * 输出: sql/seed.sql
 *
 * 说明:
 * - 密码哈希由后端 SeedHashGeneratorTest 生成并回验, 明文见文件末尾注释。
 * - 图片使用后端 SVG 占位图接口, 不依赖外网图床。
 */
const fs = require('fs');
const path = require('path');

const API = 'http://localhost:9090/api';
const PW = {
  user: '$2a$10$SYryJ6S24k/EE08IXFS08ub8XdFs/ErgswHyYTplVoSnPgt9WIPtW', // 123456
  admin: '$2a$10$m7JBfva5.IADscF58GLK2uXdxqogZtIUmIuHcgSYJf6vId/RScefa', // admin123
  merchant: '$2a$10$VeMQA0s7Uip3cxecXDrUwOcqC4Dul/jl5mLAGMKO7g2UDpc.hKlJG', // merchant123
};

const IMG_MARK = /@@IMG:([^@]*)@@/g;
const q = (v) => (v === null || v === undefined ? 'NULL' : `'${String(v).replace(/'/g, "''")}'`);
const img = (text, w, h) => `@@IMG:${text}|${w}|${h}@@`;
/** 转义后替换为真实 URL */
const resolveImg = (s) =>
  s === null || s === undefined
    ? s
    : s.replace(IMG_MARK, (_, body) => {
        const [text, w, h] = body.split('|');
        return `${API}/public/placeholder.svg?text=${encodeURIComponent(text)}&w=${w}&h=${h}`;
      });

const out = [];
const w = (line = '') => out.push(line);

w('-- =============================================================');
w('--  社区二手交易平台 - 种子数据 (由 sql/generate-seed.js 生成)');
w('--  生成时间: ' + new Date().toISOString());
w('-- =============================================================');
w();
w('USE `shopping_mall`;');
w('SET NAMES utf8mb4;');
w('SET FOREIGN_KEY_CHECKS = 0;');
w();
w('TRUNCATE TABLE `user`;');
w('TRUNCATE TABLE `merchant`;');
w('TRUNCATE TABLE `admin`;');
w('TRUNCATE TABLE `category`;');
w('TRUNCATE TABLE `merchant_tag`;');
w('TRUNCATE TABLE `product`;');
w('TRUNCATE TABLE `product_collect`;');
w('TRUNCATE TABLE `cart`;');
w('TRUNCATE TABLE `address`;');
w('TRUNCATE TABLE `orders`;');
w('TRUNCATE TABLE `order_item`;');
w('TRUNCATE TABLE `product_review`;');
w('TRUNCATE TABLE `announcement`;');
w('TRUNCATE TABLE `banner`;');
w('TRUNCATE TABLE `ai_chat`;');
w();

// ---------------------------------------------------------------
// 管理员 (id=1)
// ---------------------------------------------------------------
w('-- 管理员: admin / admin123');
w('INSERT INTO `admin` (`id`,`username`,`password`,`nickname`,`avatar`,`role`,`status`) VALUES');
w(`(1, 'admin', ${q(PW.admin)}, '超级管理员', ${q(img('管理员', 120, 120))}, 'SUPER_ADMIN', 1);`);
w();

// ---------------------------------------------------------------
// 真实商品图片 (由 scripts/fetch-product-images.mjs 下载到后端图库)
// 目录: backend/uploads/products/<key>/main.jpg 与 gallery-1..3.jpg
// 未下载到本地时自动回退为 SVG 占位图, 保证脚本可独立运行。
// ---------------------------------------------------------------
const fsx = require('fs');
const productImgRoot = path.join(__dirname, '..', 'backend', 'uploads', 'products');
const fileImg = (key, variant = 'main') => {
  const rel = `products/${key}/${variant}.jpg`;
  const abs = path.join(productImgRoot, key, `${variant}.jpg`);
  return fsx.existsSync(abs) ? `${API}/uploads/${rel}` : null;
};
/** 取该商品的主图与轮播图, 全部缺失时回退到占位图 */
const imageSet = (p) => {
  const main = p.img ? fileImg(p.img, 'main') : null;
  const gallery = p.img
    ? [1, 2, 3].map((i) => fileImg(p.img, `gallery-${i}`)).filter(Boolean)
    : [];
  if (main) {
    return { main, gallery: gallery.length ? gallery : [main] };
  }
  return { main: img(p.name, 400, 400), gallery: null };
};

// ---------------------------------------------------------------
// 商家 (id=1..4) 密码 merchant123
// ---------------------------------------------------------------
// 社区二手交易平台: 商家即"发布闲置的卖家", 均为小区业主、物业或社区周边商户
const merchants = [
  { id: 1, username: 'xunxi', store: '阳光社区业主自提点', desc: '小区内自提 · 当面验货 · 邻里互助', contact: '业主委员会', phone: '13800000001', addr: '阳光社区 3 号楼 1 单元 101' },
  { id: 2, username: 'digimall', store: '物业二手置换角', desc: '物业主办 · 公益置换 · 绿色循环', contact: '物业服务中心', phone: '13800000002', addr: '阳光社区 物业服务中心 1 楼' },
  { id: 3, username: 'hrbeauty', store: '社区周边商户联盟', desc: '周边小店 · 可送货到门 · 当面验机', contact: '商户联盟', phone: '13800000003', addr: '阳光社区 南门临街商铺 12 号' },
  { id: 4, username: 'kangmin', store: '业主闲置转让', desc: '搬家清仓 · 大件家电全出 · 价格可谈', contact: '王女士', phone: '13800000004', addr: '阳光社区 7 号楼 2 单元 602' },
];
w('-- 商家 (登录密码均为 merchant123)');
w('INSERT INTO `merchant` (`id`,`username`,`password`,`store_name`,`store_logo`,`store_desc`,`contact_name`,`contact_phone`,`address`,`status`) VALUES');
merchants.forEach((m, i) => {
  const line = `(${m.id}, ${q(m.username)}, ${q(PW.merchant)}, ${q(m.store)}, ${q(img(m.store, 160, 160))}, ${q(m.desc)}, ${q(m.contact)}, ${q(m.phone)}, ${q(m.addr)}, 1)`;
  w(line + (i === merchants.length - 1 ? ';' : ','));
});
w();

// ---------------------------------------------------------------
// 商家标签 (社区二手: 强调交易方式与邻里保障)
// 1 阳光社区业主自提点 : 小区内自提 / 支持验货
// 2 物业二手置换角     : 物业认证 / 公益置换
// 3 社区周边商户联盟   : 可送货到门 / 当面验机
// 4 业主闲置转让       : 搬家清仓 / 价格可谈
// ---------------------------------------------------------------
const tags = [
  { id: 1, mid: 1, name: '小区内自提', color: '#52c41a' },
  { id: 2, mid: 1, name: '支持验货', color: '#13c2c2' },
  { id: 3, mid: 2, name: '物业认证', color: '#597ef7' },
  { id: 4, mid: 2, name: '公益置换', color: '#faad14' },
  { id: 5, mid: 3, name: '可送货到门', color: '#eb2f96' },
  { id: 6, mid: 3, name: '当面验机', color: '#9254de' },
  { id: 7, mid: 4, name: '搬家清仓', color: '#52c41a' },
  { id: 8, mid: 4, name: '价格可谈', color: '#13c2c2' },
];
w('-- 商家标签');
w('INSERT INTO `merchant_tag` (`id`,`merchant_id`,`name`,`color`,`sort`) VALUES');
tags.forEach((t, i) => {
  w(`(${t.id}, ${t.mid}, ${q(t.name)}, ${q(t.color)}, ${i})` + (i === tags.length - 1 ? ';' : ','));
});
w();

// ---------------------------------------------------------------
// 分类: 一级 + 二级 (社区二手场景, 以小区为定位)
//   1 家居家具   2 家用电器   3 母婴用品   4 数码电子
//   5 服饰鞋包   6 图书文娱   7 其他闲置
// ---------------------------------------------------------------
const categories = [
  { id: 1, pid: 0, name: '家居家具', sort: 1 },
  { id: 2, pid: 0, name: '家用电器', sort: 2 },
  { id: 3, pid: 0, name: '母婴用品', sort: 3 },
  { id: 4, pid: 0, name: '数码电子', sort: 4 },
  { id: 5, pid: 0, name: '服饰鞋包', sort: 5 },
  { id: 6, pid: 0, name: '图书文娱', sort: 6 },
  { id: 7, pid: 0, name: '其他闲置', sort: 7 },
  { id: 8, pid: 1, name: '沙发桌椅', sort: 1 },
  { id: 9, pid: 1, name: '收纳置物', sort: 2 },
  { id: 10, pid: 2, name: '厨房电器', sort: 1 },
  { id: 11, pid: 2, name: '生活电器', sort: 2 },
  { id: 12, pid: 3, name: '婴儿车床', sort: 1 },
  { id: 13, pid: 3, name: '儿童玩具', sort: 2 },
  { id: 14, pid: 4, name: '手机', sort: 1 },
  { id: 15, pid: 4, name: '电脑平板', sort: 2 },
  { id: 16, pid: 4, name: '影音配件', sort: 3 },
  { id: 17, pid: 5, name: '外套上装', sort: 1 },
  { id: 18, pid: 5, name: '鞋靴箱包', sort: 2 },
  { id: 19, pid: 6, name: '书籍教材', sort: 1 },
  { id: 20, pid: 6, name: '儿童读物', sort: 2 },
  { id: 21, pid: 7, name: '运动健身', sort: 1 },
  { id: 22, pid: 7, name: '宠物用品', sort: 2 },
];
w('-- 商品分类 (一级 + 二级)');
w('INSERT INTO `category` (`id`,`parent_id`,`name`,`icon`,`sort`,`status`) VALUES');
categories.forEach((c, i) => {
  w(`(${c.id}, ${c.pid}, ${q(c.name)}, NULL, ${c.sort}, 1)` + (i === categories.length - 1 ? ';' : ','));
});
w();

// ---------------------------------------------------------------
// 商品 (社区二手闲置, 以小区为定位)
// 字段说明:
//   cond: 成色, 会写入 detail 详情(<p>成色: ...</p>)
//   img : 复用 backend/uploads/products 下已有的图片目录 key
// ---------------------------------------------------------------
const products = [
  // ===== 阳光社区业主自提点 (merchant 1): 家具 + 厨房电器 + 部分生活电器 =====
  { mid: 1, cid: 8, img: 'home-sofa', name: '布艺沙发 三人位 灰色', cond: '9成新', sub: '三人位 2.1 米, 布艺可拆洗, 坐垫无塌陷, 因换风格出', price: 450, op: 2800, stock: 1, sales: 0, tags: [1, 2] },
  { mid: 1, cid: 8, img: 'home-sofa', name: '实木餐桌 四人位 带四椅', cond: '8成新', sub: '1.4 米实木餐桌配四把椅子, 桌面有轻微使用痕迹, 结构稳固', price: 320, op: 1600, stock: 1, sales: 0, tags: [1] },
  { mid: 1, cid: 8, img: 'home-kitchen', name: '书桌 白色简约 120×60', cond: '9成新', sub: '桌面无划痕, 抽屉顺滑, 自提需自行拆装', price: 120, op: 499, stock: 1, sales: 0, tags: [1, 2] },
  { mid: 1, cid: 8, img: 'home-lamp', name: '人体工学电脑椅 网布', cond: '8成新', sub: '可升降调节, 腰托完好, 网布无破损', price: 180, op: 799, stock: 1, sales: 0, tags: [1] },
  { mid: 1, cid: 9, img: 'other-book', name: '实木书架 五层', cond: '8成新', sub: '1.8 米高五层, 承重好, 有正常使用痕迹', price: 180, op: 690, stock: 1, sales: 0, tags: [1] },
  { mid: 1, cid: 9, img: 'home-kitchen', name: '塑料收纳箱 三层带轮', cond: '9成新', sub: '三层抽屉式, 带轮好推, 装玩具杂物都合适', price: 35, op: 99, stock: 2, sales: 0, tags: [1, 2] },
  { mid: 1, cid: 9, img: 'cloth-bag', name: '落地衣帽架 简约款', cond: '9成新', sub: '实木挂杆, 可挂外套和帽子, 组装完好', price: 28, op: 89, stock: 2, sales: 0, tags: [1] },
  { mid: 1, cid: 9, img: 'home-kitchen', name: '三层木质鞋柜', cond: '8成新', sub: '1.2 米宽三层, 木纹饰面, 柜门开合正常', price: 90, op: 320, stock: 1, sales: 0, tags: [1] },
  { mid: 1, cid: 10, img: 'home-kitchen', name: '美的电饭煲 4L', cond: '9成新', sub: '4L 容量够三口之家, 内胆无涂层脱落, 附蒸笼', price: 80, op: 299, stock: 1, sales: 0, tags: [1, 2] },
  { mid: 1, cid: 10, img: 'home-kitchen', name: '格兰仕微波炉 20L', cond: '8成新', sub: '机械旋钮款, 加热正常, 转盘完好', price: 150, op: 499, stock: 1, sales: 0, tags: [1] },
  { mid: 1, cid: 10, img: 'home-coffee', name: '九阳豆浆机 1.2L', cond: '9成新', sub: '可打豆浆米糊, 刀头锋利, 已清洗消毒', price: 60, op: 259, stock: 1, sales: 0, tags: [1] },
  { mid: 1, cid: 10, img: 'home-kitchen', name: '美的电磁炉 2200W', cond: '9成新', sub: '火力足, 面板无划痕, 送汤锅一只', price: 55, op: 199, stock: 2, sales: 0, tags: [1, 2] },
  { mid: 1, cid: 11, img: 'home-kitchen', name: '海尔波轮洗衣机 8kg', cond: '7成新', sub: '8 公斤容量, 洗涤脱水正常, 外观有轻微磕碰, 功能无碍', price: 600, op: 1899, stock: 1, sales: 0, tags: [1] },
  { mid: 1, cid: 11, img: 'home-coffee', name: '小米空气净化器 2S', cond: '8成新', sub: '滤芯剩余约三成, 建议自购更换, 机器运行正常', price: 320, op: 899, stock: 1, sales: 0, tags: [1, 2] },
  { mid: 1, cid: 11, img: 'home-kitchen', name: '电压力锅 6L 双胆', cond: '8成新', sub: '双内胆一锅两用, 密封圈完好, 附说明书', price: 120, op: 399, stock: 1, sales: 0, tags: [1] },
  { mid: 1, cid: 11, img: 'home-lamp', name: '落地扇 静音遥控款', cond: '8成新', sub: '三档风速可遥控, 扇叶已清洗, 夏季必备', price: 40, op: 159, stock: 2, sales: 0, tags: [1] },

  // ===== 物业二手置换角 (merchant 2): 母婴 + 图书 + 小家电 =====
  { mid: 2, cid: 12, img: 'other-toy', name: '好孩子婴儿推车 可折叠', cond: '9成新', sub: '轻便可折叠, 一键收车, 遮阳篷完好, 已清洗消毒', price: 280, op: 899, stock: 1, sales: 0, tags: [3, 4] },
  { mid: 2, cid: 12, img: 'other-toy', name: '实木婴儿床 带护栏', cond: '8成新', sub: '1.2 米实木床, 可调高度, 床板完好, 需自提', price: 320, op: 1299, stock: 1, sales: 0, tags: [3] },
  { mid: 2, cid: 12, img: 'other-toy', name: '儿童安全座椅 3-12 岁', cond: '9成新', sub: 'ISOFIX 接口, 无碰撞记录, 布套已拆洗', price: 260, op: 899, stock: 1, sales: 0, tags: [3, 4] },
  { mid: 2, cid: 13, img: 'other-toy', name: '儿童滑板车 三轮稳固款', cond: '8成新', sub: '轮子顺滑, 适合 3-6 岁, 把手可调高度', price: 45, op: 159, stock: 2, sales: 0, tags: [3] },
  { mid: 2, cid: 13, img: 'other-toy', name: '儿童积木桌 带收纳', cond: '9成新', sub: '桌面可翻转为积木底板, 附一袋大颗粒积木', price: 60, op: 199, stock: 1, sales: 0, tags: [3, 4] },
  { mid: 2, cid: 13, img: 'other-toy', name: '大颗粒积木 一箱约 200 颗', cond: '8成新', sub: '已清洗消毒, 配件齐全, 适合 3 岁以上', price: 120, op: 399, stock: 1, sales: 0, tags: [3] },
  { mid: 2, cid: 19, img: 'other-book', name: '《三体》全集 三册', cond: '9成新', sub: '无划线无笔记, 书脊完好, 科幻必读', price: 45, op: 108, stock: 2, sales: 0, tags: [3, 4] },
  { mid: 2, cid: 19, img: 'other-book', name: '考研英语真题 黄皮书', cond: '7成新', sub: '有少量铅笔笔记可擦除, 内容完整', price: 35, op: 129, stock: 1, sales: 0, tags: [3] },
  { mid: 2, cid: 20, img: 'other-book', name: '儿童绘本 30 册 套装', cond: '8成新', sub: '3-6 岁适读, 无撕页, 已擦拭消毒', price: 50, op: 200, stock: 2, sales: 0, tags: [3, 4] },
  { mid: 2, cid: 20, img: 'other-book', name: '小学生课外读物 一套 8 册', cond: '9成新', sub: '注音版, 几乎全新, 适合一二年级', price: 35, op: 120, stock: 2, sales: 0, tags: [3] },
  { mid: 2, cid: 11, img: 'home-lamp', name: '护眼台灯 三档调光', cond: '9成新', sub: '无频闪, USB 供电, 孩子写作业用', price: 35, op: 109, stock: 2, sales: 0, tags: [3, 4] },
  { mid: 2, cid: 11, img: 'home-coffee', name: '家用加湿器 4L 静音', cond: '8成新', sub: '大容量静音夜灯款, 已除垢清洁', price: 60, op: 199, stock: 1, sales: 0, tags: [3] },
  { mid: 2, cid: 11, img: 'home-kitchen', name: '手持挂烫机', cond: '9成新', sub: '出汽快, 水箱完好, 附防烫手套', price: 90, op: 299, stock: 1, sales: 0, tags: [3, 4] },
  { mid: 2, cid: 11, img: 'home-kitchen', name: '油汀式电暖器 11 片', cond: '7成新', sub: '制热正常, 带加湿盒, 冬季取暖用, 较重需自提', price: 120, op: 399, stock: 1, sales: 0, tags: [3] },

  // ===== 社区周边商户联盟 (merchant 3): 数码 + 服饰鞋包 =====
  { mid: 3, cid: 14, img: 'phone-flagship', name: 'iPhone 13 128G 星光色', cond: '9成新', sub: '国行双卡, 电池健康 88%, 无维修记录, 可当面验机查序列号', price: 2100, op: 5999, stock: 1, sales: 0, tags: [5, 6] },
  { mid: 3, cid: 14, img: 'phone-flagship', name: '小米 13 12+256 徕卡影像', cond: '9成新', sub: '骁龙 8Gen2, 屏幕无划痕, 原装充电器齐全', price: 1450, op: 4599, stock: 1, sales: 0, tags: [5] },
  { mid: 3, cid: 14, img: 'phone-budget', name: '荣耀 Play 6T 8+128', cond: '8成新', sub: '备用机出售, 功能一切正常, 适合老人机或备用', price: 380, op: 1099, stock: 2, sales: 0, tags: [5, 6] },
  { mid: 3, cid: 14, img: 'phone-midrange', name: 'Redmi Note 12 Turbo 8+256', cond: '9成新', sub: '无磕碰无划痕, 一直带壳贴膜, 配件齐全', price: 899, op: 1799, stock: 1, sales: 0, tags: [5, 6] },
  { mid: 3, cid: 15, img: 'tablet', name: 'iPad 第 9 代 64G WiFi', cond: '9成新', sub: '看网课记笔记好用, 带保护套和钢化膜', price: 1350, op: 2499, stock: 1, sales: 0, tags: [5, 6] },
  { mid: 3, cid: 15, img: 'tablet', name: '小米平板 6 8+128', cond: '9成新', sub: '144Hz 高刷屏, 追剧办公都合适, 附磁吸壳', price: 980, op: 1999, stock: 1, sales: 0, tags: [5] },
  { mid: 3, cid: 15, img: 'laptop-ultra', name: '联想小新 Air14 轻薄本', cond: '8成新', sub: 'i5-1240P / 16G+512G, 办公学习够用, 电池健康 85%', price: 2350, op: 4999, stock: 1, sales: 0, tags: [5, 6] },
  { mid: 3, cid: 15, img: 'laptop-gaming', name: '华硕天选 4 游戏本 RTX4060', cond: '9成新', sub: 'R7-7735H / 16G+512G, 键盘无油光, 可现场跑分验证', price: 4600, op: 8299, stock: 1, sales: 0, tags: [5] },
  { mid: 3, cid: 16, img: 'headphones', name: '索尼 WH-1000XM4 头戴降噪', cond: '9成新', sub: '降噪效果依旧顶级, 带原装收纳包', price: 1050, op: 2299, stock: 1, sales: 0, tags: [5, 6] },
  { mid: 3, cid: 16, img: 'earbuds', name: 'AirPods 2 代 有线充版', cond: '8成新', sub: '左耳轻微使用痕迹, 音质正常, 续航约 4 小时', price: 320, op: 999, stock: 1, sales: 0, tags: [5] },
  { mid: 3, cid: 16, img: 'camera', name: '佳能 EOS 200D II 单反套机', cond: '8成新', sub: '含 18-55mm 镜头, 快门约 8000 次, 适合家用记录', price: 2200, op: 4299, stock: 1, sales: 0, tags: [5, 6] },
  { mid: 3, cid: 16, img: 'monitor', name: 'AOC 24 英寸 1080P 显示器', cond: '8成新', sub: '外接办公用, 无坏点, 附 HDMI 线', price: 380, op: 899, stock: 1, sales: 0, tags: [5] },
  { mid: 3, cid: 16, img: 'keyboard', name: '罗技 K380 无线键盘', cond: '9成新', sub: '可连三个设备, 静音手感, 附电池', price: 95, op: 249, stock: 2, sales: 0, tags: [5] },
  { mid: 3, cid: 17, img: 'cloth-jeans', name: 'Lee 牛仔外套 男 L 码', cond: '9成新', sub: '正品, 穿过几次, 无破损无褪色, 已干洗', price: 120, op: 599, stock: 1, sales: 0, tags: [5, 6] },
  { mid: 3, cid: 17, img: 'cloth-sweater', name: '优衣库摇粒绒外套 女 M 码', cond: '9成新', sub: '秋冬保暖, 无起球, 已清洗消毒', price: 65, op: 249, stock: 1, sales: 0, tags: [5] },
  { mid: 3, cid: 17, img: 'cloth-sweater', name: '美式复古卫衣 男女同款 L', cond: '9成新', sub: '厚实不透, 洗过两次不掉色', price: 55, op: 199, stock: 2, sales: 0, tags: [5] },
  { mid: 3, cid: 18, img: 'cloth-bag', name: 'JanSport 双肩背包 大容量', cond: '9成新', sub: '能装 15.6 寸电脑, 拉链顺滑, 无破损', price: 85, op: 359, stock: 1, sales: 0, tags: [5, 6] },
  { mid: 3, cid: 18, img: 'cloth-bag', name: '帆布单肩托特包', cond: '9成新', sub: '装书装电脑都行, 文艺百搭, 可机洗', price: 28, op: 99, stock: 3, sales: 0, tags: [5] },

  // ===== 业主闲置转让 (merchant 4): 运动健身 + 生活电器 + 宠物用品 =====
  { mid: 4, cid: 21, img: 'other-bike', name: '捷安特 ATX 660 山地车 27.5 寸', cond: '8成新', sub: '搬家出, 变速链条正常, 刚换后胎, 送车锁和打气筒', price: 680, op: 1899, stock: 1, sales: 0, tags: [7, 8] },
  { mid: 4, cid: 21, img: 'other-bike', name: '凤凰折叠自行车 20 寸', cond: '8成新', sub: '可折叠放阳台, 骑行正常, 适合短途买菜代步', price: 260, op: 699, stock: 1, sales: 0, tags: [7, 8] },
  { mid: 4, cid: 21, img: 'other-toy', name: '家用折叠跑步机', cond: '7成新', sub: '折叠收纳不占地, 电机运行正常, 较重需自提', price: 600, op: 1999, stock: 1, sales: 0, tags: [7, 8] },
  { mid: 4, cid: 21, img: 'other-toy', name: '瑜伽垫 加厚防滑 183×61cm', cond: '9成新', sub: '用了几次, 已清洁消毒, 附收纳绑带', price: 30, op: 99, stock: 2, sales: 0, tags: [7] },
  { mid: 4, cid: 21, img: 'other-toy', name: '可调节哑铃 20kg 一对', cond: '8成新', sub: '片数齐全, 自提较重, 不邮寄', price: 130, op: 399, stock: 1, sales: 0, tags: [7, 8] },
  { mid: 4, cid: 21, img: 'other-toy', name: '尤尼克斯羽毛球拍 双拍套装', cond: '8成新', sub: '含拍套和三个球, 网线还紧, 适合入门', price: 110, op: 399, stock: 1, sales: 0, tags: [7] },
  { mid: 4, cid: 11, img: 'home-coffee', name: '小米电热水壶 1.5L', cond: '8成新', sub: '烧水快, 自动断电, 已除垢清洁', price: 45, op: 129, stock: 2, sales: 0, tags: [7, 8] },
  { mid: 4, cid: 11, img: 'printer', name: '小熊家用吹风机', cond: '9成新', sub: '大功率速干, 恒温不伤发', price: 50, op: 159, stock: 2, sales: 0, tags: [7] },
  { mid: 4, cid: 11, img: 'home-lamp', name: '静音小风扇 USB 充电款', cond: '9成新', sub: '三档风速, 可夹床头, 夏天续命神器', price: 25, op: 79, stock: 3, sales: 0, tags: [7] },
  { mid: 4, cid: 22, img: 'other-toy', name: '猫爬架 三层 实木', cond: '8成新', sub: '猫抓柱完好, 平台稳固, 已清洁消毒', price: 80, op: 299, stock: 1, sales: 0, tags: [7, 8] },
  { mid: 4, cid: 22, img: 'other-toy', name: '宠物航空箱 中号', cond: '9成新', sub: '适合 10kg 内猫狗, 门锁正常, 带饮水碗', price: 60, op: 199, stock: 2, sales: 0, tags: [7] },
  { mid: 4, cid: 22, img: 'other-toy', name: '全封闭猫砂盆 带铲', cond: '9成新', sub: '防外溅设计, 附猫砂铲, 已彻底清洗', price: 45, op: 159, stock: 2, sales: 0, tags: [7] },
];

w('-- 商品');
w('INSERT INTO `product` (`id`,`merchant_id`,`category_id`,`name`,`sub_title`,`main_image`,`images`,`detail`,`price`,`original_price`,`stock`,`sales`,`unit`,`tag_ids`,`status`) VALUES');
const productRows = products.map((p, i) => {
  const id = i + 1;
  p._id = id;
  const { main, gallery } = imageSet(p);
  // 有本地真实图片时用真实图片, 否则回退为占位图
  const images = gallery
    ? JSON.stringify(gallery)
    : JSON.stringify([img(p.name, 400, 400), img(p.name + ' 细节', 400, 400)]);
  // 社区二手详情: 突出成色、交易方式与验货, 与"全新正品电商"的文案区分开
  const cond = p.cond || '9成新';
  const seller = merchants.find((m) => m.id === p.mid).store;
  const detail = [
    `<p>${p.sub}</p>`,
    `<p>成色: ${cond} | 交易方式: 小区门口自提 / 物业中心代收</p>`,
    `<p>该闲置由「${seller}」发布。建议当面验货确认无误后再签收, 请放心交易。</p>`,
    `<p>温馨提示: 二手物品不支持无理由退货, 请确认成色与描述一致后再下单。</p>`,
  ].join('');
  return `(${id}, ${p.mid}, ${p.cid}, ${q(p.name)}, ${q(p.sub)}, ${q(main)}, ${q(images)}, ${q(detail)}, ${p.price}, ${p.op}, ${p.stock}, 0, '件', ${q(p.tags.length ? p.tags.join(',') : null)}, 1)`;
});
w(productRows.join(',\n') + ';');
w();

// ---------------------------------------------------------------
// 用户 (密码 123456)
// ---------------------------------------------------------------
const users = [
  { id: 1, username: 'zhangsan', nick: '张三', phone: '13900000001', email: 'zhangsan@example.com', gender: 1 },
  { id: 2, username: 'lisi', nick: '李四', phone: '13900000002', email: 'lisi@example.com', gender: 2 },
  { id: 3, username: 'wangwu', nick: '王五', phone: '13900000003', email: 'wangwu@example.com', gender: 1 },
  { id: 4, username: 'zhaoliu', nick: '赵六', phone: '13900000004', email: 'zhaoliu@example.com', gender: 2 },
  { id: 5, username: 'test', nick: '测试用户', phone: '13900000005', email: 'test@example.com', gender: 0 },
];
w('-- 普通用户 (登录密码均为 123456)');
w('INSERT INTO `user` (`id`,`username`,`password`,`nickname`,`avatar`,`phone`,`email`,`gender`,`status`) VALUES');
users.forEach((u, i) => {
  w(`(${u.id}, ${q(u.username)}, ${q(PW.user)}, ${q(u.nick)}, ${q(img(u.nick, 120, 120))}, ${q(u.phone)}, ${q(u.email)}, ${u.gender}, 1)` + (i === users.length - 1 ? ';' : ','));
});
w();

// ---------------------------------------------------------------
// 收货地址
// ---------------------------------------------------------------
const addresses = [
  { id: 1, uid: 1, name: '张三', phone: '13900000001', p: '湖北省', c: '武汉市', d: '洪山区', detail: '珞喻路 1037 号华中科技大学 韵苑公寓', def: 1 },
  { id: 2, uid: 1, name: '张三(公司)', phone: '13900000001', p: '湖北省', c: '武汉市', d: '东湖新技术开发区', detail: '光谷软件园 F 栋 12 层', def: 0 },
  { id: 3, uid: 2, name: '李四', phone: '13900000002', p: '山东省', c: '济南市', d: '历下区', detail: '经十路 8888 号 某某小区 5 栋 302', def: 1 },
  { id: 4, uid: 3, name: '王五', phone: '13900000003', p: '广东省', c: '深圳市', d: '南山区', detail: '科技园南路 18 号 腾讯大厦', def: 1 },
  { id: 5, uid: 5, name: '测试用户', phone: '13900000005', p: '北京市', c: '北京市', d: '海淀区', detail: '中关村大街 1 号', def: 1 },
];
w('-- 收货地址');
w('INSERT INTO `address` (`id`,`user_id`,`receiver_name`,`receiver_phone`,`province`,`city`,`district`,`detail_address`,`is_default`) VALUES');
addresses.forEach((a, i) => {
  w(`(${a.id}, ${a.uid}, ${q(a.name)}, ${q(a.phone)}, ${q(a.p)}, ${q(a.c)}, ${q(a.d)}, ${q(a.detail)}, ${a.def})` + (i === addresses.length - 1 ? ';' : ','));
});
w();

// ---------------------------------------------------------------
// 购物车
// ---------------------------------------------------------------
const carts = [
  { uid: 1, pid: 1, qty: 2, checked: 1 },
  { uid: 1, pid: 3, qty: 1, checked: 1 },
  { uid: 1, pid: 13, qty: 1, checked: 0 },
  { uid: 2, pid: 17, qty: 1, checked: 1 },
  { uid: 3, pid: 10, qty: 1, checked: 1 },
  { uid: 5, pid: 9, qty: 1, checked: 1 },
];
w('-- 购物车');
w('INSERT INTO `cart` (`user_id`,`product_id`,`quantity`,`checked`) VALUES');
carts.forEach((c, i) => {
  w(`(${c.uid}, ${c.pid}, ${c.qty}, ${c.checked})` + (i === carts.length - 1 ? ';' : ','));
});
w();

// ---------------------------------------------------------------
// 收藏
// ---------------------------------------------------------------
const collects = [
  { uid: 1, pid: 1 }, { uid: 1, pid: 9 }, { uid: 2, pid: 3 },
  { uid: 3, pid: 17 }, { uid: 4, pid: 10 }, { uid: 5, pid: 13 },
];
w('-- 商品收藏');
w('INSERT INTO `product_collect` (`user_id`,`product_id`) VALUES');
collects.forEach((c, i) => {
  w(`(${c.uid}, ${c.pid})` + (i === collects.length - 1 ? ';' : ','));
});
w();

// ---------------------------------------------------------------
// 订单 + 明细 (状态 2 待收货 / 3 已完成)
// 约定: 满 99 免运费, 否则 10 元
// ---------------------------------------------------------------
const fmt = (d) => d.toISOString().slice(0, 19).replace('T', ' ');
const now = Date.now();
const day = 24 * 60 * 60 * 1000;

/** 订单定义: 每个订单属于一个商家, 便于按店铺拆单 */
const orderDefs = [
  { user: 1, addr: 1, mid: 1, status: 3, daysAgo: 12, items: [{ pid: 1, qty: 2 }, { pid: 5, qty: 1 }], remark: '请尽快发货, 谢谢' },
  { user: 1, addr: 1, mid: 2, status: 3, daysAgo: 9, items: [{ pid: 10, qty: 1 }], remark: null },
  { user: 2, addr: 3, mid: 1, status: 3, daysAgo: 7, items: [{ pid: 3, qty: 1 }, { pid: 7, qty: 2 }], remark: '无' },
  { user: 2, addr: 3, mid: 3, status: 2, daysAgo: 4, items: [{ pid: 18, qty: 1 }], remark: '需要礼品包装' },
  { user: 3, addr: 4, mid: 2, status: 3, daysAgo: 3, items: [{ pid: 9, qty: 1 }, { pid: 13, qty: 2 }], remark: null },
  { user: 4, addr: null, mid: 3, status: 3, daysAgo: 2, items: [{ pid: 19, qty: 1 }], remark: '发顺丰' },
  { user: 5, addr: 5, mid: 1, status: 2, daysAgo: 1, items: [{ pid: 2, qty: 1 }, { pid: 4, qty: 1 }], remark: '测试订单' },
  { user: 3, addr: 4, mid: 1, status: 0, daysAgo: 0, items: [{ pid: 6, qty: 1 }], remark: '待付款订单' },
];

const salesById = {};
const reviews = [];
const orderRows = [];
const itemRows = [];
let orderId = 0;
let itemId = 0;
const addressById = Object.fromEntries(addresses.map((a) => [a.id, a]));

for (const od of orderDefs) {
  orderId += 1;
  const addr = od.addr ? addressById[od.addr] : { name: '李四', phone: '13900000002', p: '山东省', c: '济南市', d: '历下区', detail: '经十路 8888 号 某某小区 5 栋 302' };
  const created = new Date(now - od.daysAgo * day);
  const goods = od.items.reduce((s, it) => s + products[it.pid - 1].price * it.qty, 0);
  const freight = goods >= 99 ? 0 : 10;
  const payAmount = goods + freight;
  const orderNo = `${created.toISOString().slice(0, 10).replace(/-/g, '')}${String(100000 + orderId)}`;
  const payTime = od.status === 0 ? null : fmt(new Date(created.getTime() + 5 * 60 * 1000));
  const deliveryTime = od.status === 2 || od.status === 3 ? fmt(new Date(created.getTime() + day)) : null;
  const receiveTime = od.status === 3 ? fmt(new Date(created.getTime() + 3 * day)) : null;
  const finishTime = od.status === 3 ? fmt(new Date(created.getTime() + 3 * day)) : null;
  const expressNo = deliveryTime ? 'SF' + (1000000000 + orderId * 137) : null;
  const addrText = `${addr.p}${addr.c}${addr.d}${addr.detail}`;

  orderRows.push(
    `(${orderId}, ${q(orderNo)}, ${od.user}, ${od.mid}, ${goods.toFixed(2)}, ${payAmount.toFixed(2)}, ${freight.toFixed(2)}, ${od.status}, 2, ` +
      `${payTime ? q(payTime) : 'NULL'}, ${deliveryTime ? q(deliveryTime) : 'NULL'}, ${receiveTime ? q(receiveTime) : 'NULL'}, ` +
      `${finishTime ? q(finishTime) : 'NULL'}, NULL, NULL, ${q(addr.name)}, ${q(addr.phone)}, ${q(addrText)}, ${q(od.remark)}, ` +
      `${expressNo ? q(expressNo) : 'NULL'}, ${q(fmt(created))})`
  );

  for (const it of od.items) {
    itemId += 1;
    const p = products[it.pid - 1];
    const commented = od.status === 3 ? 1 : 0;
    itemRows.push(
      `(${itemId}, ${orderId}, ${p._id}, ${q(p.name)}, ${q(img(p.name, 200, 200))}, ${p.price}, ${it.qty}, ${(p.price * it.qty).toFixed(2)}, ${commented})`
    );
    if (od.status === 2 || od.status === 3) {
      salesById[p._id] = (salesById[p._id] || 0) + it.qty;
    }
    // 已完成的订单生成评价
    if (od.status === 3) {
      reviews.push({
        pid: p._id, oid: orderId, oiid: itemId, uid: od.user, mid: od.mid,
        score: [5, 5, 4, 5, 4][reviews.length % 5],
        content: [
          '东西和描述一致, 成色比想象中还好, 邻居人也很客气!',
          '在小区门口当面交易的, 验完货没问题才付款, 很放心。',
          '价格实在, 物业中心代收的, 省了搬运的麻烦, 好评!',
          '有点小瑕疵但卖家提前说明了, 能接受, 性价比很高。',
          '卖家把东西收拾得很干净, 搬回家直接用, 没毛病。',
          '同一个小区的, 走路几分钟就取到了, 比买新的划算太多。',
        ][reviews.length % 6],
        daysAgo: od.daysAgo - 1,
        reply: null,
      });
    }
  }
}

w('-- 订单');
w('INSERT INTO `orders` (`id`,`order_no`,`user_id`,`merchant_id`,`total_amount`,`pay_amount`,`freight`,`status`,`pay_type`,`pay_time`,`delivery_time`,`receive_time`,`finish_time`,`cancel_time`,`cancel_reason`,`receiver_name`,`receiver_phone`,`receiver_address`,`remark`,`express_no`,`create_time`) VALUES');
w(orderRows.join(',\n') + ';');
w();

w('-- 订单明细');
w('INSERT INTO `order_item` (`id`,`order_id`,`product_id`,`product_name`,`product_image`,`price`,`quantity`,`total_price`,`commented`) VALUES');
w(itemRows.join(',\n') + ';');
w();

// 回填销量: 与订单明细保持一致
w('-- 根据订单明细回填商品销量, 保证看板数据自洽');
for (const [pid, sales] of Object.entries(salesById)) {
  w(`UPDATE \`product\` SET \`sales\` = ${sales} WHERE \`id\` = ${pid};`);
}
w();

// ---------------------------------------------------------------
// 评价 (部分带商家回复)
// ---------------------------------------------------------------
w('-- 商品评价 (部分含商家回复)');
w('INSERT INTO `product_review` (`product_id`,`order_id`,`order_item_id`,`user_id`,`merchant_id`,`score`,`content`,`images`,`anonymous`,`reply_content`,`reply_time`,`status`,`create_time`) VALUES');
const reviewRows = reviews.map((r, i) => {
  const created = new Date(now - Math.max(r.daysAgo, 1) * day);
  // 前 2 条给商家回复, 其余留作"待回复"便于演示回复功能
  const replied = i < 2;
  const replyContent = replied ? '感谢您的好评, 欢迎再次光临!' : null;
  const replyTime = replied ? fmt(new Date(created.getTime() + 6 * 60 * 60 * 1000)) : null;
  const images = i % 3 === 0 ? JSON.stringify([img('买家秀', 300, 300)]) : null;
  return `(${r.pid}, ${r.oid}, ${r.oiid}, ${r.uid}, ${r.mid}, ${r.score}, ${q(r.content)}, ${images ? q(images) : 'NULL'}, 0, ${replyContent ? q(replyContent) : 'NULL'}, ${replyTime ? q(replyTime) : 'NULL'}, 1, ${q(fmt(created))})`;
});
w(reviewRows.join(',\n') + ';');
w();

// ---------------------------------------------------------------
// 公告
// ---------------------------------------------------------------
const announcements = [
  { title: '关于平台每日维护的通知', type: 3, top: 1, days: 1, content: '为保障交易数据安全, 平台每日凌晨 2 点至 5 点进行例行维护。维护期间将暂停访问, 敬请谅解!' },
  { title: '社区闲置置换节活动开启', type: 2, top: 1, days: 3, content: '活动时间: 每月第一个周末。业主可集中转让家居、家电、母婴与图书类闲置, 活动期间免收平台服务费, 支持小区门口当面交易。' },
  { title: '新增 AI 智能导购功能上线公告', type: 1, top: 0, days: 5, content: '平台已上线 AI 导购助手。告诉它你的预算和需求(例如"500 元以内的二手沙发"), 它会结合在售闲置为你推荐, 并可一键直达商品页。' },
  { title: '闲置发布规范与验货提醒', type: 1, top: 0, days: 8, content: '发布闲置请如实填写成色与瑕疵, 禁止隐瞒维修史。建议在小区门口或物业服务中心等公共区域当面交易并当场验货, 家电数码请当场通电试机。' },
  { title: '关于加强账号安全的提醒', type: 1, top: 0, days: 15, content: '请勿将账号密码告知他人, 建议定期修改密码。交易时切勿脱离平台私下转账, 谨防以"保证金""解冻费"为由的诈骗。' },
];
w('-- 公告');
w('INSERT INTO `announcement` (`title`,`content`,`type`,`admin_id`,`is_top`,`status`,`publish_time`,`create_time`) VALUES');
announcements.forEach((a, i) => {
  const t = fmt(new Date(now - a.days * day));
  w(`(${q(a.title)}, ${q(a.content)}, ${a.type}, 1, ${a.top}, 1, ${q(t)}, ${q(t)})` + (i === announcements.length - 1 ? ';' : ','));
});
w();

// ---------------------------------------------------------------
// 轮播图
// ---------------------------------------------------------------
const banners = [
  { title: '社区闲置置换节 低价好物', link: '/front/goodsDetail?pid=1', pos: 1, sort: 1 },
  { title: '家具家电专场 搬家清仓', link: '/front/goodsDetail?pid=13', pos: 1, sort: 2 },
  { title: '母婴用品 物业公益置换', link: '/front/goodsDetail?pid=17', pos: 1, sort: 3 },
  { title: '数码专区 当面验机', link: '/front/search?keyword=手机', pos: 2, sort: 1 },
];
w('-- 轮播图');
w('INSERT INTO `banner` (`title`,`image`,`link_url`,`position`,`sort`,`status`) VALUES');
banners.forEach((b, i) => {
  w(`(${q(b.title)}, ${q(img(b.title, 900, 360))}, ${q(b.link)}, ${b.pos}, ${b.sort}, 1)` + (i === banners.length - 1 ? ';' : ','));
});
w();

w('SET FOREIGN_KEY_CHECKS = 1;');
w();
w('-- =============================================================');
w('-- 测试账号');
w('--   超级管理员 : admin    / admin123');
w('--   商家(4个)  : xunxi    / merchant123   (阳光社区业主自提点)');
w('--                digimall / merchant123   (物业二手置换角)');
w('--                hrbeauty / merchant123   (社区周边商户联盟)');
w('--                kangmin  / merchant123   (业主闲置转让)');
w('--   普通用户   : zhangsan / 123456');
w('--                lisi     / 123456');
w('--                test     / 123456');
w('-- =============================================================');

const sql = resolveImg(out.join('\n'));
const target = path.join(__dirname, 'seed.sql');
fs.writeFileSync(target, sql, 'utf8');
console.log(`已生成 ${target}`);
console.log(`  商品 ${products.length} 条, 订单 ${orderId} 条, 明细 ${itemId} 条, 评价 ${reviews.length} 条`);
console.log(`  剩余 @@IMG 占位符: ${(sql.match(/@@IMG:/g) || []).length} (应为 0)`);
