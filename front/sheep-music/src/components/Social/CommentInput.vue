<template>
  <div class="comment-input">
    <div class="input-header">
      <el-avatar :src="userStore.userInfo?.avatar" :size="40">
        <el-icon><User /></el-icon>
      </el-avatar>
      <div class="input-box">
        <el-input
          v-model="content"
          type="textarea"
          :rows="3"
          :placeholder="placeholder"
          maxlength="500"
          show-word-limit
          resize="none"
          ref="inputRef"
        />
        <div class="input-actions">
          <div class="rating-section" v-if="showRating">
            <span class="rating-label">评分：</span>
            <el-rate
              v-model="rating"
              :max="5"
              allow-half
              show-score
              text-color="#ff9900"
            />
          </div>
          <div class="actions-right">
            <el-popover placement="top" :width="340" trigger="click">
              <template #reference>
                <el-button text>
                  <el-icon><User /></el-icon> 表情
                </el-button>
              </template>
              <EmojiPicker @pick="insertEmoji" />
            </el-popover>
            <el-button
              type="primary"
              :loading="loading"
              @click="handleSubmit"
              :disabled="loading || !content.trim()"
            >
              发表评论
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
import { User } from '@element-plus/icons-vue'
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
    default: '说点什么...'
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

const handleSubmit = async () => {
  // 防止重复提交
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
      // 使用防抖提示，避免父组件或其他逻辑重复弹窗
      message.success('评论成功')
      content.value = ''
      rating.value = 0
      
      // 触发成功事件
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
.comment-input {
  margin-bottom: 20px;
}

.input-header {
  display: flex;
  gap: 12px;
}

.input-box {
  flex: 1;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.actions-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-label {
  font-size: 14px;
  color: var(--text-secondary, #666);
}
</style>

