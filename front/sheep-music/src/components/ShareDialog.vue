<template>
  <el-dialog 
    v-model="visible" 
    :title="dialogTitle" 
    width="520px"
    @close="handleClose"
    append-to-body
  >
    <el-form :model="form" label-width="80px">
      <el-form-item label="分享内容">
        <div class="share-preview">
          <el-image 
            v-if="shareData?.cover" 
            :src="shareData.cover" 
            fit="cover"
            class="preview-cover"
          >
            <template #error>
              <div class="image-error">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-image>
          <div class="preview-info">
            <div class="preview-title">{{ shareData?.name || '未知' }}</div>
            <div class="preview-subtitle">{{ shareData?.subtitle || '' }}</div>
          </div>
        </div>
      </el-form-item>
      
      <el-form-item label="描述">
        <el-input 
          v-model="form.description" 
          type="textarea" 
          :rows="4" 
          placeholder="给你的分享写点描述吧~" 
          maxlength="500" 
          show-word-limit 
          ref="descriptionInputRef"
        />
        <div style="margin-top: 8px; text-align: right;">
          <el-popover placement="top" :width="340" trigger="click">
            <template #reference>
              <el-button text size="small">
                <el-icon><ChatDotRound /></el-icon>
                表情
              </el-button>
            </template>
            <EmojiPicker @pick="insertEmoji" />
          </el-popover>
        </div>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleShare">
        分享
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture, ChatDotRound } from '@element-plus/icons-vue'
import { sharePlaylist, shareSong } from '@/api/share'
import EmojiPicker from './Social/EmojiPicker.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  type: {
    type: String,
    required: true,
    validator: (value) => ['playlist', 'song'].includes(value)
  },
  shareData: {
    type: Object,
    default: () => ({
      id: null,
      name: '',
      cover: '',
      subtitle: ''
    })
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const dialogTitle = computed(() => {
  return props.type === 'playlist' ? '分享歌单' : '分享歌曲'
})

const form = ref({
  description: ''
})

const loading = ref(false)
const descriptionInputRef = ref(null)

// 插入表情
const insertEmoji = (emoji) => {
  const input = descriptionInputRef.value
  if (input && input.textarea) {
    const textarea = input.textarea
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const text = form.value.description
    
    form.value.description = text.substring(0, start) + emoji + text.substring(end)
    
    nextTick(() => {
      textarea.focus()
      textarea.setSelectionRange(start + emoji.length, start + emoji.length)
    })
  } else {
    form.value.description += emoji
  }
}

// 处理分享
const handleShare = async () => {
  if (!props.shareData?.id) {
    ElMessage.error('分享数据无效')
    return
  }
  
  loading.value = true
  try {
    const data = {
      type: props.type,
      description: form.value.description || ''
    }
    
    if (props.type === 'playlist') {
      data.playlistId = props.shareData.id
      await sharePlaylist(data)
    } else if (props.type === 'song') {
      data.songId = props.shareData.id
      await shareSong(data)
    }
    
    ElMessage.success('分享成功')
    emit('success')
    handleClose()
  } catch (error) {
    console.error('分享失败:', error)
    ElMessage.error(error?.response?.data?.message || error.message || '分享失败')
  } finally {
    loading.value = false
  }
}

// 关闭弹窗
const handleClose = () => {
  form.value.description = ''
  visible.value = false
}

// 监听弹窗打开，自动聚焦
watch(visible, (val) => {
  if (val) {
    nextTick(() => {
      descriptionInputRef.value?.focus()
    })
  }
})
</script>

<style scoped>
.share-preview {
  display: flex;
  align-items: center;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.preview-cover {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  flex-shrink: 0;
  margin-right: 12px;
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
  font-size: 24px;
}

.preview-info {
  flex: 1;
  min-width: 0;
}

.preview-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-subtitle {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
