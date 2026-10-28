import request from './request'
import { setAuth, getToken } from './request'

/* ---------------- 认证 ---------------- */

export const login = (data) => request.post('/auth/login', data).then((res) => {
  setAuth(res)
  return res
})

export const register = (data) => request.post('/auth/register', data)

export const logout = () => request.post('/auth/logout')

export const getProfile = () => request.get('/auth/profile')

export const updateProfile = (data) => request.put('/auth/profile', data)

export const updatePassword = (data) => request.put('/auth/password', data)

/* ---------------- 公开数据 ---------------- */

export const getCategories = () => request.get('/public/categories')

export const getProducts = (params) => request.get('/public/products', { params })

export const getProductDetail = (id) => request.get(`/public/products/${id}`)

export const getHotProducts = (limit = 8) => request.get('/public/products/hot', { params: { limit } })

export const getProductReviews = (id, params) => request.get(`/public/products/${id}/reviews`, { params })

export const getBanners = (position) => request.get('/public/banners', { params: { position } })

export const getAnnouncements = (limit = 5) => request.get('/public/announcements', { params: { limit } })

export const getAnnouncement = (id) => request.get(`/public/announcements/${id}`)

export const getMerchantTags = (merchantId) => request.get(`/public/merchants/${merchantId}/tags`)

/* ---------------- 购物车 ---------------- */

export const getCartList = () => request.get('/cart/list')

export const getCartCount = () => request.get('/cart/count')

export const addToCart = (productId, quantity = 1) => request.post('/cart/add', { productId, quantity })

export const updateCartQuantity = (id, quantity) => request.put('/cart/quantity', { id, quantity })

export const updateCartChecked = (id, checked) => request.put('/cart/checked', null, { params: { id, checked } })

export const updateCartCheckedAll = (checked) => request.put('/cart/checked/all', null, { params: { checked } })

export const removeCartItems = (cartIds) => request.delete('/cart/remove', { data: cartIds })

export const clearCart = () => request.delete('/cart/clear')

/* ---------------- 收货地址 ---------------- */

export const getAddressList = () => request.get('/address/list')

export const getAddress = (id) => request.get(`/address/${id}`)

export const saveAddress = (data) => (data.id
  ? request.put(`/address/${data.id}`, data)
  : request.post('/address', data))

export const setDefaultAddress = (id) => request.put(`/address/${id}/default`)

export const deleteAddress = (id) => request.delete(`/address/${id}`)

/* ---------------- 订单 ---------------- */

export const previewOrder = (params) => request.get('/orders/preview', { params })

export const createOrder = (data) => request.post('/orders', data)

export const payOrder = (id, payType) => request.put(`/orders/${id}/pay`, null, { params: { payType } })

export const cancelOrder = (id, reason) => request.put(`/orders/${id}/cancel`, null, { params: { reason } })

export const receiveOrder = (id) => request.put(`/orders/${id}/receive`)

export const refundOrder = (id, reason) => request.put(`/orders/${id}/refund`, null, { params: { reason } })

export const getOrderList = (params) => request.get('/orders/list', { params })

export const getOrderDetail = (id) => request.get(`/orders/${id}`)

export const getOrderCounts = () => request.get('/orders/counts')

/* ---------------- 收藏 ---------------- */

export const toggleCollect = (productId) => request.post(`/user/collect/${productId}`)

export const getCollectList = () => request.get('/user/collect/list')

export const removeCollect = (productId) => request.delete(`/user/collect/${productId}`)

/* ---------------- 评价 ---------------- */

export const submitReview = (data) => request.post('/user/reviews', data)

export const getPendingReviews = () => request.get('/user/reviews/pending')

export const getMyReviews = (params) => request.get('/user/reviews/list', { params })

/* ---------------- AI 导购 ---------------- */

export const aiChat = (message, sessionId) => request.post('/ai/chat', { message, sessionId })

export const getAiStatus = () => request.get('/ai/status')

/**
 * 流式对话 (SSE over POST)。
 * 浏览器原生 EventSource 只支持 GET 且不能带 Authorization 头, 所以用 fetch + ReadableStream 解析。
 *
 * @param {string} message 用户输入
 * @param {string|null} sessionId 会话ID
 * @param {{onDelta:Function, onMeta:Function, signal?:AbortSignal}} handlers
 */
export async function aiChatStream(message, sessionId, { onDelta, onMeta, signal } = {}) {
  const token = getToken()
  const res = await fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify({ message, sessionId: sessionId || null }),
    signal
  })

  if (!res.ok) {
    throw new Error(`流式接口返回 ${res.status}`)
  }
  if (!res.body) {
    throw new Error('当前浏览器不支持流式响应')
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  // SSE 以空行分隔事件块
  const dispatch = (block) => {
    let event = 'message'
    let data = ''
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) event = line.slice(6).trim()
      // 按 SSE 规范: 只剥离冒号后的第一个空格, 其余空格属于数据内容
      else if (line.startsWith('data:')) data += line.slice(5).replace(/^ /, '')
    }
    if (!data) return
    if (event === 'delta') {
      onDelta && onDelta(data)
    } else if (event === 'meta') {
      try {
        onMeta && onMeta(JSON.parse(data))
      } catch (e) {
        // 元信息解析失败不影响正文
      }
    } else if (event === 'error') {
      throw new Error(data)
    }
  }

  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    let idx
    while ((idx = buffer.indexOf('\n\n')) >= 0) {
      const block = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)
      dispatch(block)
    }
  }
  if (buffer.trim()) dispatch(buffer)
}

export const getAiHistory = (sessionId) => request.get('/ai/history', { params: { sessionId } })

export const clearAiSession = (sessionId) => request.delete('/ai/session', { params: { sessionId } })

/* ---------------- 上传 ---------------- */

export const uploadImage = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/upload/image', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
