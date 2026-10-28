<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useCategoryStore } from '@/stores/category'
import { DEFAULT_IMAGE, fallbackImage } from '@/utils/placeholder'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const categoryStore = useCategoryStore()

const keyword = ref('')
const inputRef = ref(null)

// 搜索页保持输入框同步
watch(
  () => route.query.keyword,
  (val) => {
    keyword.value = val ? String(val) : ''
  },
  { immediate: true }
)

onMounted(() => {
  categoryStore.load()
})

const categories = computed(() => categoryStore.categories)

const cartBadge = computed(() => userStore.cartCount)

function search() {
  const kw = keyword.value.trim()
  router.push({ path: '/search', query: kw ? { keyword: kw } : {} })
}

function goCategory(cat) {
  router.push({ path: '/search', query: { categoryId: cat.id } })
}

async function handleCommand(cmd) {
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗?', '提示', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch (e) {
      return
    }
    await userStore.logout()
    ElMessage.success('已退出登录')
    if (route.meta?.requireAuth || ['/user', '/cart', '/confirmOrder', '/payResult', '/ai'].some((p) => route.path.startsWith(p))) {
      router.push('/')
    } else {
      router.replace({ path: route.path, query: route.query })
    }
    return
  }
  router.push(cmd)
}
</script>

<template>
  <header class="app-header">
    <!-- 顶部通栏 -->
    <div class="topbar">
      <div class="mall-container topbar-inner">
        <span class="topbar-slogan">社区二手街 · 社区二手交易平台</span>
        <div class="topbar-right">
          <template v-if="userStore.isLogin">
            <span class="topbar-user">
              <img class="topbar-avatar" :src="userStore.avatar || DEFAULT_IMAGE" alt="头像" @error="fallbackImage($event, '头像')" />
              {{ userStore.nickname }}
            </span>
            <el-divider direction="vertical" />
            <router-link to="/user/orders">我的订单</router-link>
            <el-divider direction="vertical" />
            <router-link to="/user/collect">我的收藏</router-link>
            <el-divider direction="vertical" />
            <a href="javascript:;" @click="handleCommand('logout')">退出登录</a>
          </template>
          <template v-else>
            <router-link to="/login">登录</router-link>
            <el-divider direction="vertical" />
            <router-link to="/register">注册</router-link>
            <el-divider direction="vertical" />
            <router-link to="/login">我的订单</router-link>
          </template>
        </div>
      </div>
    </div>

    <!-- 主导航 -->
    <div class="navbar">
      <div class="mall-container navbar-inner">
        <div class="logo" @click="router.push('/')">
          <span class="logo-mark">二</span>
          <span class="logo-text">社区二手街</span>
        </div>

        <nav class="main-nav">
          <router-link to="/" class="nav-link" :class="{ active: route.path === '/' }">首页</router-link>
          <router-link to="/search" class="nav-link" :class="{ active: route.path === '/search' }">全部商品</router-link>
          <router-link to="/ai" class="nav-link" :class="{ active: route.path === '/ai' }">AI 导购</router-link>
        </nav>

        <div class="search-box">
          <input
            ref="inputRef"
            v-model="keyword"
            class="search-input"
            type="text"
            placeholder="搜索商品名称 / 关键词"
            @keyup.enter="search"
          />
          <button class="search-btn" @click="search">
            <el-icon><Search /></el-icon>
          </button>
        </div>

        <div class="header-actions">
          <el-dropdown v-if="userStore.isLogin" trigger="click" @command="handleCommand">
            <span class="user-trigger">
              <el-icon><User /></el-icon>
              <span class="user-name">{{ userStore.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/user/profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="/user/orders">我的订单</el-dropdown-item>
                <el-dropdown-item command="/user/reviews">我的评价</el-dropdown-item>
                <el-dropdown-item command="/user/address">收货地址</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <router-link v-else to="/login" class="login-link">
            <el-icon><User /></el-icon>
            登录 / 注册
          </router-link>

          <router-link to="/cart" class="cart-link">
            <el-badge :value="cartBadge" :hidden="!cartBadge" :max="99">
              <el-icon :size="20"><ShoppingCart /></el-icon>
            </el-badge>
            <span class="cart-text">购物车</span>
          </router-link>
        </div>
      </div>
    </div>

    <!-- 分类快捷条 (首页以外也展示, 便于跳转) -->
    <div class="catbar">
      <div class="mall-container catbar-inner">
        <span class="catbar-label">全部商品分类</span>
        <ul class="catbar-list">
          <li v-for="cat in categories" :key="cat.id">
            <a href="javascript:;" @click="goCategory(cat)">{{ cat.name }}</a>
          </li>
        </ul>
        <router-link class="catbar-ai" to="/ai">
          <el-icon><MagicStick /></el-icon>
          AI 帮你挑
        </router-link>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

/* ---------- 顶部通栏 ---------- */
.topbar {
  background: #333;
  color: #b0b0b0;
  font-size: 12px;
  line-height: 34px;
}

.topbar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.topbar-slogan {
  color: #8c8c8c;
  letter-spacing: 0.5px;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 2px;
}

.topbar-right a {
  color: #b0b0b0;
  transition: color 0.2s;
}

.topbar-right a:hover {
  color: #fff;
}

.topbar-user {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #ff6700;
}

.topbar-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
}

.topbar :deep(.el-divider--vertical) {
  border-color: #4a4a4a;
  margin: 0 8px;
  height: 12px;
}

/* ---------- 主导航 ---------- */
.navbar-inner {
  display: flex;
  align-items: center;
  gap: 20px;
  height: 74px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
}

.logo-mark {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ff8f40, #ff6700);
  color: #fff;
  font-weight: 700;
  font-size: 17px;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: -0.5px;
}

.logo-text {
  font-size: 22px;
  font-weight: 600;
  color: #333;
  letter-spacing: 1px;
}

.main-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.nav-link {
  padding: 6px 10px;
  font-size: 15px;
  color: #333;
  border-radius: 6px;
  transition: all 0.2s;
}

.nav-link:hover,
.nav-link.active {
  color: #ff6700;
  background: #fff5ee;
}

/* 搜索框 */
.search-box {
  display: flex;
  align-items: center;
  margin-left: auto;
  width: 330px;
  height: 42px;
  border: 1px solid var(--mall-border);
  border-radius: 21px;
  overflow: hidden;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: #fff;
}

.search-box:focus-within {
  border-color: #ff6700;
  box-shadow: 0 0 0 3px rgba(255, 103, 0, 0.12);
}

.search-input {
  flex: 1;
  height: 100%;
  border: none;
  outline: none;
  padding: 0 18px;
  font-size: 14px;
  color: #333;
  background: transparent;
}

.search-input::placeholder {
  color: #bbb;
}

.search-btn {
  width: 56px;
  height: 100%;
  border: none;
  background: transparent;
  color: #757575;
  cursor: pointer;
  font-size: 17px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.search-btn:hover {
  color: #ff6700;
}

/* 右侧操作 */
.header-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  color: #333;
  font-size: 14px;
  outline: none;
}

.user-trigger:hover {
  color: #ff6700;
}

.user-name {
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.login-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #333;
}

.login-link:hover {
  color: #ff6700;
}

.cart-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 42px;
  padding: 0 16px;
  border-radius: 21px;
  background: #fff5ee;
  color: #ff6700;
  font-size: 14px;
  transition: all 0.2s;
}

.cart-link:hover {
  background: #ff6700;
  color: #fff;
}

.cart-text {
  font-size: 14px;
}

/* ---------- 分类快捷条 ---------- */
.catbar {
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

.catbar-inner {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 42px;
  font-size: 13px;
}

.catbar-label {
  color: #fff;
  background: #ff6700;
  border-radius: 12px;
  padding: 3px 12px;
  font-size: 12px;
  flex-shrink: 0;
}

.catbar-list {
  display: flex;
  align-items: center;
  gap: 18px;
  overflow: hidden;
  flex: 1;
}

.catbar-list a {
  color: #616161;
  transition: color 0.2s;
  white-space: nowrap;
}

.catbar-list a:hover {
  color: #ff6700;
}

.catbar-ai {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #ff6700;
  flex-shrink: 0;
}

@media (max-width: 1240px) {
  .search-box {
    width: 220px;
  }
  .catbar-list {
    gap: 12px;
  }
}
</style>
