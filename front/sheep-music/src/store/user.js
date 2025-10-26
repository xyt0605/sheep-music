import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')  // 从 localStorage 恢复
  }),

  getters: {
    isLogin: (state) => !!state.token,
    isAdmin: (state) => state.userInfo?.role === 'admin'
  },

  actions: {
    setToken (token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    setUserInfo (userInfo) {
      this.userInfo = userInfo
      // 保存到 localStorage（JSON 序列化）
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },

    logout () {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')  // 清除用户信息
    }
  }
})
