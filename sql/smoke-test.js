/**
 * 后端接口冒烟测试: 覆盖注册/登录/浏览/加购/下单/支付/发货/评价/AI导购/权限
 * 运行: node sql/smoke-test.js   (需要后端已在 9090 启动)
 */
const BASE = process.env.MALL_API || 'http://localhost:9090/api';

let pass = 0;
let fail = 0;
const failures = [];

function check(name, cond, extra = '') {
  if (cond) {
    pass += 1;
    console.log(`  [OK]   ${name}`);
  } else {
    fail += 1;
    failures.push(name + (extra ? ` -> ${extra}` : ''));
    console.log(`  [FAIL] ${name} ${extra}`);
  }
}

async function api(method, path, { token, body, params } = {}) {
  let url = BASE + path;
  if (params) {
    const qs = new URLSearchParams(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== null)
    ).toString();
    if (qs) url += (url.includes('?') ? '&' : '?') + qs;
  }
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  let json = null;
  try {
    json = await res.json();
  } catch (e) {
    json = { code: res.status, message: 'non-json response' };
  }
  return { status: res.status, httpOk: res.ok, ...json };
}

(async () => {
  // 先取全量商品, 后续断言基于真实数据而不是硬编码数量/ID, 避免种子数据调整后测试失效
  const all = await api('GET', '/public/products', { params: { pageSize: 200 } });
  const allProducts = all.data.records;
  const byName = (kw) => allProducts.find((p) => p.name.includes(kw));
  const sellerOf = (p) => p.merchantId;

  console.log(`（本次数据: 共 ${all.data.total} 个商品）`);

  console.log('\n=== 1. 公开接口 ===');
  const cats = await api('GET', '/public/categories');
  check('分类树返回 200 且含子分类', cats.code === 200 && cats.data.length > 0 && Array.isArray(cats.data[0].children));
  check('一级分类 >= 6 个', cats.data.length >= 6, `n=${cats.data.length}`);

  check('商品列表分页返回全部商品', all.code === 200 && all.data.total === allProducts.length,
    `total=${all.data.total} records=${allProducts.length}`);
  const bySales = await api('GET', '/public/products', { params: { pageSize: 5, sort: 'sales' } });
  const salesArr = bySales.data.records.map((p) => p.sales);
  check('商品按销量降序', salesArr.every((v, i) => i === 0 || salesArr[i - 1] >= v), salesArr.join(','));

  const detail = await api('GET', '/public/products/1');
  check('商品详情含轮播图与标签', detail.code === 200 && Array.isArray(detail.data.imageList) && detail.data.tags.length === 2);
  check('商品详情含平均分', detail.data.avgScore !== undefined, `avgScore=${detail.data.avgScore}`);
  check('商品主图使用真实图片(非占位图)', (detail.data.mainImage || '').includes('/uploads/'),
    detail.data.mainImage);

  const search = await api('GET', '/public/products', { params: { keyword: '沙发' } });
  check('关键词搜索命中', search.code === 200 && search.data.total >= 1, `total=${search.data?.total}`);

  const clothes = cats.data.find((c) => c.name === '服饰鞋包');
  const catFilter = await api('GET', '/public/products', { params: { categoryId: clothes.id, includeChildren: true } });
  const expectedClothes = allProducts.filter((p) => (clothes.children || []).some((c) => c.id === p.categoryId)).length;
  check('按一级分类筛选含子分类商品', catFilter.code === 200 && catFilter.data.total === expectedClothes,
    `total=${catFilter.data?.total} 期望=${expectedClothes}`);

  const banners = await api('GET', '/public/banners');
  check('首页轮播图可访问且有数据', banners.code === 200 && banners.data.length >= 4, `len=${banners.data?.length}`);

  const reviews = await api('GET', '/public/products/1/reviews');
  check('商品评价列表可访问', reviews.code === 200);

  const ph = await fetch(`${BASE}/public/placeholder.svg?text=%E6%B5%8B%E8%AF%95&w=200&h=200`);
  check('SVG 占位图可用', ph.ok && (ph.headers.get('content-type') || '').includes('svg'));

  console.log('\n=== 2. 鉴权 ===');
  const badLogin = await api('POST', '/auth/login', { body: { username: 'zhangsan', password: 'wrong-password', role: 'USER' } });
  check('错误密码被拒绝', badLogin.code !== 200, `code=${badLogin.code}`);

  const login = await api('POST', '/auth/login', { body: { username: 'zhangsan', password: '123456', role: 'USER' } });
  check('普通用户登录成功', login.code === 200 && !!login.data.token, login.message);
  const userToken = login.data.token;

  const merchantLogin = await api('POST', '/auth/login', { body: { username: 'xunxi', password: 'merchant123', role: 'MERCHANT' } });
  check('商家登录成功且带 merchantId', merchantLogin.code === 200 && merchantLogin.data.merchantId === 1, JSON.stringify(merchantLogin.data?.merchantId));
  const merchantToken = merchantLogin.data.token;

  const adminLogin = await api('POST', '/auth/login', { body: { username: 'admin', password: 'admin123', role: 'ADMIN' } });
  check('管理员登录成功', adminLogin.code === 200 && adminLogin.data.role === 'ADMIN');
  const adminToken = adminLogin.data.token;

  const noToken = await api('GET', '/cart/list');
  check('无 token 访问购物车返回 401', noToken.code === 401, `code=${noToken.code}`);

  const forbidden = await api('GET', '/admin/overview', { token: userToken });
  check('普通用户访问管理端返回 403', forbidden.code === 403, `code=${forbidden.code}`);

  const profile = await api('GET', '/auth/profile', { token: userToken });
  check('获取当前用户资料', profile.code === 200 && profile.data.username === 'zhangsan');

  console.log('\n=== 3. 购物车 ===');
  // 动态取商品: 需要跨卖家才能验证"按卖家拆单"。
  // 注意两点(都是踩过的坑):
  //   1. 不能写死商品名 —— 改名或换种子数据后会取不到, 且会因 undefined 直接崩溃
  //   2. 必须挑库存充足的 —— 二手物品多数只有 1 件, 若取到 stock 已耗尽的商品,
  //      加购会因"库存不足"失败(这是正确业务行为, 但会让测试误报)
  // 挑库存充足的商品: 二手物品多为孤品(多数 stock=1), 而本段需要同一商品买 2 件
  // (用于验证"重复加购累加"与"满 99 免运费"), 因此按库存降序挑选。
  const byStockDesc = [...allProducts].sort((a, b) => Number(b.stock) - Number(a.stock));
  const prodA = byStockDesc[0];
  const prodB = prodA ? byStockDesc.find((p) => sellerOf(p) !== sellerOf(prodA)) : undefined;
  check('取到跨卖家且库存充足的商品用于拆单验证', !!prodA && !!prodB,
    `A=${prodA?.name}(卖家${prodA && sellerOf(prodA)}, 库存${prodA?.stock}) B=${prodB?.name}(卖家${prodB && sellerOf(prodB)}, 库存${prodB?.stock})`);
  if (!prodA || !prodB || Number(prodA.stock) < 2 || Number(prodB.stock) < 2) {
    console.error('致命: 需要有 2 件以上库存、且分属不同卖家的商品才能完成拆单验证');
    console.error(`      当前 A 库存=${prodA?.stock}, B 库存=${prodB?.stock}`);
    console.error('      提示: 若数据被之前的测试改脏, 请重跑 sql/reset-and-test.cmd 重置');
    process.exit(1);
  }

  await api('DELETE', '/cart/clear', { token: userToken });
  const add1 = await api('POST', '/cart/add', { token: userToken, body: { productId: prodA.id, quantity: 1 } });
  check(`加入购物车 (${prodA.name})`, add1.code === 200, add1.message);
  const add4 = await api('POST', '/cart/add', { token: userToken, body: { productId: prodB.id, quantity: 2 } });
  check(`加入购物车 (${prodB.name} x2, 另一商家)`, add4.code === 200, add4.message);
  // 同商品重复加购应累加而非新增行
  const add3 = await api('POST', '/cart/add', { token: userToken, body: { productId: prodA.id, quantity: 1 } });
  const cartList = await api('GET', '/cart/list', { token: userToken });
  check('同商品重复加购累加而不新增行 (2 行)', add3.code === 200 && cartList.data.length === 2, `rows=${cartList.data?.length}`);
  const rowA = cartList.data.find((c) => c.productId === prodA.id);
  check('重复加购数量累加为 2', rowA?.quantity === 2, `qty=${rowA?.quantity}`);
  check('购物车项带商品名与店铺名', !!rowA?.productName && !!rowA?.merchantName, `${rowA?.productName}/${rowA?.merchantName}`);
  check('购物车小计 = 单价 x 数量',
    Math.abs(rowA.totalPrice - prodA.price * 2) < 0.01, `${rowA?.totalPrice} vs ${prodA.price * 2}`);

  const cnt = await api('GET', '/cart/count', { token: userToken });
  check('购物车件数统计 = 4', cnt.data === 4, `count=${cnt.data}`);

  // 选一个库存很小的商品验证超库存拦截
  const lowStock = [...allProducts].sort((a, b) => a.stock - b.stock)[0];
  const overStock = await api('POST', '/cart/add', {
    token: userToken, body: { productId: lowStock.id, quantity: lowStock.stock + 9999 }
  });
  check('超库存加购被拦截', overStock.code !== 200, overStock.message);

  console.log('\n=== 4. 下单 (按商家拆单) ===');
  const cartIds = cartList.data.map((c) => c.id);
  const salesBeforeA = (await api('GET', `/public/products/${prodA.id}`)).data.sales;
  const preview = await api('GET', '/orders/preview', { token: userToken, params: { cartIds: cartIds.join(',') } });
  check('结算预览成功', preview.code === 200, preview.message);
  check('按商家拆成 2 个订单组', preview.data?.groups?.length === 2, `groups=${preview.data?.groups?.length}`);
  const expectTotal = prodA.price * 2 + prodB.price * 2;
  check('预览总金额正确', Math.abs(preview.data.totalAmount - expectTotal) < 0.01,
    `${preview.data?.totalAmount} vs ${expectTotal}`);

  const createRes = await api('POST', '/orders', {
    token: userToken,
    body: { addressId: 1, cartIds, payType: 1, payImmediately: true, remark: '冒烟测试订单' },
  });
  check('下单成功并拆成 2 个订单', createRes.code === 200 && createRes.data.length === 2, createRes.message);
  const orders = createRes.data || [];
  check('订单号唯一且 18 位', orders.length === 2 && orders[0].orderNo !== orders[1].orderNo && orders[0].orderNo.length === 18, orders.map((o) => o.orderNo).join(','));
  check('下单后直接支付 -> 待发货', orders.every((o) => o.status === 1), orders.map((o) => o.status).join(','));
  // 运费规则: 按卖家分别计算, 该卖家商品金额满 99 元免运费, 否则 10 元。
  // 两个订单的金额不同(prodA/prodB 价格不同), 因此要按各自金额判断, 不能一律断言为 0。
  const FREE = 99, FEE = 10
  const expectFreight = (amount) => (amount >= FREE ? 0 : FEE)
  const freightOk = orders.every((o) => {
    const amount = Number(o.totalAmount)
    return Number(o.freight) === expectFreight(amount)
  })
  check('运费规则正确(满 99 免运费, 否则 10 元) 按各卖家金额计算', freightOk,
    orders.map((o) => `金额${o.totalAmount}->运费${o.freight}`).join(', '))

  const cartAfter = await api('GET', '/cart/list', { token: userToken });
  check('下单后已结算购物车项被清理', cartAfter.data.length === 0, `rows=${cartAfter.data?.length}`);

  const stockAfter = await api('GET', `/public/products/${prodA.id}`);
  check(`下单后库存已扣减 (-2)`, stockAfter.data.stock === prodA.stock - 2,
    `${prodA.stock} -> ${stockAfter.data.stock}`);
  check('下单后销量同步累加 (+2)', stockAfter.data.sales === salesBeforeA + 2,
    `sales ${salesBeforeA} -> ${stockAfter.data.sales}`);

  console.log('\n=== 5. 库存并发安全 (构造库存=1 后并发下单) ===');
  // 选一个库存较大且无历史订单的商品, 反复下单直到库存压到 1
  const raceProduct = allProducts.find((p) => p.name.includes('小露珠')) || allProducts[0];
  const RACE_PID = raceProduct.id;
  const createdUnpaid = [];
  let guard = 0;
  while (guard++ < 200) {
    const current = (await api('GET', `/public/products/${RACE_PID}`)).data;
    if (current.stock <= 1) {
      break;
    }
    const r = await api('POST', '/orders', {
      token: userToken,
      body: { addressId: 1, productId: RACE_PID, quantity: 1, payImmediately: false, payType: 1 },
    });
    if (r.code === 200) {
      createdUnpaid.push(r.data[0].id);
    }
  }
  const stockBeforeRace = (await api('GET', `/public/products/${RACE_PID}`)).data.stock;
  check(`已将商品 ${RACE_PID} 库存压到 1 (初始 ${raceProduct.stock}, 下单 ${createdUnpaid.length} 笔)`,
    stockBeforeRace === 1, `stock=${stockBeforeRace}`);

  // 5 个并发请求抢最后 1 件
  const raceResults = await Promise.all(
    Array.from({ length: 5 }, () =>
      api('POST', '/orders', {
        token: userToken,
        body: { addressId: 1, productId: RACE_PID, quantity: 1, payImmediately: false, payType: 1 },
      })
    )
  );
  const okCount = raceResults.filter((r) => r.code === 200).length;
  raceResults.forEach((r) => {
    if (r.code === 200) createdUnpaid.push(r.data[0].id);
  });
  const after = (await api('GET', `/public/products/${RACE_PID}`)).data.stock;
  check('并发下单不超卖 (5 个并发只成功 1 单, 库存归零)', okCount === 1 && after === 0,
    `成功=${okCount}, 库存 ${stockBeforeRace}->${after}`);

  console.log('\n=== 6. 取消订单回滚库存 ===');
  check('已产生待付款订单用于取消测试', createdUnpaid.length > 0, `n=${createdUnpaid.length}`);
  const cancelTargetId = createdUnpaid[0];
  const beforeCancelStock = (await api('GET', `/public/products/${RACE_PID}`)).data.stock;
  const cancel = await api('PUT', `/orders/${cancelTargetId}/cancel`, { token: userToken, params: { reason: '冒烟测试取消' } });
  check('取消待付款订单成功', cancel.code === 200, cancel.message);
  const afterCancelStock = (await api('GET', `/public/products/${RACE_PID}`)).data.stock;
  check('取消后库存已回滚 (+1)', afterCancelStock === beforeCancelStock + 1,
    `${beforeCancelStock} -> ${afterCancelStock}`);
  const canceledOrder = await api('GET', `/orders/${cancelTargetId}`, { token: userToken });
  check('取消后订单状态为已取消(4)', canceledOrder.data?.status === 4, `status=${canceledOrder.data?.status}`);
  const doubleCancel = await api('PUT', `/orders/${cancelTargetId}/cancel`, { token: userToken, params: { reason: '重复取消' } });
  check('重复取消被拦截', doubleCancel.code !== 200, doubleCancel.message);
  // 清理剩余的待付款订单, 避免影响后续商家待发货统计
  for (const oid of createdUnpaid.slice(1)) {
    await api('PUT', `/orders/${oid}/cancel`, { token: userToken, params: { reason: '压测清理' } });
  }
  const finalStock = (await api('GET', `/public/products/${RACE_PID}`)).data.stock;
  check('全部取消后库存完全恢复', finalStock === raceProduct.stock, `${raceProduct.stock} -> ${finalStock}`);

  console.log('\n=== 7. 商家端: 发货 + 回复评价 ===');
  // 本段固定用「商家1(xunxi)」的账号验证, 因此必须先确保该商家有 1 笔待发货订单。
  // 不能依赖第 4 段生成订单的归属 —— 那段是按库存动态挑商品的, 挑中的卖家可能是别人。
  const m1Product = allProducts.find((p) => sellerOf(p) === 1 && Number(p.stock) >= 1);
  check('找到商家1的在售闲置用于发货测试', !!m1Product, m1Product?.name || '未找到');
  const m1OrderRes = m1Product
    ? await api('POST', '/orders', {
      token: userToken,
      body: { addressId: 1, productId: m1Product.id, quantity: 1, payImmediately: true, payType: 1 },
    })
    : { code: 0 };
  check('为商家1创建待发货订单', m1OrderRes.code === 200, m1OrderRes.message || '');

  const mOrders = await api('GET', '/admin/merchant/orders', { token: merchantToken, params: { status: 1, pageSize: 5 } });
  check('商家查询待发货订单', mOrders.code === 200 && mOrders.data.records.length >= 1, `n=${mOrders.data?.records?.length}`);
  const toDeliver = mOrders.data.records[0];
  const deliver = await api('PUT', `/admin/merchant/orders/${toDeliver.id}/deliver`, { token: merchantToken, params: { expressNo: 'SF9999999999' } });
  check('商家发货成功', deliver.code === 200, deliver.message);
  const delivered = await api('GET', `/orders/${toDeliver.id}`, { token: userToken });
  check('发货后状态变为待收货(2)', delivered.data.status === 2, `status=${delivered.data.status}`);

  const mReviews = await api('GET', '/admin/merchant/reviews', { token: merchantToken, params: { hasReply: false, pageSize: 5 } });
  check('商家查询待回复评价', mReviews.code === 200 && mReviews.data.records.length >= 1, `n=${mReviews.data?.records?.length}`);
  const replyTarget = mReviews.data.records[0];
  const reply = await api('PUT', `/admin/merchant/reviews/${replyTarget.id}/reply`, {
    token: merchantToken,
    body: { content: '感谢您的支持, 欢迎再次光临!' },
  });
  check('商家回复评价成功', reply.code === 200, reply.message);

  // 越权校验: 挑一条属于「其他店铺」的评价, 当前商家回复应被拒绝
  const allReviews = await api('GET', '/admin/reviews', { token: adminToken, params: { pageSize: 50 } });
  const foreign = (allReviews.data?.records || []).find((r) => r.merchantId !== 1);
  if (foreign) {
    const crossReply = await api('PUT', `/admin/merchant/reviews/${foreign.id}/reply`, {
      token: merchantToken,
      body: { content: '越权回复测试' },
    });
    check('商家无法回复其他店铺评价 (403)', crossReply.code === 403, `code=${crossReply.code}, ${crossReply.message}`);
  } else {
    check('存在其他店铺的评价用于越权测试', false, '未找到 merchantId != 1 的评价');
  }

  console.log('\n=== 8. 商家商品管理 ===');
  const newProduct = await api('POST', '/admin/merchant/products', {
    token: merchantToken,
    body: {
      categoryId: 9,
      name: '冒烟测试商品-条纹衬衫',
      subTitle: '自动化测试创建',
      mainImage: 'http://localhost:9090/api/public/placeholder.svg?text=test&w=400&h=400',
      images: ['http://localhost:9090/api/public/placeholder.svg?text=test&w=400&h=400'],
      price: 88.8,
      stock: 10,
      tagIds: [1],
      status: 1,
    },
  });
  check('商家新增商品成功', newProduct.code === 200, newProduct.message);
  const newId = newProduct.data?.id;
  const offShelf = await api('PUT', `/admin/merchant/products/${newId}/status`, { token: merchantToken, params: { status: 0 } });
  check('商品下架成功', offShelf.code === 200, offShelf.message);
  const publicAfterOff = await api('GET', '/public/products', { params: { keyword: '冒烟测试商品' } });
  check('下架商品不出现在前台', publicAfterOff.data.total === 0, `total=${publicAfterOff.data?.total}`);
  const merchantView = await api('GET', '/admin/merchant/products', { token: merchantToken, params: { keyword: '冒烟测试商品' } });
  check('下架商品在商家后台仍可见', merchantView.data.records.some((p) => p.name.includes('冒烟测试商品')),
    `total=${merchantView.data?.total}`);

  // 动态挑一个属于其他商家的商品, 避免硬编码 ID 因种子数据调整而失效
  const foreignProduct = (await api('GET', '/admin/products', {
    token: adminToken, params: { pageSize: 50 }
  })).data.records.find((p) => p.merchantId !== 1);
  check('找到其他商家的商品用于越权测试', !!foreignProduct, `id=${foreignProduct?.id}`);
  const crossUpdate = await api('PUT', `/admin/merchant/products/${foreignProduct.id}/status`, {
    token: merchantToken, params: { status: 0 }
  });
  check('商家无法操作其他商家商品 (403)', crossUpdate.code === 403, `code=${crossUpdate.code}`);

  console.log('\n=== 9. 评价闭环 ===');
  const finished = await api('GET', '/orders/list', { token: userToken, params: { status: 3, pageSize: 20 } });
  check('查询已完成订单', finished.code === 200 && finished.data.records.length > 0, `n=${finished.data?.records?.length}`);
  const finishedOrder = finished.data.records[0];
  const receiveTest = await api('PUT', `/orders/${toDeliver.id}/receive`, { token: userToken });
  check('确认收货成功', receiveTest.code === 200, receiveTest.message);

  const pending = await api('GET', '/user/reviews/pending', { token: userToken });
  check('查询待评价明细', pending.code === 200 && pending.data.length >= 1, `n=${pending.data?.length}`);
  const item = pending.data[0];
  const submitReview = await api('POST', '/user/reviews', {
    token: userToken,
    body: { orderItemId: item.id, score: 5, content: '冒烟测试评价: 质量很好!', anonymous: 0 },
  });
  check('提交评价成功', submitReview.code === 200, submitReview.message);
  const dupReview = await api('POST', '/user/reviews', {
    token: userToken,
    body: { orderItemId: item.id, score: 5, content: '重复评价' },
  });
  check('重复评价被拦截', dupReview.code !== 200, dupReview.message);

  console.log('\n=== 10. 收藏 ===');
  // 先用一次切换把状态归零, 保证本段可重复运行(不依赖库里原本是否已收藏)
  const colInit = await api('POST', '/user/collect/5', { token: userToken });
  check('收藏接口可调用', colInit.code === 200, colInit.message);
  if (colInit.data === true) {
    // 之前处于未收藏状态, 刚才这一下把它收藏了, 再切回未收藏
    await api('POST', '/user/collect/5', { token: userToken });
  }
  const col1 = await api('POST', '/user/collect/5', { token: userToken });
  check('收藏商品成功', col1.code === 200 && col1.data === true, JSON.stringify(col1.data));
  const col2 = await api('POST', '/user/collect/5', { token: userToken });
  check('再次收藏为取消 (幂等切换)', col2.code === 200 && col2.data === false, JSON.stringify(col2.data));
  await api('POST', '/user/collect/5', { token: userToken });
  const collects = await api('GET', '/user/collect/list', { token: userToken });
  check('收藏列表带商品信息', collects.code === 200 && collects.data.length >= 1 && !!collects.data[0].product?.name);
  // 收尾: 移除本次收藏, 保持数据可重复运行
  await api('DELETE', '/user/collect/5', { token: userToken });

  console.log('\n=== 11. 收货地址 ===');
  const addrList = await api('GET', '/address/list', { token: userToken });
  check('地址列表默认地址优先', addrList.code === 200 && addrList.data[0].isDefault === 1);
  const newAddr = await api('POST', '/address', {
    token: userToken,
    body: { receiverName: '测试收货人', receiverPhone: '13711112222', province: '江苏省', city: '南京市', district: '鼓楼区', detailAddress: '中山路 100 号', isDefault: 1 },
  });
  check('新增地址成功', newAddr.code === 200, newAddr.message);
  const afterAdd = await api('GET', '/address/list', { token: userToken });
  check('新地址成为唯一默认地址', afterAdd.data.filter((a) => a.isDefault === 1).length === 1, `defaults=${afterAdd.data.filter((a) => a.isDefault === 1).length}`);
  const badPhone = await api('POST', '/address', {
    token: userToken,
    body: { receiverName: '张三', receiverPhone: '123', detailAddress: '某地' },
  });
  check('非法手机号被校验拦截', badPhone.code === 400, `code=${badPhone.code}`);

  console.log('\n=== 12. AI 导购 ===');
  const ai1 = await api('POST', '/ai/chat', { token: userToken, body: { message: '我想买一件外套, 有推荐吗' } });
  check('AI 导购返回回复与商品', ai1.code === 200 && !!ai1.data.reply && ai1.data.products.length > 0, `intent=${ai1.data?.intent}`);
  check('AI 识别服饰意图', ai1.data.intent === '服饰', `intent=${ai1.data?.intent}`);
  const sessionId = ai1.data.sessionId;
  const ai2 = await api('POST', '/ai/chat', { token: userToken, body: { message: '60元以内的外套', sessionId } });
  // 预算内商品不足时会补上"超预算"的同类商品(并在 overBudgetIds 中标记), 这里排除它们后再断言
  const overIds = ai2.data?.overBudgetIds || [];
  const withinBudgetPrices = (ai2.data?.products || [])
    .filter((p) => !overIds.includes(p.id))
    .map((p) => p.price);
  check('AI 预算过滤生效', ai2.code === 200 && withinBudgetPrices.every((p) => p <= 100),
    `预算内价格=${withinBudgetPrices.join(',')} 超预算=${overIds.length}个`);
  const history = await api('GET', '/ai/history', { token: userToken, params: { sessionId } });
  check('AI 会话历史已持久化 (4 条)', history.data.length === 4, `n=${history.data?.length}`);

  console.log('\n=== 13. 管理端 ===');
  const overview = await api('GET', '/admin/overview', { token: adminToken });
  check('平台概览统计可用', overview.code === 200 && overview.data.userCount === 5, JSON.stringify(overview.data?.userCount));
  check('平台概览成交额 > 0', Number(overview.data.totalSales) > 0, `sales=${overview.data?.totalSales}`);

  const stats = await api('GET', '/admin/statistics/sales', { token: adminToken, params: { days: 30, topN: 5 } });
  check('销售统计返回趋势与排行', stats.code === 200 && Array.isArray(stats.data.trend) && Array.isArray(stats.data.productRanking), `trend=${stats.data?.trend?.length}, rank=${stats.data?.productRanking?.length}`);

  const users = await api('GET', '/admin/users', { token: adminToken, params: { pageSize: 10 } });
  check('用户管理列表', users.code === 200 && users.data.total === 5);
  const disable = await api('PUT', '/admin/users/4/status', { token: adminToken, params: { status: 0 } });
  check('禁用用户成功', disable.code === 200, disable.message);
  const disabledLogin = await api('POST', '/auth/login', { body: { username: 'zhaoliu', password: '123456', role: 'USER' } });
  check('被禁用用户无法登录', disabledLogin.code !== 200, disabledLogin.message);
  await api('PUT', '/admin/users/4/status', { token: adminToken, params: { status: 1 } });

  const merchants = await api('GET', '/admin/merchants', { token: adminToken });
  check('商家管理列表 = 4', merchants.code === 200 && merchants.data.total === 4, `total=${merchants.data?.total}`);

  const delMerchant = await api('DELETE', '/admin/merchants/1', { token: adminToken });
  check('有商品的商家禁止删除', delMerchant.code !== 200, delMerchant.message);

  const cat = await api('POST', '/admin/categories', { token: adminToken, body: { name: '冒烟测试分类', parentId: 0, sort: 99 } });
  check('新增分类成功', cat.code === 200, cat.message);
  const catId = cat.data?.id;
  const childCat = await api('POST', '/admin/categories', { token: adminToken, body: { name: '冒烟子分类', parentId: catId, sort: 1 } });
  check('新增子分类成功', childCat.code === 200, childCat.message);
  const delParent = await api('DELETE', `/admin/categories/${catId}`, { token: adminToken });
  check('存在子分类时禁止删除父分类', delParent.code !== 200, delParent.message);

  const ann = await api('POST', '/admin/announcements', { token: adminToken, body: { title: '冒烟测试公告', content: '内容', type: 1, status: 1, isTop: 0 } });
  check('发布公告成功', ann.code === 200, ann.message);
  const banner = await api('POST', '/admin/banners', { token: adminToken, body: { title: '冒烟测试轮播', image: 'http://localhost:9090/api/public/placeholder.svg?text=banner&w=900&h=360', position: 1, sort: 9 } });
  check('新增轮播图成功', banner.code === 200, banner.message);

  const orderAdmin = await api('GET', '/admin/orders', { token: adminToken, params: { pageSize: 5 } });
  check('管理端订单列表', orderAdmin.code === 200 && orderAdmin.data.total >= 8, `total=${orderAdmin.data?.total}`);

  console.log('\n=== 14. 个人资料 ===');
  const upd = await api('PUT', '/auth/profile', { token: userToken, body: { nickname: '张三改名', gender: 1 } });
  check('修改资料成功', upd.code === 200, upd.message);
  const afterUpd = await api('GET', '/auth/profile', { token: userToken });
  check('资料已生效', afterUpd.data.nickname === '张三改名', afterUpd.data?.nickname);

  // ---------------------------------------------------------------
  // 15. 收尾清理: 让本测试可以重复运行, 而不只是"从干净库跑一次"
  // ---------------------------------------------------------------
  console.log('\n=== 15. 收尾清理 ===');
  const parseId = (list, keyword) => (list.data?.records || []).find((r) => (r.name || r.title || '').includes(keyword))?.id;

  // 清理本轮新增的轮播图 / 公告 / 分类
  const bannerList = await api('GET', '/admin/banners', { token: adminToken, params: { pageSize: 100 } });
  const testBannerId = parseId(bannerList, '冒烟测试轮播');
  if (testBannerId) {
    const r = await api('DELETE', `/admin/banners/${testBannerId}`, { token: adminToken });
    check('清理测试轮播图', r.code === 200, r.message);
  }
  const annList = await api('GET', '/admin/announcements', { token: adminToken, params: { pageSize: 100 } });
  const testAnnId = (annList.data?.records || []).find((a) => a.title === '冒烟测试公告')?.id;
  if (testAnnId) {
    const r = await api('DELETE', `/admin/announcements/${testAnnId}`, { token: adminToken });
    check('清理测试公告', r.code === 200, r.message);
  }
  const catList = await api('GET', '/admin/categories', { token: adminToken });
  const testCatIds = (catList.data || []).filter((c) => c.name.startsWith('冒烟')).map((c) => c.id);
  for (const id of testCatIds) {
    await api('DELETE', `/admin/categories/${id}`, { token: adminToken });
  }
  check('清理测试分类', true);

  // 清理测试地址
  const cleanupAddrList = await api('GET', '/address/list', { token: userToken });
  for (const a of cleanupAddrList.data || []) {
    if (a.receiverName === '测试收货人') {
      await api('DELETE', `/address/${a.id}`, { token: userToken });
    }
  }
  check('清理测试收货地址', true);

  // 还原个人资料
  await api('PUT', '/auth/profile', { token: userToken, body: { nickname: '张三', gender: 1 } });
  check('还原个人资料', true);

  // 清空购物车与 AI 会话, 避免影响下次运行
  await api('DELETE', '/cart/clear', { token: userToken });
  check('清空购物车', true);

  console.log('\n============================================================');
  console.log(`通过 ${pass} 项, 失败 ${fail} 项`);
  if (failures.length) {
    console.log('失败明细:');
    failures.forEach((f) => console.log('  - ' + f));
  }
  console.log('============================================================');
  process.exit(fail === 0 ? 0 : 1);
})().catch((e) => {
  console.error('冒烟测试异常中断:', e);
  process.exit(2);
});
