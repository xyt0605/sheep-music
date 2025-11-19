<template>
  <div class="comment-list">
    <div class="comment-header">
      <h3>评论 ({{ total }})</h3>
      <el-radio-group v-model="sortType" size="small" @change="handleSortChange">
        <el-radio-button value="latest">最新</el-radio-button>
        <el-radio-button value="hot">最热</el-radio-button>
      </el-radio-group>
    </div>
    
    <!-- 发表评论 -->
    <CommentInput
      :song-id="songId"
      :show-rating="showRating"
      placeholder="说点什么..."
      @success="handleCommentSuccess"
    />
    
    <!-- 评论列表 -->
    <div v-loading="loading" class="comments">
      <div v-if="comments.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无评论，快来发表第一条评论吧~" />
      </div>
      
      <CommentItem
        v-for="comment in comments"
        :key="comment.id"
        :comment="comment"
        @like="handleLike"
        @delete="handleDelete"
        @reply-success="handleReplySuccess"
      />
    </div>
    
    <!-- 加载更多 -->
    <div v-if="hasMore" class="load-more">
      <el-button
        text
        :loading="loading"
        @click="loadMore"
      >
        加载更多
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getSongComments } from '@/api/comment'
import CommentInput from './CommentInput.vue'
import CommentItem from './CommentItem.vue'

const props = defineProps({
  songId: {
    type: Number,
    required: true
  },
  showRating: {
    type: Boolean,
    default: false
  }
})

const comments = ref([])
const loading = ref(false)
const sortType = ref('latest')
const page = ref(0)
const size = ref(20)
const total = ref(0)
const hasMore = ref(true)

// 加载评论
const loadComments = async (reset = false) => {
  if (loading.value) return
  
  if (reset) {
    page.value = 0
    comments.value = []
    hasMore.value = true
  }
  
  loading.value = true
  try {
    const res = await getSongComments(props.songId, {
      page: page.value,
      size: size.value,
      sort: sortType.value
    })
    
    if (res.code === 200) {
      const data = res.data
      if (reset) {
        comments.value = data.content || []
      } else {
        comments.value.push(...(data.content || []))
      }
      
      total.value = data.totalElements || 0
      hasMore.value = !data.last
    }
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = () => {
  page.value++
  loadComments()
}

// 排序改变
const handleSortChange = () => {
  loadComments(true)
}

// 评论成功（去重保护）
const handleCommentSuccess = (comment) => {
  if (!comment) return
  const exists = comments.value.find(c => c.id === comment.id)
  if (exists) {
    // 已存在则更新内容与时间，避免重复展示
    exists.content = comment.content
    exists.createTime = comment.createTime || exists.createTime
  } else {
    comments.value.unshift(comment)
    total.value++
  }
}

// 点赞
const handleLike = (commentId, liked) => {
  const comment = comments.value.find(c => c.id === commentId)
  if (comment) {
    // 点赞状态已在CommentItem中更新
  }
}

// 删除评论
const handleDelete = (commentId) => {
  comments.value = comments.value.filter(c => c.id !== commentId)
  total.value = Math.max(0, total.value - 1)
}

// 回复成功
const handleReplySuccess = (reply) => {
  // 回复已添加到对应评论的replies中
}

onMounted(() => {
  loadComments(true)
})

// 监听歌曲切换，重置并重新加载该歌曲的评论
watch(() => props.songId, (newId, oldId) => {
  if (!newId || newId === oldId) return
  page.value = 0
  comments.value = []
  total.value = 0
  hasMore.value = true
  loadComments(true)
})
</script>

<style scoped>
.comment-list {
  padding: 20px 0;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.comment-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary, #333);
}

.comments {
  min-height: 200px;
}

.empty-state {
  padding: 40px 0;
}

.load-more {
  text-align: center;
  margin-top: 20px;
}
</style>

