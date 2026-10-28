import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { getToken, setToken, clearToken } from '@/api/request'

const USER_KEY = 'mall_admin_user'

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

/**
 * 用户 store: 保存登录态 / 角色 / 店铺信息
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: readStoredUser()
  }),

  getters: {
    isLogin: (state) => !!state.token && !!state.userInfo,
    role: (state) => (state.userInfo && state.userInfo.role) || '',
    isMerchant: (state) => (state.userInfo && state.userInfo.role) === 'MERCHANT',
    isAdmin: (state) => (state.userInfo && state.userInfo.role) === 'ADMIN',
    /** 顶部展示名: 商家显示店铺名, 管理员显示昵称 */
    displayName: (state) => {
      const u = state.userInfo
      if (!u) return ''
      if (u.role === 'MERCHANT') return u.storeName || u.nickname || u.username
      return u.nickname || u.username
    },
    avatar: (state) => {
      const u = state.userInfo
      if (!u) return ''
      return (u.role === 'MERCHANT' ? u.storeLogo : u.avatar) || u.avatar || u.storeLogo || ''
    },
    /** 该角色登录后的首页 */
    homePath: (state) => (state.userInfo && state.userInfo.role === 'ADMIN' ? '/admin/overview' : '/dashboard')
  },

  actions: {
    /** 登录: 入参 {username, password, role} */
    async login(payload) {
      const data = await authApi.login(payload)
      this.token = data.token
      this.userInfo = data
      setToken(data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(data))
      return data
    },

    /** 退出登录, 清空本地状态 */
    logout() {
      this.token = ''
      this.userInfo = null
      clearToken()
      localStorage.removeItem(USER_KEY)
    },

    /** 仅清 token (401 时调用) */
    resetAuth() {
      this.token = ''
      this.userInfo = null
      clearToken()
      localStorage.removeItem(USER_KEY)
    }
  }
})
