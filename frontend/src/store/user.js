import { defineStore } from 'pinia'
import { authApi } from '../api'

/**
 * 登录用户状态:token 持久化到 localStorage,刷新页面自动恢复。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('ai_token') || '',
    user: JSON.parse(localStorage.getItem('ai_user') || 'null')
  }),

  getters: {
    isLogin: (state) => !!state.token,
    isAdmin: (state) => state.user && state.user.role === 'ADMIN',
    nickname: (state) => (state.user ? state.user.nickname || state.user.username : '')
  },

  actions: {
    async login(form) {
      const data = await authApi.login(form)
      this.token = data.token
      this.user = data.user
      localStorage.setItem('ai_token', data.token)
      localStorage.setItem('ai_user', JSON.stringify(data.user))
      return data
    },

    async fetchMe() {
      if (!this.token) return null
      const me = await authApi.me()
      this.user = me
      localStorage.setItem('ai_user', JSON.stringify(me))
      return me
    },

    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('ai_token')
      localStorage.removeItem('ai_user')
    }
  }
})
