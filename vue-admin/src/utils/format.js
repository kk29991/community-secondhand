/** 通用格式化工具 */

/** 金额: 907 -> 907.00 */
export function money(value) {
  const n = Number(value)
  if (!isFinite(n)) return '0.00'
  return n.toFixed(2)
}

/** 金额带前缀: ¥907.00 */
export function yuan(value) {
  return `¥${money(value)}`
}

/** 大数字缩写: 12163 -> 1.22万 (可选) */
export function compactNumber(value) {
  const n = Number(value) || 0
  if (n >= 10000) return `${(n / 10000).toFixed(2)}万`
  return String(n)
}

/** 日期时间: 2026-09-16T19:04:12 -> 2026-09-16 19:04:12 */
export function dateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

/** 日期: 2026-09-16 */
export function dateOnly(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 10)
}

/** 安全解析 JSON 字符串数组 (后端 images 字段是 JSON 字符串) */
export function parseImageList(value) {
  if (!value) return []
  if (Array.isArray(value)) return value.filter(Boolean)
  try {
    const arr = JSON.parse(value)
    return Array.isArray(arr) ? arr.filter(Boolean) : []
  } catch (e) {
    // 退化为逗号分隔
    return String(value)
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
  }
}

/** 标签 id: 后端可能返回 "1,2" 或 "1,2," 或数组 */
export function parseIdList(value) {
  if (value === null || value === undefined || value === '') return []
  if (Array.isArray(value)) return value.map((v) => Number(v)).filter((v) => !isNaN(v))
  return String(value)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
    .map((s) => Number(s))
    .filter((v) => !isNaN(v))
}

/** 后端图片地址可能是相对路径, 统一处理 */
export function resolveImage(url) {
  if (!url) return ''
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:')) return url
  return url.startsWith('/') ? url : `/${url}`
}

/** 生成占位图 (无真实图片时演示用) */
export function placeholder(text, w = 400, h = 400) {
  return `/api/public/placeholder.svg?text=${encodeURIComponent(text || '商品')}&w=${w}&h=${h}`
}

/* --------------------------------------------------- 订单状态字典 */
export const ORDER_STATUS = {
  0: { text: '待付款', type: 'warning' },
  1: { text: '待发货', type: 'primary' },
  2: { text: '待收货', type: 'info' },
  3: { text: '已完成', type: 'success' },
  4: { text: '已取消', type: 'info' },
  5: { text: '已退款', type: 'danger' }
}

export function orderStatusText(status) {
  const item = ORDER_STATUS[status]
  return item ? item.text : '未知'
}

export function orderStatusType(status) {
  const item = ORDER_STATUS[status]
  return item ? item.type : 'info'
}

/** 订单状态 Tab 选项 */
export const ORDER_STATUS_TABS = [
  { label: '全部', value: '' },
  { label: '待付款', value: 0 },
  { label: '待发货', value: 1 },
  { label: '待收货', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 },
  { label: '已退款', value: 5 }
]

/** 公告类型 */
export const ANNOUNCEMENT_TYPES = [
  { label: '系统公告', value: 1, type: 'primary' },
  { label: '活动通知', value: 2, type: 'success' },
  { label: '维护通知', value: 3, type: 'warning' }
]

export function announcementTypeText(type) {
  const item = ANNOUNCEMENT_TYPES.find((t) => t.value === type)
  return item ? item.label : '未知'
}

export function announcementTypeTag(type) {
  const item = ANNOUNCEMENT_TYPES.find((t) => t.value === type)
  return item ? item.type : 'info'
}

/** 轮播图位置 */
export const BANNER_POSITIONS = [
  { label: '首页轮播', value: 1 },
  { label: '首页广告位', value: 2 }
]

export function bannerPositionText(position) {
  const item = BANNER_POSITIONS.find((t) => t.value === position)
  return item ? item.label : '未知'
}

/** 手机号校验 */
export function isPhone(value) {
  return /^1[3-9]\d{9}$/.test(String(value || ''))
}
