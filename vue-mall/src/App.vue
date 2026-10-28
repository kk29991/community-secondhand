<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

// 登录/注册页使用独立简洁布局
const bare = computed(() => route.meta.bare === true)

onMounted(() => {
  // 有 token 时静默同步购物车角标
  if (userStore.isLogin) {
    userStore.refreshCartCount()
  }
})
</script>

<template>
  <div class="app-shell">
    <AppHeader v-if="!bare" />
    <main class="app-main" :class="{ 'is-bare': bare }">
      <router-view v-slot="{ Component }">
        <component :is="Component" />
      </router-view>
    </main>
    <AppFooter v-if="!bare" />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}
.app-main {
  flex: 1;
  padding-bottom: 40px;
}
.app-main.is-bare {
  padding-bottom: 0;
}
</style>
