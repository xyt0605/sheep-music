<template>
  <div class="emoji-picker-wrap">
    <!-- emoji-picker-element Web Component -->
    <div class="picker-container">
      <emoji-picker
        ref="pickerRef"
        :dataSource="dataSource"
        :skinToneEmoji="skinToneEmoji"
        class="custom-picker"
        @emoji-click="handleEmojiPick"
      ></emoji-picker>
    </div>

    <!-- 最近使用 -->
    <div v-if="recentEmojis.length" class="recent-emojis">
      <div class="recent-label">最近使用</div>
      <div class="recent-grid">
        <button
          v-for="emoji in recentEmojis"
          :key="emoji"
          class="recent-emoji"
          @click="pickEmoji(emoji)"
          :title="emoji"
        >
          {{ emoji }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import 'emoji-picker-element'

const emit = defineEmits(['pick'])

const pickerRef = ref(null)
const recentEmojis = ref([])

// emoji-picker-element 配置
const dataSource = 'https://cdn.jsdelivr.net/npm/emoji-picker-element-data@^1/en/cldr/data.json'
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
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
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
  --input-padding: 6px 12px;
}

/* 最近使用 */
.recent-emojis {
  padding: 8px 12px;
  border-top: 1px solid var(--border-color-light, #eee);
  background: var(--bg-secondary, #f9fafc);
}

.recent-label {
  font-size: 11px;
  color: var(--text-secondary, #999);
  margin-bottom: 4px;
}

.recent-grid {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.recent-emoji {
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.recent-emoji:hover {
  background-color: rgba(0,0,0,0.05);
  transform: scale(1.1);
}
</style>

