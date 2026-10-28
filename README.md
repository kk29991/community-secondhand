# 社区二手交易平台

> **基于 SpringBoot + Vue 的社区二手交易平台的设计与实现**

一个面向**城市社区（小区）**的**二手闲置交易平台**，基于 **Vue 3 + Spring Boot 3 + MySQL** 前后端分离实现，包含**用户端前台**与**后台管理端**（卖家 / 超级管理员共用）。

平台面向社区场景，卖家为**小区业主、物业服务中心与社区周边商户**，交易方式为**小区门口自提 / 物业中心代收**（同小区内无物流环节），核心功能覆盖"发布闲置 → 浏览检索 → 购物车/直接购买 → 下单 → 当面交易 → 确认收货 → 评价与回复"的完整闭环，并接入**大模型实现 AI 智能导购**。

功能范围依据用例图、ER 图与系统架构图实现：用户端 12 个用例、卖家端 3 个用例、管理员端 6 个用例，数据库 15 张表。

> 想快速跑起来？直接看 **三、快速开始**：建库 → 设置环境变量 → 启动后端 → 启动前端。

---

## 一、技术栈

| 层次 | 技术选型 |
|---|---|
| 前端 | Vue 3（组合式 API）、Vite 5、Vue Router 4（hash 模式）、Pinia、Element Plus、ECharts、Axios |
| 后端 | Spring Boot 3.5.13、MyBatis-Plus 3.5.9、JJWT 0.12.6、springdoc-openapi 2.8.9（接口文档） |
| 数据库 | MySQL 5.7.36（utf8mb4） |
| 运行时 | JDK 17、Node.js 18+、Maven 3.9 |

---

## 二、目录结构

```
社区二手交易平台/
├── backend/                          # 后端服务（Spring Boot，端口 9090）
│   ├── pom.xml
│   ├── src/main/java/com/mall/
│   │   ├── common/                   # 统一响应、业务异常、全局异常处理、登录上下文
│   │   ├── config/                   # MyBatis-Plus、Web（拦截器/静态资源）、字段填充、AI 配置
│   │   ├── controller/               # 接口层（11 个 Controller / 84 个接口）
│   │   ├── dto/                      # 请求与响应对象
│   │   ├── entity/                   # 15 张表对应实体
│   │   ├── interceptor/              # JWT 登录鉴权拦截器
│   │   ├── mapper/                   # MyBatis-Plus Mapper
│   │   ├── service/                  # 业务逻辑
│   │   └── util/                     # JWT、JSON 工具
│   ├── src/main/resources/
│   │   ├── application.yml           # 主配置（敏感项从环境变量读取）
│   │   └── application-local.yml.example  # 本地密钥配置模板
│   └── uploads/products/             # 闲置图片库（40 组 / 160 张 / 约 17 MB）
├── vue-mall/                         # 用户端前台（端口 5174）
├── vue-admin/                        # 后台管理端（卖家 / 管理员，端口 5175）
├── sql/
│   ├── schema.sql                    # 建库建表（15 张表）
│   ├── seed.sql                      # 种子数据（60 件闲置 / 4 卖家 / 22 个分类）
│   ├── generate-seed.js              # 种子数据生成器
│   └── smoke-test.js                 # 接口冒烟测试（99 项断言，可重复运行）
├── .mvn/settings.xml                 # Maven 配置
├── .gitignore
└── README.md
```

闲置图片存放于 `backend/uploads/products/`（40 组 / 160 张 / 约 17 MB），
随 `seed.sql` 一起被引用，因此已一并提交。

---

## 三、快速开始

### 0. 环境要求与环境变量

| 依赖 | 版本 |
|---|---|
| JDK | 17 |
| Maven | 3.6+ |
| Node.js | 18+ |
| MySQL | 5.7 |

**必须设置两个环境变量**（密码等敏感信息不写进代码仓库）：

```powershell
# 数据库密码（application.yml 中的 ${DB_PASSWORD}）
setx DB_PASSWORD "你的MySQL密码"

# 大模型密钥（可选；不设则 AI 自动降级为本地规则引擎）
setx DASHSCOPE_API_KEY "你的百炼API Key"
```

> **设完必须重新打开终端 / 重启 IDE**，否则新变量不会被已有进程继承。
> 验证：`echo $env:DB_PASSWORD` 能输出值即成功。

### 1. 准备数据库

```bash
mysql -uroot -p --default-character-set=utf8mb4 < sql/schema.sql
mysql -uroot -p --default-character-set=utf8mb4 < sql/seed.sql
```

或先改种子数据再导入：

```bash
node sql/generate-seed.js      # 重新生成 sql/seed.sql
```

> 数据库连接配置在 `backend/src/main/resources/application.yml`，默认库名 `shopping_mall`，
> 用户名 `${DB_USERNAME:root}`、密码 `${DB_PASSWORD}`。

### 2. 启动后端（端口 9090）

```bash
cd backend
mvn -s ../.mvn/settings.xml spring-boot:run
```

- 接口地址：`http://localhost:9090/api`
- 接口文档：`http://localhost:9090/api/swagger-ui/index.html`

看到下面这行即启动成功：

```
=====================================================
  社区二手交易平台后端启动成功
  接口文档: http://localhost:9090/api/swagger-ui/index.html
=====================================================
```

### 3. 启动用户端前台（端口 5174）

```bash
cd vue-mall
npm install
npm run dev          # 打开 http://127.0.0.1:5174
```

### 4. 启动后台管理端（端口 5175）

```bash
cd vue-admin
npm install
npm run dev          # 打开 http://127.0.0.1:5175
```

> **关于 `npm install`**：若你的环境限制了 npm 默认缓存目录的写入（本项目开发沙箱即如此），
> 可改用工作区内缓存：`npm install --cache .npm-cache`。

### 5. 测试账号

| 角色 | 账号 | 密码 | 说明 |
|---|---|---|---|
| 超级管理员 | `admin` | `admin123` | 平台管理 |
| 卖家 | `xunxi` | `merchant123` | 阳光社区业主自提点（家具·家电，16 件） |
| 卖家 | `digimall` | `merchant123` | 物业二手置换角（母婴·图书·小家电，14 件） |
| 卖家 | `hrbeauty` | `merchant123` | 社区周边商户联盟（数码·服饰鞋包，18 件） |
| 卖家 | `kangmin` | `merchant123` | 业主闲置转让（运动健身·日用·宠物，12 件） |
| 普通用户 | `zhangsan` | `123456` | 张三（含默认地址） |
| 普通用户 | `lisi` | `123456` | 李四 |
| 普通用户 | `test` | `123456` | 测试用户 |

> 共 **60 件闲置物品**、**7 个一级分类**（家居家具 / 家用电器 / 母婴用品 / 数码电子 / 服饰鞋包 / 图书文娱 / 其他闲置），下辖 15 个二级分类。
> 卖家即"发布闲置的小区业主 / 物业 / 社区周边商户"，因此卖家账号也可理解为社区卖家。

---

## 四、功能清单

### 用户端前台
- **注册 / 登录**：JWT 鉴权，登录态持久化
- **首页**：分类导航、轮播图、公告、最新闲置
- **浏览闲置**：分类筛选（含子分类）、关键词搜索、排序（综合 / 销量 / 价格）
- **闲置详情**：多图切换、**成色说明**、卖家信息与卖家标签、库存与销量、平均评分
- **直接购买 / 加入购物车**：重复加购自动累加，超库存拦截
- **购物车管理**：单选 / 全选、数量增减、删除、清空、按卖家分组
- **收货地址管理**：增删改查、默认地址切换（用于确定小区内交易地点）
- **订单管理**：结算预览（按卖家自动拆单）、下单、支付、取消、确认收货、退款
- **闲置收藏**：收藏 / 取消收藏（幂等切换）
- **闲置评价**：待评价列表、提交评价（评分 / 内容 / 匿名）、查看卖家回复
- **AI 智能导购**：意图识别 + 闲置检索，返回推荐闲置卡片，会话记录持久化

### 卖家端
- **经营看板**：在售闲置数、订单各状态数、成交额、平均评分、待回复评价数、销量排行
- **闲置管理**：发布 / 编辑 / 上架下架 / 删除，多图与标签维护，**成色字段**
- **订单处理**：订单查询、订单详情、确认交接（填写交接凭证）
- **回复评价**：查看收到的评价、回复 / 修改 / 删除回复
- **卖家标签管理**：标签增删改（如「小区自提」「当面验机」「可小刀」）

### 超级管理员端
- **平台概览**：用户数、卖家数、闲置数、订单各状态数、平台成交额
- **交易统计**：近 N 天成交额趋势、平台闲置销量排行
- **用户管理**：查询、启用 / 禁用、删除
- **卖家管理**：查询、审核 / 启用 / 禁用、删除（有在售闲置时禁止删除）
- **分类管理**：树形分类增删改（存在子分类时禁止删除父分类）
- **闲置管理**：全平台闲置查询、上下架
- **订单管理**：全平台订单查询与详情
- **评价管理**：评价查询、显示 / 隐藏
- **公告管理**：发布、编辑、删除
- **轮播图管理**：新增、编辑、删除、上传图片

---

## 五、数据库设计（15 张表）

| 表名 | 说明 |
|---|---|
| `user` | 普通用户 |
| `merchant` | 卖家（发布闲置的小区内学生 / 组织） |
| `admin` | 管理员 |
| `category` | 闲置分类（支持二级，如「数码电子 → 手机」） |
| `merchant_tag` | 卖家标签（如「小区自提」「当面验机」） |
| `product` | 闲置物品 |
| `product_collect` | 闲置收藏 |
| `cart` | 购物车 |
| `address` | 收货地址（小区内交易地点） |
| `orders` | 订单 |
| `order_item` | 订单明细（含成色与价格快照） |
| `product_review` | 闲置评价（含卖家回复） |
| `announcement` | 公告 |
| `banner` | 轮播图 |
| `ai_chat` | AI 对话记录 |

所有表显式声明 `utf8mb4`，不依赖服务端默认字符集；统一使用 `del_flag` 逻辑删除。

> **表名沿用了电商通用命名**（如 `product`、`merchant`、`cart`）。在社区二手场景下，
> 它们的语义分别是「闲置物品」「卖家」「购物车」，功能与结构完全一致，因此未做重命名，
> 以免引入大量无收益的改动风险。

---

## 六、关键实现说明

### 1. 统一的 `/api` 前缀
未使用 `server.servlet.context-path`，而是通过 `WebMvcConfig#configurePathMatch` 的 `addPathPrefix` 为所有 `@RestController` 统一加前缀。

> 原因：配置 `context-path` 后，拦截器的 `addPathPatterns` 是相对 context-path 的，极易与业务路径约定不一致，导致鉴权**静默失效**（本项目开发过程中就踩过这个坑）。

### 2. 鉴权策略
- `AuthInterceptor` 解析 `Authorization: Bearer <token>`，写入 `UserContext`（ThreadLocal）
- **入口先清空 `UserContext`**：Tomcat 复用工作线程，若上一个请求是流式（SSE）且被客户端中断，
  `afterCompletion` 不保证执行，残留登录态会被下一个未带 token 的请求"继承"——
  实测曾观察到未登录也能读到数据的现象。入口清空可彻底杜绝跨请求污染。
- 仅精确放行无需登录的接口：`/api/auth/login`、`/api/auth/register`、`/api/auth/logout`、`/api/public/**`
  （`/ai/status` 只返回"是否启用大模型"，属公开信息，故也无需登录）
- 流式接口 `POST /api/ai/chat/stream` 在**拦截器阶段**完成鉴权：
  SSE 响应一旦提交就无法再返回 JSON 错误，若留到 controller 再抛异常，
  会因内容协商不一致而返回空响应体（客户端既拿不到数据也拿不到错误）
- `/api/admin/merchant/**` 要求卖家身份，其余 `/api/admin/**` 要求管理员身份
- 未登录返回 `401`，越权返回 `403`（HTTP 状态均为 200，业务码在响应体 `code` 字段）

### 3. 防超卖
扣库存使用**数据库原子更新**，`WHERE` 中带库存充足条件，受影响行数为 0 即抛异常回滚整笔订单：

```sql
UPDATE product SET stock = stock - #{quantity}, sales = sales + #{quantity}
WHERE id = #{id} AND del_flag = 0 AND stock >= #{quantity}
```

并发测试已验证：库存为 1 时 5 个并发下单仅 1 单成功，库存正确归零。

### 4. 按卖家拆单
结算预览按 `merchant_id` 分组，下单时每组生成一个独立订单（同订单号规则 `yyyyMMddHHmmss + 4 位随机数`）。运费规则：单笔满 99 元免运费，否则 10 元。

### 5. AI 导购 / 在线客服
采用**大模型 + 本地规则引擎双层**设计，通过 OpenAI 兼容协议接入**通义千问（阿里云百炼 DashScope）**：

| 层 | 说明 |
|---|---|
| **大模型层** | 走 `https://dashscope.aliyuncs.com/compatible-mode/v1`，不引入任何厂商 SDK，只用 JDK 自带 `HttpClient`；支持 **SSE 流式输出**（逐字显示） |
| **规则引擎层（降级）** | 未配置密钥、或调用超时/报错时**自动降级**为「意图识别 + 关键词检索」。保证断网、未配 Key 也能正常演示 |

**提示词约束**：每次提问会先用本地检索挑出相关闲置，把**真实闲置目录**（名称/价格/成色/库存/卖家/卖点）注入 system 提示词，并在提示词中明确要求模型不得编造物品与价格、必须按预算筛选。平台规则（小区自提无物流费、满 99 免平台服务费、当面验货、二手不退换但支持申诉、防诈骗提醒等）也写在提示词里。

**接口**：

| 接口 | 说明 |
|---|---|
| `POST /api/ai/chat` | 一次性返回（保留兼容） |
| `POST /api/ai/chat/stream` | **SSE 流式**，事件：`delta`（增量文本）、`meta`（会话ID/意图/引擎/推荐闲置）、`done`、`error` |
| `GET /api/ai/status` | 返回 `{llmEnabled, model}`，前端据此显示"已接入 qwen-plus"或"本地规则引擎" |

> 前端用 `fetch` + `ReadableStream` 消费 SSE（原生 `EventSource` 只支持 GET、且不能带 `Authorization` 头）。

#### 启用大模型（3 步）

```powershell
# 1) 到百炼控制台创建 API Key: https://bailian.console.aliyun.com/  ->  API-KEY 管理
#    注意: 需要的是「百炼 API Key」(sk- 开头),
#    不是阿里云 AccessKey(那是管理 ECS/OSS 等云资源用的, 调不通模型服务)

# 2) 设置环境变量(PowerShell; cmd 用 set, Linux/macOS 用 export)
$env:DASHSCOPE_API_KEY="sk-你的key"

# 3) 重启后端, 然后自检是否已接入大模型
cd backend; mvn -s ../.mvn/settings.xml spring-boot:run
# 另开终端访问:
#   GET http://localhost:9090/api/ai/status
#   返回 {"llmEnabled":true,"model":"qwen-plus"} 即表示接入成功
```

也可以不使用环境变量，改为复制 `backend/src/main/resources/application-local.yml.example`
为 `application-local.yml` 并填入 `mall.ai.api-key`（该文件已被 `.gitignore` 忽略）。

可选环境变量：`DASHSCOPE_BASE_URL`（默认北京站，国际站为 `https://dashscope-intl.aliyuncs.com/compatible-mode/v1`）、`DASHSCOPE_MODEL`（默认 `qwen-plus`）。

**密钥安全**：密钥只从环境变量或 `application-local.yml` 读取，任何情况下都不写入
`application.yml`、代码或种子数据；`.gitignore` 已排除本地配置文件。

#### 实测边界（如实说明）
- 通义千问接口已实测打通：配置 API Key 后 `/api/ai/status` 返回 `llmEnabled: true`，
  AI 对话由模型生成（`引擎: llm`），未配置 Key 时自动降级为本地规则引擎（`引擎: rule`）。
- 已验证的场景：SSE 流式输出、无密钥降级路径、SSE 分块解析边界（逐字节切分 / UTF-8 被切断等 8 项）、
  预算约束（明确预算时不推荐超预算物品）、按分类检索兜底。
- 模型回答的**内容质量**受提示词与目录注入影响，属可调项；提示词中已硬性约束「只推荐目录内真实存在的闲置，
  不得编造名称与价格」。

### 6. 图片方案
闲置与轮播图使用**真实照片**，存放于后端图库目录 `backend/uploads/products/`，通过
`/api/uploads/**` 静态映射对外访问。

**图片来源与授权**：图片通过 [loremflickr.com](https://loremflickr.com/) 从 Flickr 检索的
**免费图库照片**（按关键词如 `mobile-phone`、`medicine` 取图）。
刻意**没有**从京东、品牌官网等电商站抓取商品摄影图——那属于有版权的商业素材，
放进课程设计或对外项目存在风险。具体来源清单见 `backend/uploads/products/CREDITS.md`
与 `manifest.json`（含每张图的来源 URL）。

图片已随仓库提交（`backend/uploads/products/`），克隆后即可直接看到图，无需再下载。
若需替换为其他图片，只要按同样的目录结构放入 `uploads/products/<名称>/main.jpg` 与
`gallery-1..3.jpg` 即可。

仍有 SVG 占位图接口可用，用于兜底或新增闲置时临时配图：

```
GET /api/public/placeholder.svg?text=闲置名&w=400&h=400
```

上传接口 `POST /api/upload/image` 已实现本地存储（保存到 `backend/uploads`）。

### 7. 前后端接口约定
后端**始终返回 HTTP 200**，业务状态放在响应体的 `code` 字段：

```json
{ "code": 401, "message": "请先登录", "data": null }
```

因此前端必须在业务层判断 `code`（`401` 未登录、`403` 越权、`400` 参数校验失败），
不能只看 HTTP 状态码。两个前端的 `src/api/request.js` 均已按此实现，包含 401 自动跳登录。

### 8. 前端构建兼容层（针对禁止子进程管道的环境）
部分受限环境（例如某些禁用了进程管道的开发沙箱）**禁止子进程使用管道 stdio**（`stdio: 'pipe'`），
这会导致 esbuild 无法启动（`spawn EPERM`），而 Vite 的配置加载、`vite:define` 与代码压缩
都依赖 esbuild，标准 `vite build` / `vite` 因此必然失败。

两个前端内置了**条件式**兼容层，**普通开发环境下完全不生效**：

| 机制 | 作用 |
|---|---|
| `vue-mall/scripts/sandbox-compat.mjs`（`vue-admin` 同） | 先探测 esbuild 是否可用；不可用时才启用下列替代方案。同时让 Vite 跳过 Windows 的 `net use` 网络盘探测（该探测同样依赖管道） |
| `vue-mall/scripts/esbuild-hooks.mjs` + `esbuild-shim.mjs` | 通过 `module.register()` 注册 ESM loader hook，把裸模块 `esbuild` 重定向为纯 JS 实现（供 `vite:define` 的变量替换使用） |
| `vue-mall/scripts/build.mjs` / `dev.mjs` | 用 Node 原生 `import` 读配置，以 `configFile: false` 调用 Vite JS API，避免 CLI 用 esbuild 打包配置文件 |
| `vite.config.js` 中的兜底段 | esbuild 不可用时关闭内置转换、改用 `terser` 压缩、关闭依赖预构建 |

在 esbuild 正常的普通环境里，`npx vite build` 与 `npm run build` 等价，共用同一份配置。

---

## 七、测试

### 接口冒烟测试（99 项断言）

后端启动后执行：

```bash
node sql/smoke-test.js
```

覆盖范围：公开接口、鉴权与越权（401 / 403）、购物车、按卖家拆单、库存扣减与并发防超卖、取消订单回滚库存、卖家交接与回复评价、卖家闲置管理、评价闭环、收藏、收货地址、AI 导购、管理端全部模块、个人资料。

测试的几个关键设计：

- **可重复执行**：自带收尾清理（按「冒烟测试」前缀删除自己造的数据、还原个人资料、清空购物车、取消残留待付款订单），连续多次执行都是 99/99，不会因残留状态失败。
- **数据驱动**：开头先查全量闲置，按名称与库存动态挑选被测数据，不硬编码 id。种子数据调整后测试仍然成立。
- **真并发验证**：先把目标闲置库存压到 1，再用 `Promise.all` 同时发 5 个下单请求，断言「只成功 1 单、库存归零」——这是验证防超卖最直接的方式（串行下单测不出竞态）。

### 测试结果

| 项目 | 结果 |
|---|---|
| 接口冒烟测试 | **99 / 99 通过**（连续多次可重复） |
| vue-mall 生产构建 | 成功 |
| vue-admin 生产构建 | 成功 |

> 开发期还编写过鉴权越权、AI 对话质量、图表渲染等辅助校验脚本，属本地开发工具，
> 未随本仓库提供；仓库中的 `sql/smoke-test.js` 是可直接运行的完整回归测试。

### 关于 `vite.config.js` 中的非标准配置

两个前端的 `vite.config.js` 里有几项看起来"多余"的配置，均为解决实际问题的必要处理：

- **`dayjs` 别名指向 ESM 版本**：`element-plus` 依赖的 dayjs 默认入口是 UMD 格式，
  浏览器只能执行 ESM，直接用会报 `does not provide an export named 'default'` 并导致页面白屏。
  因此把 `dayjs` 映射到 `dayjs/esm/index.js` 及其插件路径。
- **`server.watch = null`（部分环境下）**：某些受限环境（如无文件监听权限的沙箱）中，
  Vite 的 HTML 监听器会抛 `EBUSY` 使 dev server 整个退出。关闭监听后改为手动刷新页面。
  普通开发机上不触发该分支，热更新正常。
- **启动期错误兜底**：`index.html` 内联脚本在 8 秒内未完成挂载或发生脚本错误时，
  会在页面上直接显示红色错误卡片而非留白，便于定位问题。

---

## 八、说明

- 支付为**模拟支付**：下单时可直接置为「待发货」，或后续调用支付接口，不含真实支付渠道对接。
- 退款为简化处理：待发货 / 待收货状态可直接退款并回滚库存。
- 闲置详情、轮播图等使用 SVG 占位图，便于离线演示。
- 前端未做真实浏览器内的像素级渲染与点击验证，验证覆盖到「模块编译 + 真实数据链路 + 鉴权语义 + 生产构建」层面。

> **浏览器白屏排查**：若页面打开后一片空白，请先按 `F12` 查看控制台。
> 已实测确认服务端无问题（`127.0.0.1:5174` 的全部 401 个源码模块均返回 200、无编译错误，
> 生产构建产物也正常提供）。常见原因是 **Edge 的「增强安全模式」默认阻止 localhost 上的
> JavaScript**：设置 → 隐私、搜索和服务 → 「增强 Web 安全性」→ 为 `127.0.0.1` 添加例外，
> 或改用 Chrome 打开。此时 dev server 终端通常**不会**出现任何页面请求日志——这正是脚本
> 被浏览器拦截、根本没发出去的特征。
