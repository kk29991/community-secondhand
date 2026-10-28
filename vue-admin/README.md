# 社区二手交易平台 - 后台管理端 (vue-admin)

中文电商毕设项目的后台管理端，**商家**与**超级管理员**共用同一套工程，按登录角色动态渲染菜单与路由，对接真实后端接口（`http://localhost:9090`）。

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 框架 | Vue 3（`<script setup>` 组合式 API）+ Vite 5 |
| 路由 | Vue Router 4（**hash 模式** `createWebHashHistory`） |
| 状态 | Pinia（`stores/user.js` 保存登录态与角色） |
| UI | Element Plus（全量引入）+ `@element-plus/icons-vue` |
| 图表 | ECharts 5 |
| 请求 | Axios（`src/api/request.js` 统一封装） |

主题色为小米橙 `#ff6700`，浅灰背景 + 白色卡片。

## 快速开始

```bash
npm install
npm run dev      # http://localhost:5175
npm run build    # 产物输出到 dist/
```

> 后端需先运行在 `http://localhost:9090`，Vite 已配置 `/api` 代理。

### 测试账号

| 角色 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 卖家 | `xunxi` | `merchant123` | 阳光社区业主自提点 |
| 卖家 | `digimall` | `merchant123` | 物业二手置换角 |
| 卖家 | `hrbeauty` | `merchant123` | 社区周边商户联盟 |
| 卖家 | `kangmin` | `merchant123` | 业主闲置转让 |
| 超级管理员 | `admin` | `admin123` | 平台管理 |

> 登录页的「填入卖家账号」按钮会**从接口动态读取**卖家列表，可轮流切换四个卖家，
> 因此卖家改名或新增都会自动同步，不会出现账号写死在页面上的问题。

## 目录结构

```
src/
├── api/
│   ├── request.js        # Axios 封装: 注入 token / 统一错误 / 401 处理
│   └── index.js          # 全部后端接口 (authApi / merchantApi / adminApi ...)
├── components/
│   ├── ChartBox.vue      # ECharts 容器 (自动 resize)
│   └── ImageUpload.vue   # 图片上传 (对接 /api/upload/image)
├── layout/
│   └── AdminLayout.vue   # 侧边菜单 + 顶部面包屑/用户下拉 + 内容区
├── router/index.js       # 路由表 + 路由守卫 (登录校验 / 角色越权拦截)
├── stores/user.js        # 用户 store (token / 角色 / 店铺信息)
├── styles/index.css      # 全局样式与主题变量
├── utils/format.js       # 金额/日期/状态字典等工具
└── views/
    ├── Login.vue
    ├── merchant/         # Dashboard / Products / Orders / Reviews / Tags
    └── admin/            # Overview / Statistics / Users / Merchants / Admins
                          # Categories / Products / Orders / Reviews
                          # Announcements / Banners
scripts/                  # 构建入口与受限环境兼容层 (见下)
```

## 页面与路由

**公共**：`/login`

**商家端（MERCHANT）**

| 路由 | 页面 |
| --- | --- |
| `/dashboard` | 经营看板：统计卡片 + 待处理事项 + 商品销量排行（柱状图） |
| `/products` | 商品管理：搜索/筛选/分页，新增·编辑抽屉（分类级联、主图与多图上传、标签多选），上下架、删除 |
| `/orders` | 订单管理：状态 Tab + 搜索 + 分页，详情抽屉，待发货可填快递单号发货 |
| `/reviews` | 评价管理：按待回复/已回复筛选，回复、修改回复、删除回复 |
| `/tags` | 标签管理：增删改（名称、颜色、排序） |

**管理员端（ADMIN）**

| 路由 | 页面 |
| --- | --- |
| `/admin/overview` | 平台概览：统计卡片 + 订单状态分布（饼图） |
| `/admin/statistics` | 销售统计：销售额趋势（折线）+ 商品销量排行（柱状），可切 7/30/90 天 |
| `/admin/users` | 用户管理：搜索、状态筛选、启用/禁用、删除 |
| `/admin/merchants` | 商家管理：搜索、审核/启用/禁用、删除 |
| `/admin/audit` | 商家入驻审核（复用商家管理页） |
| `/admin/admins` | 管理员列表 |
| `/admin/categories` | 分类管理：树形表格，增删改 |
| `/admin/products` | 平台商品：全平台列表、搜索、上下架 |
| `/admin/orders` | 平台订单：列表 + 详情 |
| `/admin/reviews` | 评价管理：显示/隐藏 |
| `/admin/announcements` | 公告管理：增删改查 |
| `/admin/banners` | 轮播图管理：增删改查、上传图片 |

## 已实现的机制

1. **路由守卫**：未登录跳 `/login`；已登录访问非本角色路由时提示无权限并回到自己的首页（商家访问 `/admin/**` 被拦截，后端同样返回 403）。
2. **动态菜单**：`AdminLayout.vue` 依据 `role` 渲染商家菜单或管理员菜单。
3. **Axios 封装**：自动注入 `Authorization: Bearer <token>`；`code != 200` 统一 `ElMessage.error`；401 清理登录态并跳登录页。
4. 所有表格页具备 loading、空状态、分页；所有删除操作有 `ElMessageBox.confirm` 二次确认。
5. 表单均配置 Element Plus `rules` 校验（必填、数字范围、长度、颜色格式、快递单号格式等）。

### 关于 401 / 403 的处理方式

后端把未登录（401）与越权（403）也放在 **HTTP 200 的响应体**里返回，例如：

```json
{ "code": 401, "message": "请先登录", "data": null }
```

因此 `src/api/request.js` 在业务层判断 `code`，而不是只看 HTTP 状态码。

## 受限环境下的构建适配（重要）

本工程默认使用 Vite 标准行为。但如果运行环境的沙箱**禁止子进程使用管道 stdio**，esbuild 会完全无法启动（一律 `spawn EPERM`），而 Vite 的 `vite:define` 插件在任何一次 build 中都必须调用 `esbuild.transform` 做 `process.env` / `import.meta.env` 替换，因此 `vite build` 会失败。

为此工程内置了一套**条件式**兼容层：

- `vite.config.js` 会先用 `esbuild.transformSync` 探测 esbuild 是否可用；
  - **可用**（普通开发机）：完全使用 Vite 默认行为，兼容层不生效；
  - **不可用**：关闭 esbuild 转换（`esbuild: false`）、改用 `terser` 压缩、关闭依赖预构建（`optimizeDeps.noDiscovery`）。
- `scripts/build.mjs` / `scripts/dev.mjs` 是 `npm run build` / `npm run dev` 的入口：用 Node 原生 `import` 读取配置并以 `configFile: false` 调用 Vite 的 JS API，避免 Vite CLI 用 esbuild 打包配置文件。
- `scripts/sandbox-compat.mjs` 处理两件事：
  1. 让 Vite 跳过 Windows 下的 `exec("net use")` 网络驱动器探测（同样依赖管道）；
  2. 检测到 esbuild 不可用时，用 `module.register()` 注册 ESM loader hook，把 `esbuild` 重定向到 `scripts/esbuild-shim.mjs`。
- `scripts/esbuild-shim.mjs` + `scripts/js-define.mjs` 是纯 JS 的 `define` 替换实现（词法扫描跳过字符串/注释/正则，长 key 优先，带标识符边界校验），行为与 esbuild 一致。

在 esbuild 正常的环境里，直接 `npx vite build` 或 `npm run build:vite` 亦可构建，与 `npm run build` 共用同一份配置。
