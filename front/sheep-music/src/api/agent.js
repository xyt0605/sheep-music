const BASE = (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE_URL)
  || (typeof process !== 'undefined' && process.env?.VUE_APP_API_BASE_URL)
  || '/api'

import request from '@/utils/request'

const getToken = () => localStorage.getItem('token')

/**
 * 小屋 DJ：fetch + ReadableStream 解析 SSE（EventSource 无法携带 JWT）
 * @param {string} query 用户需求
 * @param {string} sessionId 会话 ID（同一抽屉生命周期复用，服务端据此维持多轮记忆）
 * @param {Object} handlers onEvent(event, data) 事件回调
 * @returns {Promise<void>} 流结束（done/error/断开）后 resolve
 */
export async function streamDj(query, sessionId, { onEvent } = {}) {
  const res = await fetch(BASE + '/agent/dj/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getToken()}`
    },
    body: JSON.stringify({ query, sessionId })
  })
  if (!res.ok || !res.body) {
    // 401 等由拦截器语义约定：这里抛给调用方展示
    let message = `请求失败（HTTP ${res.status}）`
    if (res.status === 401) message = '登录已过期，请重新登录'
    throw new Error(message)
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  for (;;) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    // SSE 事件以空行分隔
    let sep
    while ((sep = buffer.indexOf('\n\n')) >= 0) {
      const chunk = buffer.slice(0, sep)
      buffer = buffer.slice(sep + 2)
      let event = 'message'
      const dataLines = []
      for (const line of chunk.split('\n')) {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
      }
      if (!dataLines.length) continue
      let data = null
      try { data = JSON.parse(dataLines.join('\n')) } catch (e) { data = { raw: dataLines.join('\n') } }
      onEvent?.(event, data)
      if (event === 'done' || event === 'error') return
    }
  }
}

// ===== AI 连接配置（P3 BYOK 可视化配置板块） =====
export const getAiConfig = () => request({ url: '/agent/config', method: 'get' })
export const saveAiConfig = (data) => request({ url: '/agent/config', method: 'post', data })
export const clearAiConfig = () => request({ url: '/agent/config', method: 'delete' })
export const testAiConfig = (data) => request({ url: '/agent/config/test', method: 'post', data, timeout: 60000 })
