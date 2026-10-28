// 路由表与需求文档一致性校验 (回归防护)
// 运行: node --no-warnings scripts/verify/routes-check.mjs
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const ROOT = path.resolve(__dirname, '..', '..')
const routerSrc = fs.readFileSync(path.join(ROOT, 'src', 'router', 'index.js'), 'utf8')

const required = [
  { path: '/', view: 'views/Home.vue' },
  { path: '/search', view: 'views/Search.vue' },
  { path: '/goodsDetail', view: 'views/GoodsDetail.vue' },
  { path: '/cart', view: 'views/Cart.vue' },
  { path: '/confirmOrder', view: 'views/ConfirmOrder.vue' },
  { path: '/payResult', view: 'views/PayResult.vue' },
  { path: '/user', view: 'views/user/UserLayout.vue' },
  { path: '/user/profile', view: 'views/user/Profile.vue' },
  { path: '/user/orders', view: 'views/user/Orders.vue' },
  { path: '/user/collect', view: 'views/user/Collect.vue' },
  { path: '/user/reviews', view: 'views/user/Reviews.vue' },
  { path: '/user/address', view: 'views/user/Address.vue' },
  { path: '/ai', view: 'views/AiShopping.vue' },
  { path: '/login', view: 'views/Login.vue' },
  { path: '/register', view: 'views/Register.vue' }
]

const problems = []

// 1. 必须使用 hash 模式
if (!/createWebHashHistory/.test(routerSrc)) problems.push('路由未使用 createWebHashHistory (hash 模式)')
if (/createWebHistory\s*\(/.test(routerSrc)) problems.push('路由误用了 createWebHistory')

// 2. 404 兜底
if (!/pathMatch\(\.\*\)\*/.test(routerSrc)) problems.push('缺少 404 兜底路由 (/:pathMatch(.*)*)')

// 3. 每条必需路由都存在且指向正确组件
for (const route of required) {
  const viewName = route.view
  const hasView = routerSrc.includes(`@/${viewName}`)
  if (!hasView) {
    problems.push(`路由 ${route.path} 未指向 ${viewName}`)
    continue
  }
  // 校验该 import 紧跟在对应 path 附近 (同一行或后续 2 行内)
  const idx = routerSrc.indexOf(`@/${viewName}`)
  const around = routerSrc.slice(Math.max(0, idx - 220), idx)
  const pathLiteral = route.path.startsWith('/user/') ? route.path.split('/').pop() : route.path
  if (!around.includes(`'${pathLiteral}'`) && !around.includes(`"${pathLiteral}"`)) {
    problems.push(`路由 ${route.path} 与组件 ${viewName} 未直接对应, 请检查顺序`)
  }
}

// 4. 需要登录的页面必须挂 beforeEnter: requireAuth
const protectedPaths = ['/cart', '/confirmOrder', '/payResult', '/ai']
for (const p of protectedPaths) {
  const re = new RegExp(`path:\\s*'${p.replace('/', '\\/')}'[\\s\\S]{0,320}?beforeEnter:\\s*requireAuth`)
  if (!re.test(routerSrc)) problems.push(`${p} 未配置 beforeEnter: requireAuth`)
}
if (!/path:\s*'\/user'[\s\S]{0,200}?beforeEnter:\s*requireAuth/.test(routerSrc)) {
  problems.push('/user 未配置 beforeEnter: requireAuth')
}

// 5. 登录/注册页使用独立布局 (bare)
for (const p of ['/login', '/register']) {
  const re = new RegExp(`path:\\s*'${p}'[^\\n]*bare:\\s*true`)
  if (!re.test(routerSrc)) problems.push(`${p} 未标记 meta.bare (登录/注册独立布局)`)
}

console.log('校验路由条目: ' + required.length + ' 条')
if (problems.length) {
  console.log('')
  for (const p of problems) console.log(' - ' + p)
  process.exit(1)
}
console.log('路由表与需求一致: hash 模式, 404 兜底, 登录鉴权, 独立登录布局均已配置。')
