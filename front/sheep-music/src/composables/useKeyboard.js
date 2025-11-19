import { onMounted, onBeforeUnmount } from 'vue'

export function useKeyboard(handlers = {}) {
  const handleKeyDown = (event) => {
    // 如果正在输入文本，不触发快捷键
    if (event.target.tagName === 'INPUT' || 
        event.target.tagName === 'TEXTAREA' ||
        event.target.isContentEditable) {
      return
    }

    const key = event.key.toLowerCase()
    const ctrl = event.ctrlKey || event.metaKey
    const shift = event.shiftKey
    const alt = event.altKey

    // 生成快捷键字符串
    let shortcut = ''
    if (ctrl) shortcut += 'ctrl+'
    if (shift) shortcut += 'shift+'
    if (alt) shortcut += 'alt+'
    shortcut += key

    // 单个按键处理
    if (handlers[key]) {
      event.preventDefault()
      handlers[key](event)
    }

    // 组合键处理
    if (handlers[shortcut]) {
      event.preventDefault()
      handlers[shortcut](event)
    }
  }

  onMounted(() => {
    window.addEventListener('keydown', handleKeyDown)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeyDown)
  })

  return {
    handleKeyDown
  }
}

// 预定义的快捷键映射
export const SHORTCUTS = {
  PLAY_PAUSE: ' ', // 空格键
  NEXT: 'arrowright',
  PREV: 'arrowleft',
  VOLUME_UP: 'arrowup',
  VOLUME_DOWN: 'arrowdown',
  MUTE: 'm',
  SEARCH: 'ctrl+f',
  HOME: 'h',
  LIKE: 'l'
}

