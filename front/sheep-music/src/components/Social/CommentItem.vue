<template>
  <div class="comment-item" :class="{ 'is-reply': isReply }">
    <el-avatar :src="comment.userAvatar || comment.user?.avatar" :size="isReply ? 32 : 40">
      <el-icon><User /></el-icon>
    </el-avatar>
    
    <div class="comment-content">
      <div class="comment-header">
        <span class="username">{{ comment.username || comment.user?.nickname || '匿名用户' }}</span>
        <div class="comment-meta">
          <span class="time">{{ formatTime(comment.createTime) }}</span>
          <el-rate
            v-if="comment.rating"
            :model-value="comment.rating"
            :max="5"
            disabled
            show-score
            text-color="#ff9900"
            size="small"
            style="margin-left: 8px;"
          />
        </div>
      </div>
      
      <div class="comment-text">{{ comment.content }}</div>
      
      <div class="comment-actions">
        <el-button
          text
          type="primary"
          size="small"
          @click="handleLike"
          :loading="likeLoading"
        >
          <el-icon><Star /></el-icon>
          <span>{{ comment.likeCount || 0 }}</span>
        </el-button>
        
        <el-button
          v-if="!isReply"
          text
          type="primary"
          size="small"
          @click="handleReply"
        >
          <el-icon><ChatLineRound /></el-icon>
          回复
        </el-button>
        
        <el-button
          v-if="canDelete"
          text
          type="danger"
          size="small"
          @click="handleDelete"
          :loading="deleteLoading"
        >
          删除
        </el-button>
      </div>
      
      <!-- 回复列表 -->
      <div v-if="replies.length > 0" class="replies">
        <CommentItem
          v-for="reply in replies"
          :key="reply.id"
          :comment="reply"
          :is-reply="true"
          @like="handleReplyLike"
          @delete="handleReplyDelete"
        />
      </div>
      
      <!-- 回复输入框 -->
      <CommentInput
        v-if="showReplyInput"
        :song-id="comment.songId"
        :parent-id="comment.id"
        placeholder="回复这条评论..."
        @success="handleReplySuccess"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Star, ChatLineRound } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { toggleCommentLike, deleteComment, getCommentReplies } from '@/api/comment'
import CommentInput from './CommentInput.vue'

const props = defineProps({
  comment: {
    type: Object,
    required: true
  },
  isReply: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['like', 'delete', 'reply-success'])

const userStore = useUserStore()
const replies = ref([])
const showReplyInput = ref(false)
const likeLoading = ref(false)
const deleteLoading = ref(false)

const canDelete = computed(() => {
  return userStore.userInfo?.id === props.comment.userId
})

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  
  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 加载回复
const loadReplies = async () => {
  try {
    const res = await getCommentReplies(props.comment.id)
    if (res.code === 200) {
      replies.value = res.data || []
    }
  } catch (error) {
    console.error('加载回复失败:', error)
  }
}

// 点赞/取消点赞
const handleLike = async () => {
  likeLoading.value = true
  try {
    const res = await toggleCommentLike(props.comment.id)
    if (res.code === 200) {
      props.comment.likeCount = res.data 
        ? (props.comment.likeCount || 0) + 1 
        : Math.max(0, (props.comment.likeCount || 0) - 1)
      
      emit('like', props.comment.id, res.data)
    }
  } catch (error) {
    console.error('点赞失败:', error)
  } finally {
    likeLoading.value = false
  }
}

// 回复
const handleReply = () => {
  showReplyInput.value = !showReplyInput.value
  if (showReplyInput.value && replies.value.length === 0) {
    loadReplies()
  }
}

// 回复成功
const handleReplySuccess = (reply) => {
  if (!reply) return
  const exists = replies.value.find(r => r.id === reply.id)
  if (exists) {
    // 已存在则更新内容与时间，避免重复展示
    exists.content = reply.content
    exists.createTime = reply.createTime || exists.createTime
  } else {
    replies.value.push(reply)
    props.comment.commentCount = (props.comment.commentCount || 0) + 1
  }
  showReplyInput.value = false
  emit('reply-success', reply)
}

// 删除评论
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    deleteLoading.value = true
    const res = await deleteComment(props.comment.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      emit('delete', props.comment.id)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  } finally {
    deleteLoading.value = false
  }
}

// 处理回复点赞
const handleReplyLike = (commentId, liked) => {
  const reply = replies.value.find(r => r.id === commentId)
  if (reply) {
    reply.likeCount = liked 
      ? (reply.likeCount || 0) + 1 
      : Math.max(0, (reply.likeCount || 0) - 1)
  }
}

// 处理回复删除
const handleReplyDelete = (commentId) => {
  replies.value = replies.value.filter(r => r.id !== commentId)
}

onMounted(() => {
  if (!props.isReply && props.comment.parentId === null) {
    loadReplies()
  }
})
</script>

<style scoped>
.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color, #eee);
}

.comment-item.is-reply {
  padding: 12px 0;
  margin-left: 52px;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.username {
  font-weight: 500;
  color: var(--text-primary, #333);
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-tertiary, #999);
}

.comment-text {
  color: var(--text-secondary, #666);
  line-height: 1.6;
  margin-bottom: 8px;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}

.replies {
  margin-top: 16px;
  padding-left: 12px;
  border-left: 2px solid var(--border-color, #eee);
}
</style>

