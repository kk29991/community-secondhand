# 社区二手交易平台 · 用户端前台 (vue-mall)

基于 **Vue 3 + Vite + Vue Router 4 + Pinia + Element Plus + Axios** 的单页应用，
界面风格参考主流电商前端的信息布局（主色 `#ff6700`），对接 `http://localhost:9090` 的 Spring Boot 后端。

## 一、快速开始

```bash
npm install          # 建议在项目目录内执行
npm run dev          # 开发服务器: http://localhost:5174  (hash 路由)
npm run build        # 生产构建, 产物输出到 dist/
npm run preview      # 预览构建产物
```

> `/api` 由 Vite 代理到 `http://localhost:9090`（见 `vite.config.js`），浏览器侧无跨域。
> 演示账号：`zhangsan` / `123456`（普通用户）。

## 二、目录结构

```
vue-mall/
├── index.html
├── vite.config.js                 # 端口 5174 + /api 代理 + @ 别名
├── package.json
└── src/
    ├── main.js                    # 应用入口 (全量注册 Element Plus 与图标)
    ├── App.vue                    # 全局布局 (登录/注册页为独立 bare 布局)
    ├── api/
    │   ├── request.js             # 既有 axios 封装 (JWT 注入 / 解包 / 401 跳登录) —— 直接复用
    │   └── index.js               # 既有全部后端接口函数 —— 直接复用
    ├── router/index.js            # hash 模式路由 + 登录鉴权守卫 + 404 兜底
    ├── stores/
    │   ├── user.js                # 登录态 / 用户资料 / 购物车角标
    │   └── category.js            # 分类缓存 (首页导航与筛选共用)
    ├── utils/placeholder.js       # 后端 SVG 占位图工具 + 金额/时间/图片数组格式化
    ├── styles/main.css            # 全局样式与 Element Plus 主题覆盖 (小米橙)
    ├── components/
    │   ├── AppHeader.vue          # 顶部通栏 + 导航 + 搜索 + 用户菜单 + 购物车角标 + 分类条
    │   ├── AppFooter.vue
    │   └── ProductCard.vue        # 商品卡片 (网格/列表两种模式, 支持直接加购)
    └── views/
        ├── Home.vue               # 首页: 公告条 / 分类悬浮弹层 / 轮播 / 猜你喜欢
        ├── Search.vue             # 搜索与商品列表: 分类·排序筛选 + 分页
        ├── GoodsDetail.vue        # 商品详情: 主图切换 / 店铺标签 / 加购·立即购买·收藏 / 详情·评价 Tab
        ├── Cart.vue               # 购物车: 店铺分组 / 全选 / 数量 / 删除 / 清空 / 结算
        ├── ConfirmOrder.vue       # 确认订单: 地址增改 / 店铺分组清单 / 运费规则 / 备注 / 支付方式
        ├── PayResult.vue          # 支付结果: 支持 ?orderIds=1,2 与 ?orderId=1
        ├── AiShopping.vue         # AI 导购: 会话历史 / 推荐商品卡片 / 加购 / 清空会话
        ├── Login.vue              # 登录 (Tab 内含注册入口)
        ├── Register.vue           # 注册
        ├── NotFound.vue           # 404
        └── user/
            ├── UserLayout.vue     # 个人中心侧边栏 + 嵌套路由
            ├── Profile.vue        # 资料查看/修改 + 修改密码
            ├── Orders.vue         # 订单列表: 状态筛选 / 支付·取消·收货·退款
            ├── Collect.vue        # 我的收藏
            ├── Reviews.vue        # 待评价 + 提交评价弹窗 + 已发表评价
            └── Address.vue        # 收货地址增删改 + 设默认
```

## 三、路由一览（hash 模式，路径与需求文档一致）

| 路径 | 说明 | 需登录 |
| --- | --- | --- |
| `#/` | 首页 | 否 |
| `#/search?keyword=&categoryId=&sort=&pageNum=` | 商品列表 | 否 |
| `#/goodsDetail?pid=1`（兼容 `?id=1`） | 商品详情 | 否 |
| `#/cart` | 购物车 | 是 |
| `#/confirmOrder?cartIds=1,2` 或 `?productId=1&quantity=2` | 确认订单 | 是 |
| `#/payResult?orderIds=1,2`（兼容 `?orderId=1`） | 支付结果 | 是 |
| `#/user/profile｜orders｜collect｜reviews｜address` | 个人中心 | 是 |
| `#/ai` | AI 导购 | 是 |
| `#/login`、`#/register` | 登录 / 注册 | 否 |
| 其它任意路径 | 404 | 否 |

## 四、业务规则

- **分页结构**：MyBatis-Plus `{ records, total, size, current, pages }`。
- **排序**：`sort` 取值 `default | sales | priceAsc | priceDesc`。
- **运费**：按店铺分别计算，单店商品金额满 **99 元免运费**，否则每单 **10 元**（与后端 `OrderService` 一致）。
- **图片**：统一使用后端 SVG 占位图 `/api/public/placeholder.svg?text=..&w=..&h=..`，由 `utils/placeholder.js` 生成。
- **登录态**：token 存于 `localStorage`（键 `mall_mall_token`），`request.js` 自动注入 `Authorization`，401 自动清理并跳登录页。

## 五、离线自检脚本（可选）

`sandbox`（受限构建环境）下无法运行 Vite 时，可用这些脚本验证源码与接口：

```bash
npm run verify:static   # 语法 / 图标 / 路由一致性 / 路由守卫
npm run verify:sfc      # 用 Vue 官方编译器真实编译全部 .vue 组件
npm run verify:api      # 通过 src/api 层真实调用后端, 校验 50 项数据契约
npm run verify:auth     # 登录态持久化 / 资料读写 / 注册 / 401 行为
node --no-warnings --import ./scripts/verify/register.mjs scripts/verify/cleanup.mjs   # 清理自检产生的临时账号(需管理员)
```

> 注意：`npm run verify:*` 依赖 npm 通过管道捕获子进程输出；在禁止管道 stdio 的沙箱中，
> 直接用 `node` 执行对应的 `scripts/verify/*.mjs` 即可。
