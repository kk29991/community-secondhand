import { createRouter, createWebHashHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import AdminLayout from '@/layout/AdminLayout.vue'

/**
 * 路由表
 * meta.roles: 允许访问的角色; 不写表示登录后即可访问
 * meta.title: 面包屑 / 菜单标题
 * meta.icon : 菜单图标 (Element Plus 图标组件名)
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      /* ---------------------------------------------------- 商家端 */
      {
        path: 'dashboard',
        name: 'MerchantDashboard',
        component: () => import('@/views/merchant/Dashboard.vue'),
        meta: { title: '经营看板', icon: 'Odometer', roles: ['MERCHANT'] }
      },
      {
        path: 'products',
        name: 'MerchantProducts',
        component: () => import('@/views/merchant/Products.vue'),
        meta: { title: '商品管理', icon: 'Goods', roles: ['MERCHANT'] }
      },
      {
        path: 'orders',
        name: 'MerchantOrders',
        component: () => import('@/views/merchant/Orders.vue'),
        meta: { title: '订单管理', icon: 'List', roles: ['MERCHANT'] }
      },
      {
        path: 'reviews',
        name: 'MerchantReviews',
        component: () => import('@/views/merchant/Reviews.vue'),
        meta: { title: '评价管理', icon: 'ChatDotRound', roles: ['MERCHANT'] }
      },
      {
        path: 'tags',
        name: 'MerchantTags',
        component: () => import('@/views/merchant/Tags.vue'),
        meta: { title: '标签管理', icon: 'PriceTag', roles: ['MERCHANT'] }
      },

      /* -------------------------------------------------- 管理员端 */
      {
        path: 'admin/overview',
        name: 'AdminOverview',
        component: () => import('@/views/admin/Overview.vue'),
        meta: { title: '平台概览', icon: 'DataBoard', roles: ['ADMIN'] }
      },
      {
        path: 'admin/statistics',
        name: 'AdminStatistics',
        component: () => import('@/views/admin/Statistics.vue'),
        meta: { title: '销售统计', icon: 'TrendCharts', roles: ['ADMIN'] }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理', icon: 'User', roles: ['ADMIN'] }
      },
      {
        path: 'admin/merchants',
        name: 'AdminMerchants',
        component: () => import('@/views/admin/Merchants.vue'),
        meta: { title: '商家管理', icon: 'Shop', roles: ['ADMIN'] }
      },
      {
        path: 'admin/audit',
        // 商家入驻审核: 直接复用商家管理页面
        redirect: '/admin/merchants'
      },
      {
        path: 'admin/admins',
        name: 'AdminAdmins',
        component: () => import('@/views/admin/Admins.vue'),
        meta: { title: '管理员列表', icon: 'Avatar', roles: ['ADMIN'] }
      },
      {
        path: 'admin/categories',
        name: 'AdminCategories',
        component: () => import('@/views/admin/Categories.vue'),
        meta: { title: '分类管理', icon: 'Menu', roles: ['ADMIN'] }
      },
      {
        path: 'admin/products',
        name: 'AdminProducts',
        component: () => import('@/views/admin/Products.vue'),
        meta: { title: '商品管理', icon: 'GoodsFilled', roles: ['ADMIN'] }
      },
      {
        path: 'admin/orders',
        name: 'AdminOrders',
        component: () => import('@/views/admin/Orders.vue'),
        meta: { title: '订单管理', icon: 'Tickets', roles: ['ADMIN'] }
      },
      {
        path: 'admin/reviews',
        name: 'AdminReviews',
        component: () => import('@/views/admin/Reviews.vue'),
        meta: { title: '评价管理', icon: 'Star', roles: ['ADMIN'] }
      },
      {
        path: 'admin/announcements',
        name: 'AdminAnnouncements',
        component: () => import('@/views/admin/Announcements.vue'),
        meta: { title: '公告管理', icon: 'Bell', roles: ['ADMIN'] }
      },
      {
        path: 'admin/banners',
        name: 'AdminBanners',
        component: () => import('@/views/admin/Banners.vue'),
        meta: { title: '轮播图管理', icon: 'Picture', roles: ['ADMIN'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在', public: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

/* ------------------------------------------------------------ 路由守卫 */
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const isLogin = userStore.isLogin

  // 1. 未登录: 只放行 public 页面, 其余跳登录页
  if (!isLogin) {
    if (to.meta && to.meta.public) {
      next()
    } else {
      next({ path: '/login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : {} })
    }
    return
  }

  // 2. 已登录再访问登录页 -> 回到自己的首页
  if (to.path === '/login') {
    next(userStore.homePath)
    return
  }

  // 3. 根路径 -> 按角色进入各自首页
  if (to.path === '/') {
    next(userStore.homePath)
    return
  }

  // 4. 角色越权拦截 (商家访问 /admin/** 等)
  const roles = to.meta && to.meta.roles
  if (roles && roles.length && !roles.includes(userStore.role)) {
    ElMessage.error('无权限访问该页面')
    next(userStore.homePath)
    return
  }

  next()
})

router.afterEach((to) => {
  const base = '社区二手交易平台 - 后台管理'
  document.title = to.meta && to.meta.title ? `${to.meta.title} | ${base}` : base
})

export default router
