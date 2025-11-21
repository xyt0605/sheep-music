<template>
  <div class="comment-list">
    <div class="comment-header">
      <h3>评论 <span class="count">({{ total }})</span></h3>
      <div class="sort-tabs">
        <span 
          class="sort-item" 
          :class="{ active: sortType === 'hot' }"
          @click="changeSort('hot')"
        >最热</span>
        <span class="divider">|</span>
        <span 
          class="sort-item" 
          :class="{ active: sortType === 'latest' }"
          @click="changeSort('latest')"
        >最新</span>
      </div>
    </div>
    
    <!-- 发表评论 -->
    <CommentInput
      :song-id="songId"
      :show-rating="showRating"
      placeholder="说点什么..."
      @success="handleCommentSuccess"
    />
    
    <!-- 评论列表 -->
    <div v-loading="loading" class="comments-wrapper">
      <div v-if="comments.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无评论，快来发表第一条评论吧~" :image-size="120" />
      </div>
      
      <div class="comments-container" v-else>
        <CommentItem
          v-for="comment in comments"
          :key="comment.id"
          :comment="comment"
          @like="handleLike"
          @delete="handleDelete"
          @reply-success="handleReplySuccess"
        />
      </div>
    </div>
    
    <!-- 加载更多 -->
    <div v-if="hasMore && comments.length > 0" class="load-more">
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
const sortType = ref('hot') // 默认最热
const page = ref(0)
const size = ref(20)
const total = ref(0)
const hasMore = ref(true)

// 切换排序
const changeSort = (type) => {
  if (sortType.value === type) return
  sortType.value = type
  loadComments(true)
}

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

// 评论成功
const handleCommentSuccess = (comment) => {
  if (!comment) return
  // 去重检查
  const exists = comments.value.some(c => c.id === comment.id)
  if (!exists) {
    comments.value.unshift(comment)
    total.value++
  }
}

// 点赞
const handleLike = (commentId, liked) => {
  // 状态已在 Item 内更新
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

watch(() => props.songId, (newId, oldId) => {
  if (!newId || newId === oldId) return
  loadComments(true)
})
</script>

<style scoped>
.comment-list {
  padding: 8px 4px;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.comment-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.count {
  font-size: 14px;
  color: #909399;
  font-weight: normal;
  margin-left: 4px;
}

.sort-tabs {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #909399;
}

.sort-item {
  cursor: pointer;
  padding: 4px 8px;
  transition: all 0.2s;
}

.sort-item:hover {
  color: #606266;
}

.sort-item.active {
  color: #303133;
  font-weight: 600;
}

.divider {
  margin: 0 4px;
  color: #e4e7ed;
  font-size: 12px;
}

.comments-wrapper {
  min-height: 200px;
}

.empty-state {
  padding: 60px 0;
  display: flex;
  justify-content: center;
}

.load-more {
  text-align: center;
  margin-top: 30px;
  margin-bottom: 20px;
}
</style>



