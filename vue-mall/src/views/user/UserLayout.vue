<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { DEFAULT_IMAGE, fallbackImage } from '@/utils/placeholder'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = [
  { path: '/user/profile', title: '个人资料', icon: 'User' },
  { path: '/user/orders', title: '我的订单', icon: 'List' },
  { path: '/user/collect', title: '我的收藏', icon: 'Star' },
  { path: '/user/reviews', title: '我的评价', icon: 'ChatDotSquare' },
  { path: '/user/address', title: '收货地址', icon: 'LocationInformation' }
]

const activeMenu = computed(() => route.path)

const displayName = computed(() => userStore.profile?.nickname || userStore.nickname)
const avatar = computed(() => userStore.profile?.avatar || userStore.avatar || DEFAULT_IMAGE)

onMounted(() => {
  userStore.loadProfile(true)
  userStore.refreshCartCount()
})
</script>

<template>
  <div class="user-layout">
    <div class="mall-container">
      <div class="user-body">
        <aside class="user-aside">
          <div class="user-card">
            <img class="avatar" :src="avatar" alt="头像" @error="fallbackImage($event, displayName)" />
            <h3 class="nickname">{{ displayName }}</h3>
            <p class="username">@{{ userStore.user?.username || '-' }}</p>
            <el-tag size="small" type="danger" effect="plain">普通会员</el-tag>
          </div>

          <nav class="user-menu">
            <a
              v-for="menu in menus"
              :key="menu.path"
              href="javascript:;"
              class="menu-item"
              :class="{ active: activeMenu === menu.path }"
              @click="router.push(menu.path)"
            >
              <el-icon><component :is="menu.icon" /></el-icon>
              <span>{{ menu.title }}</span>
              <el-icon class="menu-arrow"><ArrowRight /></el-icon>
            </a>
          </nav>

          <div class="aside-extra">
            <router-link to="/cart" class="extra-link">
              <el-icon><ShoppingCart /></el-icon>
              购物车 ({{ userStore.cartCount }})
            </router-link>
            <router-link to="/ai" class="extra-link">
              <el-icon><MagicStick /></el-icon>
              AI 导购助手
            </router-link>
          </div>
        </aside>

        <section class="user-content">
          <router-view />
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-layout {
  padding-top: 18px;
}

.user-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.user-aside {
  width: 226px;
  flex-shrink: 0;
}

.user-card {
  background: #fff;
  border-radius: var(--mall-radius);
  padding: 24px 16px;
  text-align: center;
  margin-bottom: 14px;
}

.avatar {
  width: 78px;
  height: 78px;
  border-radius: 50%;
  object-fit: cover;
  margin: 0 auto 12px;
  border: 3px solid #fff2e6;
  background: #fafafa;
}

.nickname {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.username {
  margin: 0 0 10px;
  font-size: 12px;
  color: #b0b0b0;
}

.user-menu {
  background: #fff;
  border-radius: var(--mall-radius);
  padding: 8px 0;
  margin-bottom: 14px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 46px;
  padding: 0 18px;
  font-size: 14px;
  color: #616161;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.menu-item:hover {
  color: #ff6700;
  background: #fff8f4;
}

.menu-item.active {
  color: #ff6700;
  background: #fff8f4;
  border-left-color: #ff6700;
  font-weight: 500;
}

.menu-arrow {
  margin-left: auto;
  font-size: 12px;
  opacity: 0.5;
}

.aside-extra {
  background: #fff;
  border-radius: var(--mall-radius);
  padding: 10px 0;
}

.extra-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  font-size: 13px;
  color: #757575;
}

.extra-link:hover {
  color: #ff6700;
}

.user-content {
  flex: 1;
  min-width: 0;
}

@media (max-width: 900px) {
  .user-body {
    flex-direction: column;
  }
  .user-aside {
    width: 100%;
  }
}
</style>
