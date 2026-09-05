<template>
  <div class="emoji-picker-wrap">
    <!-- emoji-picker-element Web Component -->
    <div class="picker-container">
      <emoji-picker
        ref="pickerRef"
        :skin-tone-emoji="skinToneEmoji"
        class="custom-picker"
        @emoji-click="handleEmojiPick"
      />
    </div>

    <!-- 最近使用 -->
    <div
      v-if="recentEmojis.length"
      class="recent-emojis"
    >
      <div class="recent-label">
        最近使用
      </div>
      <div class="recent-grid">
        <button
          v-for="emoji in recentEmojis"
          :key="emoji"
          class="recent-emoji"
          :title="emoji"
          @click="pickEmoji(emoji)"
        >
          {{ emoji }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
// 本地 import 才会注册 emoji-picker 自定义元素；
// 不再额外引入 emoji-picker-element-data 的 CLDR 数据集（426KB），
// 组件内置的默认数据库功能相同，避免两份 emoji 数据同时打进产物。
import 'emoji-picker-element'

const emit = defineEmits(['pick'])

const pickerRef = ref(null)
const recentEmojis = ref([])

// emoji-picker-element 配置
const skinToneEmoji = '🖐️'

// 处理 emoji 选择
const handleEmojiPick = (event) => {
  const e = event.detail?.emoji
  const ch = e?.unicode || e?.emoji || event.detail?.unicode || ''
  if (ch) {
    pickEmoji(ch)
  }
}

// 选择 emoji 的核心逻辑
const pickEmoji = (ch) => {
  emit('pick', ch)
  
  // 更新最近使用
  const idx = recentEmojis.value.indexOf(ch)
  if (idx > -1) {
    recentEmojis.value.splice(idx, 1)
  }
  recentEmojis.value.unshift(ch)
  
  // 只保留最近 8 个
  if (recentEmojis.value.length > 8) {
    recentEmojis.value.pop()
  }
  
  // 保存到 localStorage
  try {
    localStorage.setItem('recentEmojis', JSON.stringify(recentEmojis.value))
  } catch (e) {
    console.warn('Failed to save recent emojis:', e)
  }
}

// 初始化最近使用
onMounted(() => {
  try {
    const saved = localStorage.getItem('recentEmojis')
    if (saved) {
      recentEmojis.value = JSON.parse(saved)
    }
  } catch (e) {
    console.warn('Failed to load recent emojis:', e)
  }
})
</script>

<style scoped>
.emoji-picker-wrap {
  background: var(--card-bg, #fff);
  border: 1px solid var(--border-color-light, #eee);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* emoji-picker-element 容器 */
.picker-container {
  width: 100%;
  height: 320px;
}

.custom-picker {
  width: 100%;
  height: 100%;
  --border-radius: 0;
  --background: transparent;
}

/* 深度样式 - emoji-picker-element Web Component */
:deep(emoji-picker) {
  --border-color: var(--border-color-light, #e0e0e0);
  --background: var(--card-bg, #ffffff);
  --text-color: var(--text-primary, #333);
  --category-button-color: var(--text-secondary, #999);
  --category-button-active-color: var(--primary-color, #409eff);
  --outline-color: var(--primary-color, #409eff);
  --input-border-radius: 20px;
  --input-padding: 8px 16px;
  --indicator-color: var(--primary-color, #409eff);
  --button-hover-background: rgba(0,0,0,0.05);
}

/* 最近使用 */
.recent-emojis {
  padding: 10px 16px;
  border-top: 1px solid var(--border-color-light, #eee);
  background: var(--bg-secondary, #f9fafc);
}

.recent-label {
  font-size: 12px;
  color: var(--text-secondary, #909399);
  margin-bottom: 8px;
  font-weight: 500;
}

.recent-grid {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.recent-emoji {
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
}

.recent-emoji:hover {
  background-color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.recent-emoji:active {
  transform: scale(0.95);
}
</style>

