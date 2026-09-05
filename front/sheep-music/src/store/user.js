import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    // localStorage 内容被写坏（如存了 "undefined"）时不应让整个应用白屏
    userInfo: (() => {
      try {
        return JSON.parse(localStorage.getItem('userInfo') || 'null')
      } catch (_) {
        localStorage.removeItem('userInfo')
        return null
      }
    })()
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
