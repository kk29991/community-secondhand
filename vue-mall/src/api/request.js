import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 把后端返回的绝对图片地址归一化为「同源相对地址」。
 *
 * 后端返回 http://localhost:9090/api/uploads/xxx.jpg 这类绝对地址,
 * 而页面可能来自 127.0.0.1:5174 (dev) 或 127.0.0.1:5180 (静态部署),
 * 二者并不同源。跨源加载图片在 IPv6 解析 / 防火墙 / 浏览器安全策略下都可能失败,
 * 而 /api 在两种部署下都有代理, 因此统一改为 /api/... 相对地址。
 *
 * 注意: 这里刻意不 import @/utils/placeholder, 避免与其形成循环依赖。
 */
export function normalizeImageUrl(url) {
  if (!url || typeof url !== 'string') return url || ''
  if (!/^https?:\/\//i.test(url)) return url
  const match = url.match(/^https?:\/\/[^/]+(\/api\/.*)$/i)
  return match ? match[1] : url
}

/**
 * 统一 axios 实例
 * - baseURL 走 Vite 代理, 浏览器侧不出现跨域
 * - 请求自动携带 JWT
 * - 统一解包后端 { code, message, data } 结构
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 20000
})

/** token 在 localStorage 中的键, 两个前端各自独立 */
export const TOKEN_KEY = 'mall_mall_token'
export const USER_KEY = 'mall_mall_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setAuth(user) {
  if (user && user.token) {
    localStorage.setItem(TOKEN_KEY, user.token)
  }
  localStorage.setItem(USER_KEY, JSON.stringify(user || {}))
}

export function getCurrentUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || '{}')
  } catch (e) {
    return {}
  }
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** 需要归一化为「同源相对地址」的图片字段 */
const IMAGE_FIELDS = [
  'mainImage', 'productImage', 'avatar', 'storeLogo', 'image', 'icon',
  'images', 'imageList', 'logoSrc'
]

/**
 * 递归把响应体里的绝对图片地址(http://localhost:9090/api/... )
 * 改写为同源相对地址(/api/...)。
 *
 * 原因: 页面可能部署在 5174(dev) 或 5180(静态), 与后端不是同一个源。
 * 跨源加载图片在 IPv6 解析 / 防火墙 / 浏览器安全策略下可能失败,
 * 而 /api 在两种部署下都有代理, 因此统一走相对地址。
 */
function normalizeImages(node, depth = 0) {
  if (node === null || node === undefined || depth > 8) return node
  if (Array.isArray(node)) {
    node.forEach((item, i) => {
      if (typeof item === 'string') {
        node[i] = normalizeImageUrl(item)
      } else {
        normalizeImages(item, depth + 1)
      }
    })
    return node
  }
  if (typeof node === 'object') {
    for (const key of Object.keys(node)) {
      const value = node[key]
      if (IMAGE_FIELDS.includes(key)) {
        if (typeof value === 'string') {
          node[key] = normalizeImageUrl(value)
        } else if (Array.isArray(value)) {
          node[key] = value.map((v) => (typeof v === 'string' ? normalizeImageUrl(v) : normalizeImages(v, depth + 1)))
        } else if (value && typeof value === 'object') {
          normalizeImages(value, depth + 1)
        }
      } else if (value && typeof value === 'object') {
        normalizeImages(value, depth + 1)
      }
    }
  }
  return node
}

request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 非标准结构(如文件流)直接返回
    if (body === null || typeof body !== 'object' || body.code === undefined) {
      return body
    }
    if (body.code === 200) {
      return normalizeImages(body.data)
    }
    if (body.code === 401) {
      clearAuth()
      ElMessage.error(body.message || '登录已过期, 请重新登录')
      // 避免在登录页反复跳转
      if (!window.location.hash.includes('/login')) {
        setTimeout(() => {
          window.location.hash = '#/login'
        }, 600)
      }
      return Promise.reject(new Error(body.message || '未登录'))
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      clearAuth()
      ElMessage.error('登录已过期, 请重新登录')
    } else {
      ElMessage.error(error?.message?.includes('timeout') ? '请求超时, 请重试' : '网络异常, 请检查后端服务是否启动')
    }
    return Promise.reject(error)
  }
)

export default request
