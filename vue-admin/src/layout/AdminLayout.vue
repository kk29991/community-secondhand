<template>
  <el-container class="admin-layout">
    <!-- ------------------------------------------------ 左侧菜单 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="admin-aside">
      <div class="logo" :class="{ 'logo-collapse': isCollapse }">
        <span class="logo-mark">二手</span>
        <span v-show="!isCollapse" class="logo-text">后台管理</span>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          background-color="#2b2f3a"
          text-color="#c0c4cc"
          active-text-color="#ff6700"
        >
          <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>

      <div v-show="!isCollapse" class="aside-footer">
        <span class="role-badge">{{ roleLabel }}</span>
      </div>
    </el-aside>

    <el-container>
      <!-- ---------------------------------------------- 顶部栏 -->
      <el-header class="admin-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>

          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ roleLabel }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-tag v-if="userStore.isMerchant" type="warning" effect="plain" size="small">
            {{ userStore.userInfo?.storeName || '我的店铺' }}
          </el-tag>

          <el-tooltip content="刷新页面" placement="bottom">
            <el-icon class="header-icon" @click="reload"><Refresh /></el-icon>
          </el-tooltip>

          <el-dropdown @command="onCommand">
            <div class="user-box">
              <el-avatar :size="30" :src="userStore.avatar">
                {{ (userStore.displayName || 'U').slice(0, 1) }}
              </el-avatar>
              <span class="user-name">{{ userStore.displayName || '未登录' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  账号：{{ userStore.userInfo?.username || '-' }}
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- ---------------------------------------------- 主内容区 -->
      <el-main class="admin-main">
        <router-view v-slot="{ Component }">
          <keep-alive :max="6">
            <component :is="Component" :key="route.fullPath" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)

/** 商家菜单 */
const MERCHANT_MENUS = [
  { path: '/dashboard', title: '经营看板', icon: 'Odometer' },
  { path: '/products', title: '商品管理', icon: 'Goods' },
  { path: '/orders', title: '订单管理', icon: 'List' },
  { path: '/reviews', title: '评价管理', icon: 'ChatDotRound' },
  { path: '/tags', title: '标签管理', icon: 'PriceTag' }
]

/** 管理员菜单 */
const ADMIN_MENUS = [
  { path: '/admin/overview', title: '平台概览', icon: 'DataBoard' },
  { path: '/admin/statistics', title: '销售统计', icon: 'TrendCharts' },
  { path: '/admin/users', title: '用户管理', icon: 'User' },
  { path: '/admin/merchants', title: '商家管理', icon: 'Shop' },
  { path: '/admin/admins', title: '管理员列表', icon: 'Avatar' },
  { path: '/admin/categories', title: '分类管理', icon: 'Menu' },
  { path: '/admin/products', title: '商品管理', icon: 'GoodsFilled' },
  { path: '/admin/orders', title: '订单管理', icon: 'Tickets' },
  { path: '/admin/reviews', title: '评价管理', icon: 'Star' },
  { path: '/admin/announcements', title: '公告管理', icon: 'Bell' },
  { path: '/admin/banners', title: '轮播图管理', icon: 'Picture' }
]

/** 按角色动态渲染菜单 */
const menus = computed(() => (userStore.isAdmin ? ADMIN_MENUS : MERCHANT_MENUS))

const roleLabel = computed(() => (userStore.isAdmin ? '超级管理员' : '商家中心'))

const activeMenu = computed(() => route.path)

const currentTitle = computed(() => (route.meta && route.meta.title) || '首页')

function reload() {
  window.location.reload()
}

async function onCommand(command) {
  if (command !== 'logout') return
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定退出',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  userStore.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

/* ------------------------------------------------------------ 侧边栏 */
.admin-aside {
  background: #2b2f3a;
  display: flex;
  flex-direction: column;
  transition: width 0.25s;
  overflow: hidden;
}

.logo {
  height: 58px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  overflow: hidden;
  white-space: nowrap;
}

.logo-collapse {
  justify-content: center;
  padding: 0;
}

.logo-mark {
  background: var(--el-color-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  border-radius: 6px;
  padding: 4px 7px;
  flex-shrink: 0;
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.menu-scroll {
  flex: 1;
  overflow-x: hidden;
}

.menu-scroll :deep(.el-menu) {
  border-right: none;
}

.menu-scroll :deep(.el-menu-item.is-active) {
  background-color: rgba(255, 103, 0, 0.12);
}

.menu-scroll :deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.05);
}

.aside-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.role-badge {
  display: block;
  text-align: center;
  font-size: 12px;
  color: #ff6700;
  background: rgba(255, 103, 0, 0.12);
  border-radius: 4px;
  padding: 4px 0;
}

/* -------------------------------------------------------------- 顶部 */
.admin-header {
  height: 58px;
  background: #fff;
  border-bottom: 1px solid var(--admin-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 19px;
  cursor: pointer;
  color: #606266;
}

.collapse-btn:hover {
  color: var(--el-color-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  font-size: 17px;
  cursor: pointer;
  color: #606266;
}

.header-icon:hover {
  color: var(--el-color-primary);
}

.user-box {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-name {
  font-size: 14px;
  color: #303133;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ------------------------------------------------------------ 主内容 */
.admin-main {
  background: var(--admin-bg);
  padding: 0;
  overflow-y: auto;
}
</style>
