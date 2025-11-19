import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import '@/styles/responsive.css' // 导入响应式样式
import '@/styles/theme.css' // 导入主题样式
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
