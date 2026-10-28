import request from './request'

/**
 * 后端接口统一封装
 * 后端地址: http://localhost:9090  (Vite 代理 /api -> http://localhost:9090)
 * 统一返回 {code, message, data}; request.js 已把 data 解包出来
 */

/* ------------------------------------------------------------------ 认证 */
export const authApi = {
  /** 登录: role = MERCHANT | ADMIN */
  login: (data) => request.post('/auth/login', data)
}

/* -------------------------------------------------- 公共接口 (无需登录) */
export const publicApi = {
  /** 分类树, 用于商品表单的级联选择 */
  categories: () => request.get('/public/categories', { silent: true }),
  /** 演示用商家账号列表(仅用户名与店铺名), 登录页展示用 */
  demoMerchants: () => request.get('/public/demo-merchants', { silent: true })
}

/* ------------------------------------------------------------ 文件上传 */
export const uploadApi = {
  /** 上传图片, form-data 字段名 file, 返回 {url} */
  image: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/upload/image', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000
    })
  }
}

/* ===================================================================
 * 订单详情兼容处理
 * -------------------------------------------------------------------
 * 后端 GET /admin/merchant/orders/{id} 与 GET /admin/orders/{id}
 * 均固定返回 500: OrderService.fillItems() 内部对 List.of(order)
 * (不可变集合) 调用了 sort(), 抛 UnsupportedOperationException。
 * 由于不允许修改后端, 这里退化为从列表接口中按 id 查找 —— 列表接口
 * 已经内联返回了 items[] 与 statusText, 数据完全够用。
 * =================================================================== */
async function loadOrderDetail(base, id) {
  try {
    return await request.get(`${base}/orders/${id}`, { silent: true })
  } catch (e) {
    // 后端该接口异常, 回退: 列表接口已包含 items / statusText
    const page = await request.get(`${base}/orders`, {
      params: { pageNum: 1, pageSize: 100 }
    })
    const records = (page && page.records) || []
    const found = records.find((o) => String(o.id) === String(id))
    if (!found) throw new Error('订单不存在或无权查看')
    return found
  }
}

/* ==================================================== 商家端 MERCHANT */
const M = '/admin/merchant'

export const merchantApi = {
  /** 经营看板 */
  dashboard: () => request.get(`${M}/dashboard`),
  /** 销售统计 */
  statistics: (params) => request.get(`${M}/statistics`, { params }),

  /** 商品列表 (分页) */
  products: (params) => request.get(`${M}/products`, { params }),
  /** 商品详情 (编辑回显) */
  productDetail: (id) => request.get(`${M}/products/${id}`),
  /** 新增 / 编辑商品 */
  saveProduct: (data) => request.post(`${M}/products`, data),
  /** 上架 / 下架 */
  updateProductStatus: (id, status) =>
    request.put(`${M}/products/${id}/status`, null, { params: { status } }),
  /** 删除商品 */
  deleteProduct: (id) => request.delete(`${M}/products/${id}`),

  /** 订单列表 */
  orders: (params) => request.get(`${M}/orders`, { params }),
  /** 订单详情 (带后端 500 兼容回退) */
  orderDetail: (id) => loadOrderDetail(M, id),
  /** 发货 */
  deliver: (id, expressNo) =>
    request.put(`${M}/orders/${id}/deliver`, null, { params: { expressNo } }),
  /** 各状态订单数量 */
  orderCounts: () => request.get(`${M}/orders/counts`),

  /** 评价列表 */
  reviews: (params) => request.get(`${M}/reviews`, { params }),
  /** 回复评价 */
  replyReview: (id, content) => request.put(`${M}/reviews/${id}/reply`, { content }),
  /** 删除回复 */
  deleteReply: (id) => request.delete(`${M}/reviews/${id}/reply`),

  /** 标签列表 */
  tags: () => request.get(`${M}/tags`),
  /** 新增 / 编辑标签 */
  saveTag: (data) => request.post(`${M}/tags`, data),
  /** 删除标签 */
  deleteTag: (id) => request.delete(`${M}/tags/${id}`)
}

/* ===================================================== 管理员 ADMIN */
const A = '/admin'

export const adminApi = {
  /** 平台概览 */
  overview: () => request.get(`${A}/overview`),
  /** 销售统计 */
  statistics: (params) => request.get(`${A}/statistics/sales`, { params }),

  /** 用户管理 */
  users: (params) => request.get(`${A}/users`, { params }),
  updateUserStatus: (id, status) =>
    request.put(`${A}/users/${id}/status`, null, { params: { status } }),
  deleteUser: (id) => request.delete(`${A}/users/${id}`),

  /** 商家管理 */
  merchants: (params) => request.get(`${A}/merchants`, { params }),
  updateMerchantStatus: (id, status) =>
    request.put(`${A}/merchants/${id}/status`, null, { params: { status } }),
  deleteMerchant: (id) => request.delete(`${A}/merchants/${id}`),

  /** 管理员列表 */
  admins: (params) => request.get(`${A}/admins`, { params }),

  /** 分类管理 */
  categories: () => request.get(`${A}/categories`),
  categoryTree: () => request.get(`${A}/categories/tree`),
  saveCategory: (data) => request.post(`${A}/categories`, data),
  deleteCategory: (id) => request.delete(`${A}/categories/${id}`),

  /** 平台商品 */
  products: (params) => request.get(`${A}/products`, { params }),
  updateProductStatus: (id, status) =>
    request.put(`${A}/products/${id}/status`, null, { params: { status } }),

  /** 平台订单 */
  orders: (params) => request.get(`${A}/orders`, { params }),
  orderDetail: (id) => loadOrderDetail(A, id),

  /** 评价管理 */
  reviews: (params) => request.get(`${A}/reviews`, { params }),
  updateReviewStatus: (id, status) =>
    request.put(`${A}/reviews/${id}/status`, null, { params: { status } }),

  /** 公告管理 */
  announcements: (params) => request.get(`${A}/announcements`, { params }),
  saveAnnouncement: (data) => request.post(`${A}/announcements`, data),
  deleteAnnouncement: (id) => request.delete(`${A}/announcements/${id}`),

  /** 轮播图管理 */
  banners: (params) => request.get(`${A}/banners`, { params }),
  saveBanner: (data) => request.post(`${A}/banners`, data),
  deleteBanner: (id) => request.delete(`${A}/banners/${id}`)
}

export default { authApi, publicApi, uploadApi, merchantApi, adminApi }
