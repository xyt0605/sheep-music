import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import '@/styles/responsive.css' // 导入响应式样式
import '@/styles/theme.css' // 导入主题样式
import '@/styles/mobile-fix.css' // 导入移动端修复样式
import '@/styles/studio.css' // UI v3：全站音乐编辑台视觉覆盖
import { useTheme } from '@/composables/useTheme'
import 'emoji-picker-element' // 注册 emoji-picker-element Web Component

// 初始化主题
const { initTheme } = useTheme()
initTheme()

// 忽略 ResizeObserver 循环错误（这是 Element Plus 表格的已知问题，不影响功能）
const resizeObserverErrorHandler = (e) => {
  if (e.message === 'ResizeObserver loop completed with undelivered notifications.') {
    const resizeObserverErr = e
    resizeObserverErr.stopImmediatePropagation()
    return false
  }
  return true
}

window.addEventListener('error', resizeObserverErrorHandler)

const app = createApp(App)
const pinia = createPinia()

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(ElementPlus)
app.use(router)
app.mount('#app')

// Service Worker 只在生产构建里注册。
// sw.js 用的是「缓存优先且永不回源」策略，开发模式下它会把 Vite 的模块请求
// （/src/xxx.vue?vue&type=style&scoped=xxx&lang.css 等）永久缓存，改完代码刷新拿到的
// 仍是旧版本，还会出现组件 JS 与其 scoped CSS 来自不同版本、data-v 哈希对不上、
// 整个组件样式失效的情况。所以 dev 下反过来主动注销并清空缓存，让已被污染的浏览器自愈。
if ('serviceWorker' in navigator) {
  if (import.meta.env.PROD) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/sw.js')
        .then(registration => {
          console.log('SW registered:', registration)
        })
        .catch(error => {
          console.log('SW registration failed:', error)
        })
    })
  } else {
    const dropServiceWorker = async () => {
      const registrations = await navigator.serviceWorker.getRegistrations()
      await Promise.all(registrations.map(registration => registration.unregister()))
      if (window.caches) {
        const keys = await caches.keys()
        await Promise.all(keys.map(key => caches.delete(key)))
      }
      // 当前页面仍被旧 SW 控制时，刷新一次才能拿到干净的模块（注销后不会再次触发）
      if (navigator.serviceWorker.controller) {
        console.warn('[dev] 已注销 Service Worker 并清空缓存，正在重新加载...')
        window.location.reload()
      }
    }
    dropServiceWorker()
  }
}
