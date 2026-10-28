import { defineStore } from 'pinia'
import { getCategories } from '@/api/index'

/**
 * 轻量分类缓存: 首页悬浮分类导航 / 搜索页筛选 / 详情页面包屑共用
 */
export const useCategoryStore = defineStore('category', {
  state: () => ({
    categories: [],
    loaded: false,
    loading: false
  }),

  actions: {
    async load(force = false) {
      if (this.loaded && !force) return this.categories
      if (this.loading) return this.categories
      this.loading = true
      try {
        const data = await getCategories()
        this.categories = Array.isArray(data) ? data : []
        this.loaded = true
      } catch (e) {
        this.categories = []
      } finally {
        this.loading = false
      }
      return this.categories
    }
  }
})
