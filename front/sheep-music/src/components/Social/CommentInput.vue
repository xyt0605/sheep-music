<template>
  <div class="comment-input-wrapper">
    <div class="input-main">
      <el-avatar :src="userStore.userInfo?.avatar" :size="40" class="user-avatar">
        {{ userStore.userInfo?.nickname?.charAt(0) }}
      </el-avatar>
      
      <div class="input-container" :class="{ active: isFocused || content.length > 0 }">
        <div class="textarea-wrapper">
          <el-input
            v-model="content"
            type="textarea"
            :rows="isFocused || content.length > 0 ? 3 : 1"
            :placeholder="placeholder"
            resize="none"
            maxlength="500"
            class="custom-textarea"
            @focus="handleFocus"
            @blur="handleBlur"
            ref="inputRef"
          />
        </div>
        
        <div class="input-footer" v-show="isFocused || content.length > 0">
          <div class="footer-left">
            <el-popover placement="top-start" :width="320" trigger="click" :teleported="false">
              <template #reference>
                <el-button link class="tool-btn">
                  <span class="emoji-icon">😃</span>
                  <span class="btn-text">表情</span>
                </el-button>
              </template>
              <EmojiPicker @pick="insertEmoji" />
            </el-popover>
            
            <div class="rating-wrap" v-if="showRating">
              <span class="rating-label">评分</span>
              <el-rate v-model="rating" size="small" allow-half />
            </div>
          </div>
          
          <div class="footer-right">
            <span class="word-count" :class="{ full: content.length >= 500 }">{{ content.length }}/500</span>
            <el-button 
              type="primary" 
              size="small" 
              round 
              :loading="loading" 
              @click="handleSubmit" 
              :disabled="!content.trim()"
              class="submit-btn"
            >
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import message from '@/utils/message'
import { useUserStore } from '@/store/user'
import { addComment } from '@/api/comment'
import EmojiPicker from '@/components/Social/EmojiPicker.vue'

const props = defineProps({
  songId: {
    type: Number,
    required: true
  },
  parentId: {
    type: Number,
    default: null
  },
  placeholder: {
    type: String,
    default: '发表评论...'
  },
  showRating: {
    type: Boolean,
    default: false
  },
  onSuccess: {
    type: Function,
    default: null
  }
})

const emit = defineEmits(['success'])

const userStore = useUserStore()
const content = ref('')
const rating = ref(0)
const loading = ref(false)
const inputRef = ref()
const isFocused = ref(false)

const handleFocus = () => {
  isFocused.value = true
}

const handleBlur = () => {
  // 延迟失焦，防止点击表情或发送按钮时 input 收起
  setTimeout(() => {
    // 只有当内容为空时才视为失焦收起
    if (!content.value) {
      isFocused.value = false
    }
  }, 200)
}

const handleSubmit = async () => {
  if (loading.value) return

  if (!content.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  loading.value = true
  try {
    const res = await addComment({
      songId: props.songId,
      content: content.value.trim(),
      rating: props.showRating && rating.value > 0 ? rating.value : null,
      parentId: props.parentId
    })

    if (res.code === 200) {
      message.success('评论成功')
      content.value = ''
      rating.value = 0
      isFocused.value = false // 发送成功后收起
      
      emit('success', res.data)
      if (props.onSuccess) {
        props.onSuccess(res.data)
      }
    }
  } catch (error) {
    console.error('发表评论失败:', error)
  } finally {
    loading.value = false
  }
}

const insertEmoji = (emoji) => {
  const el = inputRef.value?.textarea || inputRef.value?.$refs?.textarea
  if (!el) {
    content.value += emoji
    return
  }
  const start = el.selectionStart || 0
  const end = el.selectionEnd || 0
  const before = content.value.slice(0, start)
  const after = content.value.slice(end)
  content.value = before + emoji + after
  nextTick(() => {
    try {
      const pos = start + emoji.length
      el.focus()
      el.setSelectionRange(pos, pos)
    } catch (_) {}
  })
}
</script>

<style scoped>
.comment-input-wrapper {
  margin-bottom: 24px;
}

.input-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.user-avatar {
  flex-shrink: 0;
  border: 1px solid rgba(0,0,0,0.05);
}

.input-container {
  flex: 1;
  background: var(--bg-secondary, #f5f7fa);
  border: 1px solid transparent;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.input-container.active {
  background: #fff;
  border-color: var(--border-color, #dcdfe6);
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.textarea-wrapper {
  padding: 4px;
}

.custom-textarea :deep(.el-textarea__inner) {
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 8px 12px;
  font-size: 14px;
  line-height: 1.6;
}

.custom-textarea :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-top: 1px solid var(--border-color-lighter, #ebeef5);
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.tool-btn {
  padding: 0;
  height: auto;
  color: #606266;
  font-weight: normal;
}

.tool-btn:hover {
  color: var(--primary-color, #409eff);
}

.emoji-icon {
  font-size: 18px;
  margin-right: 4px;
}

.rating-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-label {
  font-size: 12px;
  color: #909399;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.word-count {
  font-size: 12px;
  color: #c0c4cc;
}

.word-count.full {
  color: #f56c6c;
}

.submit-btn {
  padding: 8px 20px;
  font-weight: 500;
}
</style>



