import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import 'element-plus/dist/index.css'
import '@/styles/main.css'

import App from './App.vue'
import router from './router'

/**
 * 把启动期异常显示到页面上, 避免只留一个白屏。
 * 具体渲染逻辑在 index.html 的兜底脚本里。
 */
function reportBootError(err) {
  // eslint-disable-next-line no-console
  console.error('[启动失败]', err)
  if (typeof window !== 'undefined' && window.__showBootError) {
    window.__showBootError(err)
  }
}

try {
  const app = createApp(App)

  // 全量注册 Element Plus 图标
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }

  app.use(createPinia())
  app.use(router)
  app.use(ElementPlus, { locale: zhCn })

  // 组件渲染期异常也统一暴露出来
  app.config.errorHandler = (err, instance, info) => {
    reportBootError(new Error(`${info}: ${err && err.message ? err.message : err}`))
  }

  app.mount('#app')

  // 挂载成功, 移除加载占位
  if (typeof window !== 'undefined' && window.__clearBootFallback) {
    window.__clearBootFallback()
  }
} catch (err) {
  reportBootError(err)
}
