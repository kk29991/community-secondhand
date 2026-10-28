import { createRouter, createWebHashHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken } from '@/api/request'

/** 未登录时跳转登录页, 并携带回跳地址 */
function requireAuth(to) {
  if (getToken()) {
    return true
  }
  ElMessage.warning('请先登录')
  return { path: '/login', query: { redirect: to.fullPath } }
}

const routes = [
  { path: '/', name: 'home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
  { path: '/search', name: 'search', component: () => import('@/views/Search.vue'), meta: { title: '商品列表' } },
  { path: '/goodsDetail', name: 'goodsDetail', component: () => import('@/views/GoodsDetail.vue'), meta: { title: '商品详情' } },
  {
    path: '/cart',
    name: 'cart',
    component: () => import('@/views/Cart.vue'),
    meta: { title: '购物车' },
    beforeEnter: requireAuth
  },
  {
    path: '/confirmOrder',
    name: 'confirmOrder',
    component: () => import('@/views/ConfirmOrder.vue'),
    meta: { title: '确认订单' },
    beforeEnter: requireAuth
  },
  {
    path: '/payResult',
    name: 'payResult',
    component: () => import('@/views/PayResult.vue'),
    meta: { title: '支付结果' },
    beforeEnter: requireAuth
  },
  {
    path: '/user',
    component: () => import('@/views/user/UserLayout.vue'),
    meta: { title: '个人中心' },
    beforeEnter: requireAuth,
    children: [
      { path: '', redirect: '/user/profile' },
      { path: 'profile', name: 'userProfile', component: () => import('@/views/user/Profile.vue'), meta: { title: '个人资料' } },
      { path: 'orders', name: 'userOrders', component: () => import('@/views/user/Orders.vue'), meta: { title: '我的订单' } },
      { path: 'collect', name: 'userCollect', component: () => import('@/views/user/Collect.vue'), meta: { title: '我的收藏' } },
      { path: 'reviews', name: 'userReviews', component: () => import('@/views/user/Reviews.vue'), meta: { title: '我的评价' } },
      { path: 'address', name: 'userAddress', component: () => import('@/views/user/Address.vue'), meta: { title: '收货地址' } }
    ]
  },
  {
    path: '/ai',
    name: 'ai',
    component: () => import('@/views/AiShopping.vue'),
    meta: { title: 'AI 导购' },
    beforeEnter: requireAuth
  },
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { title: '登录', bare: true } },
  { path: '/register', name: 'register', component: () => import('@/views/Register.vue'), meta: { title: '注册', bare: true } },
  { path: '/:pathMatch(.*)*', name: 'notFound', component: () => import('@/views/NotFound.vue'), meta: { title: '页面不存在' } }
]

const router = createRouter({
  // 必须使用 hash 模式
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.afterEach((to) => {
  const title = to.meta?.title
  document.title = title ? `${title} - 社区二手街` : '社区二手街 - 社区二手交易平台'
})

export default router
