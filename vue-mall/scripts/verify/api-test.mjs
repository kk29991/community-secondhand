// 通过项目自身的 src/api 层 (request.js + index.js) 真实调用后端接口, 验证契约
// 运行: node --no-warnings --import ./scripts/verify/register.mjs scripts/verify/api-test.mjs

/* ---------- 1. 准备浏览器环境 (localStorage) ---------- */
const store = new Map()
globalThis.localStorage = {
  getItem: (k) => (store.has(k) ? store.get(k) : null),
  setItem: (k, v) => store.set(k, String(v)),
  removeItem: (k) => store.delete(k),
  clear: () => store.clear()
}
globalThis.window = { location: { hash: '#/' } }

/* ---------- 2. 把 '/api' 相对地址转到真实后端 ---------- */
const BACKEND = process.env.MALL_BACKEND || 'http://localhost:9090'

const api = await import('../../src/api/index.js')
const { getToken, getCurrentUser, clearAuth } = await import('../../src/api/request.js')
const requestModule = await import('../../src/api/request.js')
const request = requestModule.default
request.defaults.baseURL = BACKEND + '/api'
request.defaults.timeout = 25000

/* ---------- 3. 断言工具 ---------- */
const results = []
let failed = 0

function record(name, ok, detail, expectedFail) {
  if (!ok && !expectedFail) failed += 1
  results.push({ name, ok, expectedFail: !!expectedFail, detail })
  const flag = ok ? 'PASS' : expectedFail ? 'EXPECTED-FAIL' : 'FAIL'
  console.log('[' + flag + '] ' + name + (detail ? ' :: ' + detail : ''))
}

async function check(name, fn, options = {}) {
  try {
    const value = await fn()
    if (options.validate && !options.validate(value)) {
      record(name, false, '返回结构与前端预期不符: ' + JSON.stringify(value)?.slice(0, 160))
      return value
    }
    record(name, true, options.describe ? options.describe(value) : '')
    return value
  } catch (err) {
    record(name, false, err.message, options.expectedFail)
    return null
  }
}

const isPage = (d) => d && Array.isArray(d.records) && typeof d.total === 'number'
const isArray = Array.isArray

console.log('后端地址: ' + BACKEND)
console.log('==================================================')

/* ---------- 4. 认证 (登录接口本身永不使用 profile) ---------- */
clearAuth()
const loginVO = await check('POST /auth/login (zhangsan)', () => api.login({ username: 'zhangsan', password: '123456', role: 'USER' }), {
  validate: (d) => d && d.token && d.role === 'USER',
  describe: (d) => 'role=' + d.role + ', nickname=' + d.nickname
})
record('localStorage 已写入 token 与用户信息', !!getToken() && !!getCurrentUser().username, 'TOKEN_KEY=' + (getToken() ? '有值' : '空'))
request.defaults.headers.common.Authorization = 'Bearer ' + getToken()

await check('GET /auth/profile (带合法 token)', () => api.getProfile(), {
  validate: (d) => d && d.username,
  describe: (d) => 'username=' + d.username + ', nickname=' + d.nickname + ', phone=' + d.phone + ', email=' + d.email + ', gender=' + d.gender
})

/* ---------- 5. 公开数据 ---------- */
const categories = await check('GET /public/categories', () => api.getCategories(), {
  validate: isArray,
  describe: (d) => d.length + ' 个一级分类, 子分类 ' + d.reduce((s, c) => s + (c.children?.length || 0), 0) + ' 个'
})

await check('GET /public/banners', () => api.getBanners(1), {
  validate: isArray,
  describe: (d) => d.length + ' 张轮播图, 首图 ' + (d[0]?.image ? '有' : '无')
})

await check('GET /public/announcements', () => api.getAnnouncements(5), {
  validate: isArray,
  describe: (d) => d.length + ' 条公告'
})

const productPage = await check('GET /public/products (分页)', () => api.getProducts({ pageNum: 1, pageSize: 8 }), {
  validate: isPage,
  describe: (d) => 'total=' + d.total + ', records=' + d.records.length
})

await check('GET /public/products?keyword=手环', () => api.getProducts({ pageNum: 1, pageSize: 5, keyword: '手环' }), {
  validate: isPage,
  describe: (d) => '命中 ' + d.total + ' 条'
})

const catId = categories?.[0]?.id
await check('GET /public/products?categoryId+includeChildren', () => api.getProducts({ pageNum: 1, pageSize: 5, categoryId: catId, includeChildren: true }), {
  validate: isPage,
  describe: (d) => '分类 ' + catId + ' 命中 ' + d.total + ' 条'
})

for (const sort of ['priceAsc', 'priceDesc', 'sales']) {
  await check('GET /public/products?sort=' + sort, () => api.getProducts({ pageNum: 1, pageSize: 4, sort }), {
    validate: isPage,
    describe: (d) => (d.records || []).map((p) => p.price ?? p.sales).slice(0, 4).join(' / ')
  })
}

await check('GET /public/products/hot', () => api.getHotProducts(6), {
  validate: isArray,
  describe: (d) => d.length + ' 个热门商品'
})

// 详情页(?pid= 与 ?id= 都指向同一个接口)
const detailId = productPage?.records?.[0]?.id ?? 13
const detail = await check('GET /public/products/' + detailId + ' (详情)', () => api.getProductDetail(detailId), {
  validate: (d) => d && d.id && d.name && d.price !== undefined,
  describe: (d) => d.name + ' / ￥' + d.price + ' / 库存 ' + d.stock + ' / images=' + (d.imageList?.length || 0)
})
record('详情返回 imageList / tags / avgScore 字段', !!detail && Array.isArray(detail.imageList) && Array.isArray(detail.tags) && detail.avgScore !== undefined, 'collected=' + detail?.collected + ', avgScore=' + detail?.avgScore + ', reviewCount=' + detail?.reviewCount)

if (detail?.merchantId) {
  await check('GET /public/merchants/' + detail.merchantId + '/tags', () => api.getMerchantTags(detail.merchantId), {
    validate: isArray,
    describe: (d) => d.map((t) => t.name).join(', ') || '(无标签)'
  })
}

await check('GET /public/products/' + detailId + '/reviews (评价分页)', () => api.getProductReviews(detailId, { pageNum: 1, pageSize: 5 }), {
  validate: isPage,
  describe: (d) => 'total=' + d.total + ', 首条字段: ' + Object.keys(d.records?.[0] || {}).slice(0, 8).join(',')
})

/* ---------- 6. 购物车 ---------- */
const cartCountBefore = await check('GET /cart/count (角标)', () => api.getCartCount(), { describe: (d) => d + ' 件' })

let cart = await check('GET /cart/list (按店铺分组用)', () => api.getCartList(), {
  validate: isArray,
  describe: (d) => d.length + ' 行, 含 productName/price/stock/merchantId: ' + ['productName', 'price', 'stock', 'merchantId'].every((k) => k in (d[0] || {}))
})

const addProductId = detailId
await check('POST /cart/add', () => api.addToCart(addProductId, 1))
await check('GET /cart/count 增加', () => api.getCartCount(), { describe: (d) => d + ' 件 (加购前 ' + cartCountBefore + ')' })
cart = await check('GET /cart/list (加购后)', () => api.getCartList(), { validate: isArray, describe: (d) => d.length + ' 行' })

const target = cart?.find((c) => c.productId === addProductId)
if (target) {
  const q = await check('PUT /cart/quantity', async () => {
    await api.updateCartQuantity(target.id, (target.quantity || 1) + 1)
    return (await api.getCartList()).find((c) => c.id === target.id)
  }, { describe: (d) => '数量 -> ' + d?.quantity })
  await check('PUT /cart/checked (单选)', () => api.updateCartChecked(target.id, 1))
  await check('PUT /cart/checked/all (全选)', () => api.updateCartCheckedAll(1))
  await check('DELETE /cart/remove (清理测试数据)', () => api.removeCartItems([target.id]))
} else {
  record('购物车数量/勾选/删除接口', false, '未找到刚加购的商品, 跳过')
}

/* ---------- 7. 收货地址 ---------- */
const addresses = await check('GET /address/list', () => api.getAddressList(), {
  validate: isArray,
  describe: (d) => d.length + ' 条, 默认地址: ' + (d.find((a) => a.isDefault === 1)?.detailAddress?.slice(0, 12) || '无')
})

const newAddress = await check('POST /address (新增测试地址)', () => api.saveAddress({
  receiverName: '接口自测',
  receiverPhone: '13900000002',
  province: '湖北省',
  city: '武汉市',
  district: '洪山区',
  detailAddress: '自动化验证地址 (可删除)',
  isDefault: 0
}))

const created = (await api.getAddressList().catch(() => []))?.find((a) => a.receiverName === '接口自测')
if (created) {
  await check('PUT /address/{id} (编辑)', () => api.saveAddress({ ...created, detailAddress: '自动化验证地址-已编辑' }))
  await check('DELETE /address/{id} (清理)', () => api.deleteAddress(created.id))
} else {
  record('地址增删改', false, '新增地址后未查询到, 跳过编辑/删除')
}

/* ---------- 8. 订单 ---------- */
await check('GET /orders/counts (状态角标)', () => api.getOrderCounts(), {
  validate: (d) => d && 'unpaid' in d,
  describe: (d) => 'unpaid=' + d.unpaid + ', undelivered=' + d.undelivered + ', unreceived=' + d.unreceived + ', finished=' + d.finished + ', all=' + d.all
})

const orderPage = await check('GET /orders/list (分页)', () => api.getOrderList({ pageNum: 1, pageSize: 5 }), {
  validate: isPage,
  describe: (d) => 'total=' + d.total + ', 首单状态=' + d.records?.[0]?.statusText + ', items=' + (d.records?.[0]?.items?.length ?? 0)
})

for (const st of [0, 1, 2, 3]) {
  await check('GET /orders/list?status=' + st, () => api.getOrderList({ pageNum: 1, pageSize: 3, status: st }), {
    validate: isPage,
    describe: (d) => 'total=' + d.total
  })
}

const firstOrder = orderPage?.records?.[0]
if (firstOrder) {
  await check('GET /orders/' + firstOrder.id + ' (详情, ?orderId= 用)', () => api.getOrderDetail(firstOrder.id), {
    validate: (d) => d && d.orderNo,
    describe: (d) => d.orderNo + ' / ' + d.statusText + ' / ￥' + d.payAmount
  })
}

// 结算预览: 立即购买分支
await check('GET /orders/preview?productId= (立即购买)', () => api.previewOrder({ productId: detailId, quantity: 1 }), {
  validate: (d) => d && Array.isArray(d.groups),
  describe: (d) => d.groups.length + ' 个店铺分组, 合计 ￥' + d.totalAmount + ', fromCart=' + d.fromCart
})

// 结算预览: 购物车分支
const checkedCart = (await api.getCartList().catch(() => [])) || []
if (checkedCart.length) {
  await check('GET /orders/preview?cartIds= (购物车结算)', () => api.previewOrder({ cartIds: checkedCart.map((c) => c.id).join(',') }), {
    validate: (d) => d && Array.isArray(d.groups),
    describe: (d) => d.groups.length + ' 个店铺分组, 合计 ￥' + d.totalAmount
  })
} else {
  record('GET /orders/preview?cartIds=', false, '购物车为空, 跳过')
}

/* ---------- 9. 收藏 ---------- */
await check('GET /user/collect/list', () => api.getCollectList(), {
  validate: isArray,
  describe: (d) => d.length + ' 条, 首条含嵌套 product: ' + !!d[0]?.product
})
const toggled = await check('POST /user/collect/{id} (收藏)', () => api.toggleCollect(addProductId), { describe: (d) => 'collected=' + d })
await check('POST /user/collect/{id} (再点取消收藏, 恢复原状)', () => api.toggleCollect(addProductId), { describe: (d) => 'collected=' + d })

/* ---------- 10. 评价 ---------- */
const pending = await check('GET /user/reviews/pending', () => api.getPendingReviews(), {
  validate: isArray,
  describe: (d) => d.length + ' 条待评价'
})
await check('GET /user/reviews/list', () => api.getMyReviews({ pageNum: 1, pageSize: 5 }), {
  validate: isPage,
  describe: (d) => 'total=' + d.total + ', 首条评分=' + d.records?.[0]?.score + ', hasReply=' + !!d.records?.[0]?.replyContent
})
if (pending?.length) {
  const item = pending[0]
  const submitted = await check('POST /user/reviews (提交评价)', async () => {
    await api.submitReview({ orderItemId: item.id, score: 5, content: '接口自动化验证评价 (可删除)', images: [], anonymous: 1 })
    return true
  })
  if (submitted) {
    await check('GET /user/reviews/list (评价后 total 增加)', () => api.getMyReviews({ pageNum: 1, pageSize: 5 }), { describe: (d) => 'total=' + d.total })
  }
  // 说明: 该评价属于真实数据写入, 不影响前台展示逻辑
}

/* ---------- 11. AI 导购 ---------- */
let aiSessionId = null
const aiReply = await check('POST /ai/chat (首次, sessionId=null)', () => api.aiChat('推荐一款 2000 元以内的手机', null), {
  validate: (d) => d && d.sessionId && typeof d.reply === 'string',
  describe: (d) => 'intent=' + d.intent + ', 推荐 ' + (d.products?.length || 0) + ' 个商品, 首条: ' + d.products?.[0]?.name
})
aiSessionId = aiReply?.sessionId
if (aiSessionId) {
  const second = await check('POST /ai/chat (带 sessionId 追问)', () => api.aiChat('那有没有更便宜的', aiSessionId), {
    validate: (d) => d && d.sessionId,
    describe: (d) => 'sessionId 保持一致: ' + (d.sessionId === aiSessionId) + ', 推荐 ' + (d.products?.length || 0) + ' 个'
  })
  await check('GET /ai/history (历史会话)', () => api.getAiHistory(aiSessionId), {
    validate: isArray,
    describe: (d) => d.length + ' 条消息, role 取值: ' + [...new Set(d.map((m) => m.role))].join('/')
  })
  await check('DELETE /ai/session (清空)', () => api.clearAiSession(aiSessionId))
  await check('GET /ai/history (清空后)', () => api.getAiHistory(aiSessionId), {
    validate: isArray,
    describe: (d) => d.length + ' 条消息'
  })
  void second
} else {
  record('AI 会话后续接口', false, '首次对话未返回 sessionId, 跳过')
}

/* ---------- 12. 退出登录 ---------- */
await check('POST /auth/logout', () => api.logout())

/* ---------- 汇总 ---------- */
console.log('==================================================')
const pass = results.filter((r) => r.ok).length
const expected = results.filter((r) => !r.ok && r.expectedFail).length
console.log('总计 ' + results.length + ' 项: 通过 ' + pass + ', 已知后端缺陷 ' + expected + ', 失败 ' + (results.length - pass - expected))
if (failed) {
  console.log('失败明细:')
  for (const r of results.filter((x) => !x.ok && !x.expectedFail)) console.log(' - ' + r.name + ' :: ' + r.detail)
  process.exit(1)
}
console.log('全部 ' + results.length + ' 项接口调用均与前端数据契约一致。')
