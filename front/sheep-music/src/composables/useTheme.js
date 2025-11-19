import { ref, watch, onMounted } from 'vue'

const THEME_KEY = 'sheep-music-theme'

// 全局主题状态
const isDark = ref(false)

export function useTheme() {
  // 初始化主题
  const initTheme = () => {
    // 从 localStorage 读取或使用系统偏好
    const savedTheme = localStorage.getItem(THEME_KEY)
    
    if (savedTheme) {
      isDark.value = savedTheme === 'dark'
    } else {
      // 检测系统主题
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      isDark.value = prefersDark
    }
    
    applyTheme()
  }

  // 应用主题
  const applyTheme = () => {
    if (isDark.value) {
      document.documentElement.classList.add('dark')
      document.documentElement.setAttribute('data-theme', 'dark')
    } else {
      document.documentElement.classList.remove('dark')
      document.documentElement.setAttribute('data-theme', 'light')
    }
  }

  // 切换主题
  const toggleTheme = () => {
    isDark.value = !isDark.value
    localStorage.setItem(THEME_KEY, isDark.value ? 'dark' : 'light')
    applyTheme()
  }

  // 设置主题
  const setTheme = (dark) => {
    isDark.value = dark
    localStorage.setItem(THEME_KEY, dark ? 'dark' : 'light')
    applyTheme()
  }

  // 监听主题变化
  watch(isDark, () => {
    applyTheme()
  })

  return {
    isDark,
    toggleTheme,
    setTheme,
    initTheme
  }
}

