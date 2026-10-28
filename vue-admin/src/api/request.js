import axios from 'axios'
import { ElMessage } from 'element-plus'

/** 需要归一化为「同源相对地址」的图片字段 */
const IMAGE_FIELDS = [
  'mainImage', 'productImage', 'avatar', 'storeLogo', 'image', 'icon',
  'images', 'imageList', 'logoSrc'
]

/**
 * 把后端返回的绝对图片地址(http://localhost:9090/api/...) 改写为同源相对地址(/api/...)。
 * 页面可能部署在 5175(dev) 或 5181(静态), 与后端不同源; /api 在两种部署下都有代理,
 * 统一走相对地址可避免跨源加载失败(IPv6 解析 / 防火墙 / 浏览器安全策略)。
 */
export function normalizeImageUrl(url) {
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

/**
 * 统一 Axios 封装
 * - 自动注入 token (Authorization: Bearer <token>)
 * - code === 200 时直接把 data 交给调用方
 * - code !== 200 统一 ElMessage.error(message) 并 reject
 * - 401 清除登录态并跳登录页; 403 提示越权
 *
 * 注意: 本项目后端把 401 / 403 也放在 HTTP 200 的响应体里返回
 * (例如 {"code":401,"message":"请先登录"}), 所以必须在业务层判断 code。
 */

const TOKEN_KEY = 'mall_admin_token'

/** 未授权处理器: 由 main.js 注入, 避免 request <-> store <-> router 循环依赖 */
let unauthorizedHandler = null
export function setUnauthorizedHandler(fn) {
  unauthorizedHandler = fn
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}
export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
}
export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// ---------------------------------------------------------------- 请求拦截
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---------------------------------------------------------------- 响应拦截
request.interceptors.response.use(
  (response) => {
    // 二进制流直接返回
    if (response.config && response.config.responseType === 'blob') {
      return response.data
    }

    const res = response.data
    const silent = !!(response.config && response.config.silent)

    // 非标准返回 (例如直接返回字符串), 原样透传
    if (res === null || res === undefined || typeof res !== 'object' || !('code' in res)) {
      return res
    }

    if (res.code === 200) {
      return normalizeImages(res.data)
    }

    const message = res.message || '请求失败'

    if (res.code === 401) {
      if (!silent) ElMessage.error(message || '登录已过期，请重新登录')
      clearToken()
      if (unauthorizedHandler) unauthorizedHandler()
    } else if (res.code === 403) {
      if (!silent) ElMessage.error(message || '无权限访问该资源')
    } else {
      if (!silent) ElMessage.error(message)
    }

    const err = new Error(message)
    err.code = res.code
    return Promise.reject(err)
  },
  (error) => {
    const config = error.config || {}
    const silent = !!config.silent
    const status = error.response && error.response.status
    let message = '网络异常，请稍后重试'

    if (status === 401) {
      message = '登录已过期，请重新登录'
      clearToken()
      if (unauthorizedHandler) unauthorizedHandler()
    } else if (status === 403) {
      message = '无权限访问该资源'
    } else if (status === 404) {
      message = '请求的接口不存在'
    } else if (status >= 500) {
      message = '服务器繁忙，请稍后重试'
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请检查后端服务是否已启动'
    } else if (error.message && error.message.includes('Network Error')) {
      message = '无法连接后端服务 (http://localhost:9090)'
    }

    if (!silent) ElMessage.error(message)
    const err = new Error(message)
    err.code = status
    return Promise.reject(err)
  }
)

export default request
