import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import {
  login as loginApi,
  register as registerApi,
  logout as logoutApi,
  getProfile,
  getCartCount
} from '@/api/index'
import { getToken, getCurrentUser, clearAuth, setAuth } from '@/api/request'

/**
 * 用户状态: 登录态 / 用户资料 / 购物车角标
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    user: getCurrentUser(),
    profile: getCurrentUser(),
    cartCount: 0,
    profileError: false
  }),

  getters: {
    isLogin: (state) => !!state.token,
    nickname: (state) => state.user?.nickname || state.user?.username || '用户',
    avatar: (state) => state.user?.avatar || '',
    role: (state) => state.user?.role || '',
    userId: (state) => state.user?.id || null
  },

  actions: {
    /** 登录 */
    async login(payload) {
      const data = await loginApi({ ...payload, role: payload.role || 'USER' })
      this.token = data.token
      this.user = data
      this.profile = data
      this.profileError = false
      await this.refreshCartCount()
      return data
    },

    /** 注册 */
    async register(payload) {
      return registerApi({ ...payload, role: 'USER' })
    },

    /** 退出登录 */
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 退出接口失败不阻塞本地清理
      }
      this.clear()
    },

    /** 本地清理登录态 */
    clear() {
      clearAuth()
      this.token = ''
      this.user = {}
      this.profile = {}
      this.cartCount = 0
    },

    /**
     * 拉取最新资料并同步到本地缓存。
     * 失败时保留登录时缓存的资料, 避免打断用户浏览。
     */
    async loadProfile(silent = true) {
      if (!this.isLogin) return null
      try {
        const data = await getProfile()
        this.profile = data
        this.profileError = false
        // 资料接口返回体不含 token, 这里合并保留 token 后再落盘
        this.user = { ...this.user, ...data }
        setAuth({ ...this.user, token: this.token })
        return data
      } catch (e) {
        this.profileError = true
        if (!silent) {
          ElMessage.warning('资料同步失败, 当前展示本地缓存的账号信息')
        }
        return null
      }
    },

    /** 资料更新成功后同步本地缓存 */
    patchProfile(partial) {
      this.profile = { ...this.profile, ...partial }
      this.user = { ...this.user, ...partial }
      setAuth({ ...this.user, token: this.token })
    },

    /** 刷新购物车角标 */
    async refreshCartCount() {
      if (!this.isLogin) {
        this.cartCount = 0
        return 0
      }
      try {
        const count = await getCartCount()
        this.cartCount = Number(count) || 0
      } catch (e) {
        this.cartCount = 0
      }
      return this.cartCount
    }
  }
})
