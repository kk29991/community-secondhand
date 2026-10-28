/**
 * 后端 SVG 占位图工具
 * 与后端 PlaceholderController 生成的 URL 完全一致:
 *   http://localhost:9090/api/public/placeholder.svg?text=商品名&w=400&h=400
 * 走 Vite 代理时也可以使用相对地址 /api/public/placeholder.svg
 */

/** 后端服务地址 (图片直连后端, 避免代理层多余开销) */
export const BACKEND_ORIGIN = 'http://localhost:9090'

/**
 * 生成占位图 URL
 * @param {string} text 图片上的文字
 * @param {number} w 宽
 * @param {number} h 高
 * @param {string} [bg] 背景色, 形如 ff7a45 或 #ff7a45
 * @param {boolean} [relative] 是否使用相对地址(走 Vite 代理)
 */
export function placeholder(text = '商品', w = 400, h = 400, bg, relative = false) {
  const base = relative ? '/api/public/placeholder.svg' : `${BACKEND_ORIGIN}/api/public/placeholder.svg`
  const params = new URLSearchParams()
  params.set('text', String(text ?? '商品'))
  params.set('w', String(w))
  params.set('h', String(h))
  if (bg) params.set('bg', String(bg).replace('#', ''))
  return `${base}?${params.toString()}`
}

/** 全站默认占位图 (相对地址, 永远可用) */
export const DEFAULT_IMAGE = '/api/public/placeholder.svg?text=%E5%95%86%E5%93%81&w=400&h=400'

/**
 * 把后端返回的绝对图片地址归一化为「同源相对地址」。
 * 与 api/request.js 中的同名函数保持一致(那边为避免循环依赖单独实现了一份)。
 *
 * @param {string} url 原始地址
 * @returns {string} 相对地址或原样返回
 */
export function normalizeImageUrl(url) {
  if (!url || typeof url !== 'string') return url || ''
  if (!/^https?:\/\//i.test(url)) return url
  const match = url.match(/^https?:\/\/[^/]+(\/api\/.*)$/i)
  return match ? match[1] : url
}

/** 图片加载失败时兜底 */
export function fallbackImage(e, text = '商品') {
  const el = e?.target
  if (!el || el.dataset.fallback === '1') return
  el.dataset.fallback = '1'
  el.src = placeholder(text, 400, 400, undefined, true)
}

/**
 * 解析后端的 images 字段 (JSON 数组字符串 / 逗号分隔), 返回图片数组
 * @param {string|string[]} images
 * @param {string} [mainImage] 兜底主图
 */
export function resolveImages(images, mainImage) {
  let list = []
  if (Array.isArray(images)) {
    list = images.filter(Boolean)
  } else if (typeof images === 'string' && images.trim()) {
    const raw = images.trim()
    if (raw.startsWith('[')) {
      try {
        const parsed = JSON.parse(raw)
        if (Array.isArray(parsed)) list = parsed.filter(Boolean)
      } catch (e) {
        list = []
      }
    } else {
      list = raw.split(',').map((s) => s.trim()).filter(Boolean)
    }
  }
  if (!list.length && mainImage) list = [mainImage]
  // 统一归一化为同源相对地址
  return list.map(normalizeImageUrl)
}

/** 金额格式化: 保留两位小数 */
export function money(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return '0.00'
  return num.toFixed(2)
}

/** 日期时间格式化: 2026-09-16T19:04:12 -> 2026-09-16 19:04:12 */
export function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

/** 只取日期: 2026-09-16 */
export function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 10)
}
