import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

// 解析后端BaseURL，复用与request.js一致的逻辑
const API_BASE = (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE_URL)
  || (typeof process !== 'undefined' && process.env?.VUE_APP_API_BASE_URL)
  || '/api'

// STOMP客户端单例
class WSClient {
  constructor() {
    this.client = null
    this.connected = false
    this.userId = null
    this.token = null
    this.chatHandlers = []
    this.notifyHandlers = []
  }

  connect({ userId, token }) {
    if (this.connected && this.userId === userId) return
    this.userId = userId
    this.token = token

    // 使用SockJS端点，确保开发环境通过代理到后端
    const endpoint = `${API_BASE}/ws-chat`

    this.client = new Client({
      // 优先使用 WebSocket，其次使用 XHR 轮询，避免部分环境下 xhr_streaming 500
      webSocketFactory: () => new SockJS(endpoint, null, { transports: ['websocket', 'xhr-polling'] }),
      // 携带认证头，后端在@MessageMapping中读取Authorization
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug: () => {},
      reconnectDelay: 3000,
    })

    this.client.onConnect = () => {
      this.connected = true
      // 订阅用户专属主题
      const chatTopic = `/topic/user.${this.userId}.chat`
      const notifyTopic = `/topic/user.${this.userId}.notifications`

      this.client.subscribe(chatTopic, (frame) => {
        try {
          const msg = JSON.parse(frame.body)
          this.chatHandlers.forEach(h => h(msg))
        } catch (_) {}
      })

      this.client.subscribe(notifyTopic, (frame) => {
        try {
          const data = JSON.parse(frame.body)
          this.notifyHandlers.forEach(h => h(data))
        } catch (_) {}
      })
    }

    this.client.onStompError = () => {
      // 保持静默，自动重连
    }

    this.client.onWebSocketClose = () => {
      this.connected = false
    }

    this.client.activate()
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate()
      this.client = null
      this.connected = false
    }
  }

  // 发送聊天消息到 /app/chat/send
  sendChatMessage(payload) {
    if (!this.client || !this.connected) return
    const headers = this.token ? { Authorization: `Bearer ${this.token}` } : {}
    this.client.publish({ destination: '/app/chat/send', headers, body: JSON.stringify(payload) })
  }

  // 注册处理器
  onChatMessage(handler) {
    this.chatHandlers.push(handler)
    return () => {
      this.chatHandlers = this.chatHandlers.filter(h => h !== handler)
    }
  }

  onNotification(handler) {
    this.notifyHandlers.push(handler)
    return () => {
      this.notifyHandlers = this.notifyHandlers.filter(h => h !== handler)
    }
  }
}

const wsClient = new WSClient()
export default wsClient