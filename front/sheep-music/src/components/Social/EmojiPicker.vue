<template>
  <div class="emoji-picker-wrap">
    <!-- 搜索框 -->
    <div class="emoji-search">
      <input
        v-model="searchQuery"
        type="text"
        class="search-input"
        placeholder="搜索表情..."
        @keydown.enter="pickFirstResult"
      />
    </div>

    <!-- emoji-picker-element Web Component -->
    <div ref="pickerContainer" class="picker-container">
      <emoji-picker
        ref="pickerRef"
        :dataSource="dataSource"
        :skinToneEmoji="skinToneEmoji"
        :categories="categories"
        @emoji-click="handleEmojiPick"
      />
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
import { ref, onMounted, watch, computed } from 'vue'
import 'emoji-picker-element'

const props = defineProps({
  visible: { type: Boolean, default: true }
})

const emit = defineEmits(['pick'])

const pickerRef = ref(null)
const pickerContainer = ref(null)
const searchQuery = ref('')
const recentEmojis = ref([])

// emoji-picker-element 配置
const dataSource = 'https://cdn.jsdelivr.net/npm/emoji-picker-element-data@^1/en/cldr/data.json'
const skinToneEmoji = '🖐️'
const categories = [
  'emoji', 'people', 'nature', 'food', 'place', 'activity', 'object', 'symbols', 'flags'
]

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
  
  // 只保留最近 20 个
  if (recentEmojis.value.length > 20) {
    recentEmojis.value.pop()
  }
  
  // 保存到 localStorage
  try {
    localStorage.setItem('recentEmojis', JSON.stringify(recentEmojis.value))
  } catch (e) {
    console.warn('Failed to save recent emojis:', e)
  }
}

// 搜索回调
const pickFirstResult = () => {
  if (searchQuery.value.trim()) {
    // emoji-picker-element 会自动过滤，这里只需要触发第一个结果
    const firstButton = pickerContainer.value?.querySelector('button[role="tab"]')
    if (firstButton) {
      firstButton.click()
    }
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

// 监听搜索查询变化
watch(searchQuery, (newQuery) => {
  if (pickerRef.value) {
    // 更新搜索过滤
    const input = pickerContainer.value?.querySelector('input[type="search"]')
    if (input) {
      input.value = newQuery
      // 触发搜索事件
      const event = new Event('input', { bubbles: true })
      input.dispatchEvent(event)
    }
  }
})
</script>

<style scoped>
.emoji-picker-wrap {
  background: var(--card-bg);
  border: 1px solid var(--border-color-light);
  border-radius: 12px;
  box-shadow: var(--shadow-lg);
  padding: 0;
  max-width: 360px;
  overflow: hidden;
}

/* 搜索框 */
.emoji-search {
  padding: 8px;
  border-bottom: 1px solid var(--border-color-light);
}

.search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  font-size: 14px;
  color: var(--text-primary);
  background: var(--input-bg, #ffffff);
  transition: border-color 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: var(--primary-color, #409eff);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.search-input::placeholder {
  color: var(--text-secondary, #999);
}

/* emoji-picker-element 容器 */
.picker-container {
  width: 100%;
  min-height: 350px;
}

/* 深度样式 - emoji-picker-element Web Component */
:deep(emoji-picker) {
  --border-color: var(--border-color-light, #e0e0e0);
  --background: var(--card-bg, #ffffff);
  --text-color: var(--text-primary, #333);
  --category-button-color: var(--text-secondary, #999);
  --category-button-active-color: var(--primary-color, #409eff);
  --outline-color: var(--primary-color, #409eff);
}

/* 最近使用 */
.recent-emojis {
  padding: 8px;
  border-top: 1px solid var(--border-color-light);
}

.recent-label {
  font-size: 12px;
  color: var(--text-secondary, #999);
  margin-bottom: 6px;
  padding: 0 4px;
  font-weight: 500;
}

.recent-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
}

.recent-emoji {
  width: 100%;
  aspect-ratio: 1;
  padding: 4px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.recent-emoji:hover {
  background-color: var(--hover-bg, #f5f5f5);
}

.recent-emoji:active {
  background-color: var(--active-bg, #e0e0e0);
}

/* 响应式 */
@media (max-width: 480px) {
  .emoji-picker-wrap {
    max-width: 100%;
  }

  .picker-container {
    min-height: 280px;
  }

  .recent-grid {
    grid-template-columns: repeat(6, 1fr);
  }
}
</style>

