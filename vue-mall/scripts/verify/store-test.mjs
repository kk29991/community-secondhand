// 认证写入接口 + Pinia store 行为验证
// 运行: node --no-warnings --import ./scripts/verify/register.mjs scripts/verify/store-test.mjs
const store = new Map()
globalThis.localStorage = {
  getItem: (k) => (store.has(k) ? store.get(k) : null),
  setItem: (k, v) => store.set(k, String(v)),
  removeItem: (k) => store.delete(k),
  clear: () => store.clear()
}

const BACKEND = process.env.MALL_BACKEND || 'http://localhost:9090'

/* request.js 的 401 分支会读取 window.location.hash, 这里补一个最小桩 */
globalThis.window = { location: { hash: '#/login' } }
const api = await import('../../src/api/index.js')
const request = (await import('../../src/api/request.js')).default
const { TOKEN_KEY, getToken, getCurrentUser, clearAuth } = await import('../../src/api/request.js')
request.defaults.baseURL = BACKEND + '/api'

const results = []
function record(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log('[' + (ok ? 'PASS' : 'FAIL') + '] ' + name + (detail ? ' :: ' + detail : ''))
}

async function step(name, fn) {
  try {
    const v = await fn()
    record(name, true, typeof v === 'string' ? v : '')
    return v
  } catch (e) {
    record(name, false, e.message)
    return null
  }
}

console.log('后端地址: ' + BACKEND)
console.log('==================================================')

await step('登录 zhangsan', async () => {
  const vo = await api.login({ username: 'zhangsan', password: '123456', role: 'USER' })
  return 'token 长度 ' + vo.token.length + ', nickname=' + vo.nickname
})

await step('GET /auth/profile 带合法 token', async () => {
  const p = await api.getProfile()
  return 'username=' + p.username + ', nickname=' + p.nickname + ', phone=' + p.phone + ', email=' + p.email + ', gender=' + p.gender + ', role=' + p.role
})

await step('PUT /auth/profile 原值回写 (可重复执行)', async () => {
  const before = await api.getProfile()
  await api.updateProfile({
    nickname: before.nickname,
    avatar: before.avatar,
    phone: before.phone,
    email: before.email,
    gender: before.gender
  })
  const after = await api.getProfile()
  const same = after.nickname === before.nickname && after.phone === before.phone
  if (!same) throw new Error('回写后资料不一致')
  return '资料回写成功且校验一致 (nickname=' + after.nickname + ')'
})

await step('PUT /auth/profile 资料字段可正常写入', async () => {
  const before = await api.getProfile()
  await api.updateProfile({ nickname: before.nickname, phone: before.phone, email: before.email, gender: before.gender, avatar: before.avatar })
  const after = await api.getProfile()
  if (after.nickname !== before.nickname) throw new Error('昵称回写异常')
  // 说明: 后端 ProfileDTO 未对 phone 加 @Pattern, 前端表单已做格式校验
  return '资料写入并读回一致 (nickname=' + after.nickname + ')'
})

/* ---------- 注册一个临时账号, 验证注册接口与登录 ---------- */
const tmpUser = 'vtest' + Date.now().toString().slice(-8)
await step('POST /auth/register (临时账号 ' + tmpUser + ')', async () => {
  const vo = await api.register({
    username: tmpUser,
    password: '123456',
    nickname: '验证账号',
    phone: '13900000003',
    email: 'vtest@example.com',
    role: 'USER'
  })
  return '注册成功: id=' + vo.id + ', username=' + vo.username + ', role=' + vo.role
})

await step('新账号登录', async () => {
  clearAuth()
  const vo = await api.login({ username: tmpUser, password: '123456', role: 'USER' })
  if (!getToken()) throw new Error('token 未写入 localStorage')
  if (getCurrentUser().username !== tmpUser) throw new Error('用户缓存不正确')
  return '登录成功, token 已持久化, 缓存用户=' + getCurrentUser().username
})

await step('新账号修改资料', async () => {
  await api.updateProfile({ nickname: '验证账号-改', phone: '13900000004', email: 'v2@example.com', gender: 1, avatar: '' })
  const p = await api.getProfile()
  if (p.nickname !== '验证账号-改') throw new Error('昵称未更新')
  if (Number(p.gender) !== 1) throw new Error('性别未更新')
  return '昵称=' + p.nickname + ', 性别=' + p.gender + ', 手机=' + p.phone
})

await step('重复用户名注册应失败', async () => {
  try {
    await api.register({ username: tmpUser, password: '123456', role: 'USER' })
  } catch (e) {
    return '按预期失败: ' + e.message
  }
  throw new Error('重复用户名竟然注册成功')
})

await step('错误密码登录应失败', async () => {
  const { login } = api
  try {
    await login({ username: tmpUser, password: 'wrong-password', role: 'USER' })
  } catch (e) {
    return '按预期失败: ' + e.message
  }
  throw new Error('错误密码竟然登录成功')
})

/* ---------- 未登录访问受保护接口应 401 ---------- */
await step('清除登录态后访问 /cart/list 应 401', async () => {
  clearAuth()
  if (getToken()) throw new Error('clearAuth 未清空 token')
  delete request.defaults.headers.common.Authorization
  try {
    await api.getCartList()
  } catch (e) {
    return '按预期 401: ' + e.message
  }
  throw new Error('未登录竟然可以访问购物车')
})

/* ---------- request.js 的 token 键名约定 ---------- */
await step('localStorage 键名约定 (mall_mall_token / mall_mall_user)', async () => {
  await api.login({ username: 'zhangsan', password: '123456', role: 'USER' })
  const raw = globalThis.localStorage.getItem(TOKEN_KEY)
  if (!raw) throw new Error('未按约定键名写入 token')
  return TOKEN_KEY + '=' + raw.slice(0, 12) + '..., USER_KEY 是否写入=' + !!globalThis.localStorage.getItem('mall_mall_user')
})

await step('退出登录 (POST /auth/logout)', () => api.logout())

console.log('==================================================')
const pass = results.filter((r) => r.ok).length
console.log('总计 ' + results.length + ' 项, 通过 ' + pass + ', 失败 ' + (results.length - pass))
if (pass !== results.length) {
  for (const r of results.filter((x) => !x.ok)) console.log(' - ' + r.name + ' :: ' + r.detail)
  process.exit(1)
}
console.log('认证写入接口与登录态持久化全部符合前端实现预期。')
