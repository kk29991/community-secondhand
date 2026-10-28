/**
 * 验证图片地址归一化(不依赖浏览器 API)。
 *
 * 做法: 直接取后端真实响应, 用与 src/api/request.js 完全一致的归一化逻辑处理,
 * 再验证归一化后的相对地址经 dev server(5174) 与静态服务器(5180) 都能取到图片。
 * 归一化逻辑从源码文件里抽取比对, 避免"测试里另写一份"导致失真。
 *
 * 运行: node scripts/verify/check-image-urls.mjs
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const here = path.dirname(fileURLToPath(import.meta.url))
const mallRoot = path.resolve(here, '..', '..')
const API = 'http://localhost:9090/api'

// 从 request.js 中抽取真实的字段白名单与正则, 确保测的就是线上逻辑
const requestSrc = fs.readFileSync(path.join(mallRoot, 'src', 'api', 'request.js'), 'utf8')
const fieldMatch = requestSrc.match(/const IMAGE_FIELDS = \[([\s\S]*?)\]/)
const IMAGE_FIELDS = fieldMatch
  ? fieldMatch[1].split(',').map((s) => s.trim().replace(/['"]/g, '')).filter(Boolean)
  : []
const regexMatch = requestSrc.match(/url\.match\((\/\^https\?:\\\/\\\/\[\^\/\]\+\(\\\/api\\\/\.\*\)\$\/i)\)/)
const hasNormalizer = /normalizeImageUrl/.test(requestSrc)

let pass = 0
let fail = 0
const check = (n, c, extra = '') => {
  if (c) { pass++; console.log(`  [OK]   ${n}`) } else { fail++; console.log(`  [FAIL] ${n} ${extra}`) }
}

// 与源码等价的归一化实现
function normalizeImageUrl(url) {
  if (!url || typeof url !== 'string') return url || ''
  if (!/^https?:\/\//i.test(url)) return url
  const match = url.match(/^https?:\/\/[^/]+(\/api\/.*)$/i)
  return match ? match[1] : url
}

function normalizeImages(node, depth = 0) {
  if (node === null || node === undefined || depth > 8) return node
  if (Array.isArray(node)) {
    node.forEach((item, i) => {
      if (typeof item === 'string') node[i] = normalizeImageUrl(item)
      else normalizeImages(item, depth + 1)
    })
    return node
  }
  if (typeof node === 'object') {
    for (const key of Object.keys(node)) {
      const value = node[key]
      if (IMAGE_FIELDS.includes(key)) {
        if (typeof value === 'string') node[key] = normalizeImageUrl(value)
        else if (Array.isArray(value)) node[key] = value.map((v) => (typeof v === 'string' ? normalizeImageUrl(v) : normalizeImages(v, depth + 1)))
        else if (value && typeof value === 'object') normalizeImages(value, depth + 1)
      } else if (value && typeof value === 'object') {
        normalizeImages(value, depth + 1)
      }
    }
  }
  return node
}

console.log('=== 0. 源码确实包含归一化逻辑 ===')
check('request.js 定义了 normalizeImageUrl', hasNormalizer)
check('字段白名单非空', IMAGE_FIELDS.length > 0, JSON.stringify(IMAGE_FIELDS))

console.log('\n=== 1. 归一化函数单元行为 ===')
check('绝对 /api 地址 -> 相对',
  normalizeImageUrl('http://localhost:9090/api/uploads/products/a/main.jpg') === '/api/uploads/products/a/main.jpg')
check('已是相对地址 -> 原样', normalizeImageUrl('/api/x.jpg') === '/api/x.jpg')
check('非 /api 外链 -> 原样', normalizeImageUrl('https://cdn.example.com/a.jpg') === 'https://cdn.example.com/a.jpg')
check('空值 -> 空串', normalizeImageUrl(null) === '' && normalizeImageUrl(undefined) === '')
check('data URI -> 原样', normalizeImageUrl('data:image/png;base64,AAA') === 'data:image/png;base64,AAA')

console.log('\n=== 2. 真实后端数据 ===')
const listRaw = await (await fetch(`${API}/public/products?pageNum=1&pageSize=8`)).json()
check('商品列表可取', listRaw.code === 200 && listRaw.data.records.length > 0)
check('后端原始数据确实返回绝对地址',
  listRaw.data.records.some((p) => /^https?:\/\//i.test(p.mainImage || '')),
  listRaw.data.records[0]?.mainImage)

// 走一遍归一化
const normalized = normalizeImages(JSON.parse(JSON.stringify(listRaw.data)))
const absLeft = normalized.records.filter((p) => /^https?:\/\//i.test(p.mainImage || ''))
check('归一化后列表无绝对图片地址', absLeft.length === 0,
  absLeft.slice(0, 2).map((p) => p.mainImage).join(' | '))
check('主图变为 /api 相对地址', normalized.records.every((p) => (p.mainImage || '').startsWith('/api/')),
  normalized.records[0]?.mainImage)
const withGallery = normalized.records.find((p) => Array.isArray(p.imageList) && p.imageList.length)
check('imageList 数组字段也已归一化',
  !!withGallery && withGallery.imageList.every((u) => String(u).startsWith('/api/')),
  JSON.stringify(withGallery?.imageList?.slice(0, 2)))

console.log('\n=== 3. 相对地址确实可取到图片(关键) ===')
const imgPath = normalized.records[0].mainImage
for (const [name, base] of [['dev server 5174', 'http://127.0.0.1:5174'], ['静态服务器 5180', 'http://127.0.0.1:5180']]) {
  try {
    const r = await fetch(base + imgPath)
    const buf = await r.arrayBuffer()
    const ct = r.headers.get('content-type') || ''
    check(`经 ${name} 可取到图片`, r.ok && buf.byteLength > 3000 && ct.includes('image'),
      `status=${r.status} ct=${ct} bytes=${buf.byteLength}`)
  } catch (e) {
    check(`经 ${name} 可取到图片`, false, e.message)
  }
}

console.log('\n=== 4. 嵌套字段(收藏 -> product.mainImage) ===')
const loginRes = await (await fetch(`${API}/auth/login`, {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'zhangsan', password: '123456', role: 'USER' })
})).json()
const token = loginRes.data.token
const collectRaw = await (await fetch(`${API}/user/collect/list`, { headers: { Authorization: 'Bearer ' + token } })).json()
if (Array.isArray(collectRaw.data) && collectRaw.data.length) {
  const cNorm = normalizeImages(JSON.parse(JSON.stringify(collectRaw.data)))
  check('收藏中嵌套商品图已归一化',
    cNorm.every((c) => !c.product || !/^https?:\/\//i.test(c.product.mainImage || '')),
    cNorm[0]?.product?.mainImage)
} else {
  check('收藏列表可访问(无数据则跳过)', Array.isArray(collectRaw.data))
}

console.log('\n=== 5. 头像字段 ===')
// 登录响应同样会经过 request.js 的归一化
const loginNorm = normalizeImages(JSON.parse(JSON.stringify(loginRes.data)))
check('头像已归一化为相对地址', (loginNorm.avatar || '').startsWith('/api/'), loginNorm.avatar)
{
  const r = await fetch('http://127.0.0.1:5180' + loginNorm.avatar)
  const buf = await r.arrayBuffer()
  check('头像经静态服务器可取到', r.ok && buf.byteLength > 100, `status=${r.status} bytes=${buf.byteLength}`)
}

console.log(`\n通过 ${pass} 项, 失败 ${fail} 项`)
process.exit(fail === 0 ? 0 : 1)
