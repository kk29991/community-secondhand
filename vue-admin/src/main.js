import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import 'element-plus/dist/index.css'
import '@/styles/index.css'

import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from '@/api/request'
import { useUserStore } from '@/stores/user'

/** 把启动期异常显示到页面上, 避免只留一个白屏(渲染逻辑在 index.html 的兜底脚本里) */
function reportBootError(err) {
  // eslint-disable-next-line no-console
  console.error('[启动失败]', err)
  if (typeof window !== 'undefined' && window.__showBootError) {
    window.__showBootError(err)
  }
}

try {
  const app = createApp(App)
  const pinia = createPinia()

  app.use(pinia)
  app.use(router)
  app.use(ElementPlus, { locale: zhCn })

  // 全量注册 Element Plus 图标, 模板中可直接 <el-icon><Goods /></el-icon>
  for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(name, component)
  }

  // 注入 401 处理: 清除登录态并跳登录页 (避免 request <-> store <-> router 循环依赖)
  const userStore = useUserStore(pinia)
  setUnauthorizedHandler(() => {
    userStore.resetAuth()
    const current = router.currentRoute.value
    if (!current.meta || current.meta.public !== true) {
      router.replace({ path: '/login', query: { redirect: current.fullPath } })
    }
  })

  // 组件渲染期异常也统一暴露出来
  app.config.errorHandler = (err, instance, info) => {
    reportBootError(new Error(`${info}: ${err && err.message ? err.message : err}`))
  }

  app.mount('#app')

  if (typeof window !== 'undefined' && window.__clearBootFallback) {
    window.__clearBootFallback()
  }
} catch (err) {
  reportBootError(err)
}
