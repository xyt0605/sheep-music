import { ElMessage, ElNotification } from 'element-plus'

/**
 * 优化的消息提示工具
 */

// 防抖消息队列
const messageQueue = new Map()
const DEBOUNCE_TIME = 1000

/**
 * 显示成功消息
 */
export function showSuccess(message, options = {}) {
  return showMessage({
    message,
    type: 'success',
    ...options
  })
}

/**
 * 显示错误消息
 */
export function showError(message, options = {}) {
  return showMessage({
    message,
    type: 'error',
    duration: 3000,
    ...options
  })
}

/**
 * 显示警告消息
 */
export function showWarning(message, options = {}) {
  return showMessage({
    message,
    type: 'warning',
    ...options
  })
}

/**
 * 显示信息消息
 */
export function showInfo(message, options = {}) {
  return showMessage({
    message,
    type: 'info',
    ...options
  })
}

/**
 * 显示加载消息
 */
export function showLoading(message = '加载中...', options = {}) {
  return ElMessage({
    message,
    type: 'info',
    duration: 0,
    ...options
  })
}

/**
 * 基础消息显示（带防抖）
 */
function showMessage(config) {
  const { message, debounce = true } = config
  
  if (debounce) {
    // 防止重复消息
    const key = `${config.type}-${message}`
    const now = Date.now()
    
    if (messageQueue.has(key)) {
      const lastTime = messageQueue.get(key)
      if (now - lastTime < DEBOUNCE_TIME) {
        return
      }
    }
    
    messageQueue.set(key, now)
    
    // 清理过期的消息记录
    setTimeout(() => {
      messageQueue.delete(key)
    }, DEBOUNCE_TIME)
  }
  
  return ElMessage({
    showClose: true,
    duration: 2500,
    ...config
  })
}

/**
 * 显示通知
 */
export function showNotification(config) {
  const { title = '', message = '', debounce = true } = config || {}
  if (debounce) {
    const key = `notify-${title}-${message}`
    const now = Date.now()
    if (messageQueue.has(key)) {
      const lastTime = messageQueue.get(key)
      if (now - lastTime < DEBOUNCE_TIME) {
        return
      }
    }
    messageQueue.set(key, now)
    setTimeout(() => messageQueue.delete(key), DEBOUNCE_TIME)
  }
  return ElNotification({
    showClose: true,
    duration: 4500,
    position: 'top-right',
    ...config
  })
}

/**
 * 显示成功通知
 */
export function notifySuccess(title, message, options = {}) {
  return showNotification({
    title,
    message,
    type: 'success',
    ...options
  })
}

/**
 * 显示错误通知
 */
export function notifyError(title, message, options = {}) {
  return showNotification({
    title,
    message,
    type: 'error',
    duration: 5000,
    ...options
  })
}

/**
 * 显示警告通知
 */
export function notifyWarning(title, message, options = {}) {
  return showNotification({
    title,
    message,
    type: 'warning',
    ...options
  })
}

/**
 * 显示信息通知
 */
export function notifyInfo(title, message, options = {}) {
  return showNotification({
    title,
    message,
    type: 'info',
    ...options
  })
}

/**
 * 关闭所有消息
 */
export function closeAllMessages() {
  ElMessage.closeAll()
}

/**
 * 显示操作确认
 */
export async function confirmAction(message, title = '确认操作', options = {}) {
  const { ElMessageBox } = await import('element-plus')
  return ElMessageBox.confirm(message, title, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    ...options
  })
}

// 默认导出
export default {
  success: showSuccess,
  error: showError,
  warning: showWarning,
  info: showInfo,
  loading: showLoading,
  notify: showNotification,
  notifySuccess,
  notifyError,
  notifyWarning,
  notifyInfo,
  closeAll: closeAllMessages,
  confirm: confirmAction
}

