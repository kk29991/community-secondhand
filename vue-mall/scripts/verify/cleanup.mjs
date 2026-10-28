// 清理验证过程中创建的临时账号 (需要管理员账号)
// 运行: node --no-warnings --import ./scripts/verify/register.mjs scripts/verify/cleanup.mjs
const store = new Map()
globalThis.localStorage = {
  getItem: (k) => (store.has(k) ? store.get(k) : null),
  setItem: (k, v) => store.set(k, String(v)),
  removeItem: (k) => store.delete(k),
  clear: () => store.clear()
}
globalThis.window = { location: { hash: '#/' } }

const BACKEND = process.env.MALL_BACKEND || 'http://localhost:9090'
const api = await import('../../src/api/index.js')
const request = (await import('../../src/api/request.js')).default
request.defaults.baseURL = BACKEND + '/api'

// 先以普通用户登录, 拿到可用的 token 结构; admin 账号密码见 sql 种子数据
const ADMIN = process.env.MALL_ADMIN || 'admin'
const ADMIN_PWD = process.env.MALL_ADMIN_PWD || '123456'

let token = ''
try {
  const vo = await api.login({ username: ADMIN, password: ADMIN_PWD, role: 'ADMIN' })
  token = vo.token
  console.log('管理员登录成功: ' + vo.username + ' (' + vo.role + ')')
} catch (e) {
  console.log('管理员登录失败, 跳过清理: ' + e.message)
  process.exit(0)
}

const res = await request.get('/admin/users', { params: { pageNum: 1, pageSize: 200 } })
const users = res?.records || []
const temp = users.filter((u) => /^vtest\d+$/i.test(u.username || ''))
console.log('发现临时测试账号: ' + temp.length + ' 个 -> ' + temp.map((u) => u.username).join(', '))
for (const u of temp) {
  try {
    await request.delete('/admin/users/' + u.id)
    console.log('已删除: ' + u.username)
  } catch (e) {
    console.log('删除失败 ' + u.username + ': ' + e.message)
  }
}
console.log('清理完成 (token 前缀: ' + token.slice(0, 8) + '...)')
