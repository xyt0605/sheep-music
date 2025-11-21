<template>
  <div class="moments-page">
    <div class="page-header">
      <h2>动态</h2>
      <el-button type="primary" @click="showPublishDialog = true">
        <el-icon>
          <Plus />
        </el-icon>
        发布动态
      </el-button>
    </div>

    <div v-loading="loading" class="moments-list">
      <div v-if="moments.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无动态" />
      </div>

      <div v-for="moment in moments" :key="moment.id" class="moment-card">
        <!-- 用户信息 -->
        <div class="moment-header">
          <el-avatar :src="moment.userAvatar || moment.user?.avatar" :size="45">
            <el-icon>
              <User />
            </el-icon>
          </el-avatar>
          <div class="header-info">
            <div class="user-name">{{ moment.username || moment.user?.nickname }}</div>
            <div class="moment-time">{{ formatTime(moment.createTime) }}</div>
          </div>
        </div>

        <!-- 动态内容 -->
        <div class="moment-content">
          <div v-if="moment.content" class="moment-text">
            {{ moment.content }}
          </div>

          <!-- 分享的歌曲 -->
          <div v-if="moment.type === 'song'" class="moment-share">
            <div class="share-item" @click="goToSong(moment.relatedId)">
              <el-image :src="moment.relatedCover" fit="cover" class="share-cover">
                <template #error>
                  <div class="image-error">
                    <el-icon>
                      <Picture />
                    </el-icon>
                  </div>
                </template>
              </el-image>
              <div class="share-info">
                <div class="share-title">{{ moment.relatedTitle }}</div>
                <div class="share-type">歌曲</div>
              </div>
            </div>
          </div>

          <!-- 分享的歌单 -->
          <div v-if="moment.type === 'playlist'" class="moment-share">
            <div class="share-item" @click="goToPlaylist(moment.relatedId)">
              <el-image :src="moment.relatedCover" fit="cover" class="share-cover">
                <template #error>
                  <div class="image-error">
                    <el-icon>
                      <Picture />
                    </el-icon>
                  </div>
                </template>
              </el-image>
              <div class="share-info">
                <div class="share-title">{{ moment.relatedTitle }}</div>
                <div class="share-type">歌单</div>
              </div>
            </div>
          </div>

          <!-- 图片 -->
          <div v-if="moment.images" class="moment-images">
            <el-image v-for="(img, index) in parseImages(moment.images)" :key="index" :src="img" fit="cover"
              :preview-src-list="parseImages(moment.images)" :initial-index="index" class="moment-image" />
          </div>
        </div>

        <!-- 操作栏 -->
        <div class="moment-actions">
          <el-button text type="primary" @click="handleLike(moment)" :loading="likeLoading === moment.id">
            <el-icon>
              <Star />
            </el-icon>
            <span>{{ moment.likeCount || 0 }}</span>
          </el-button>

          <el-button text type="primary" @click="handleComment(moment)">
            <el-icon>
              <ChatLineRound />
            </el-icon>
            <span>{{ moment.commentCount || 0 }}</span>
          </el-button>

          <el-button v-if="canDelete(moment)" text type="danger" @click="handleDelete(moment)"
            :loading="deleteLoading === moment.id">
            删除
          </el-button>
        </div>

        <!-- 内联评论区域 -->
        <div class="moment-comments-section">
          <div class="comment-input">
            <el-input
              v-model="getMomentState(moment.id).input"
              type="textarea"
              :rows="2"
              placeholder="写评论..."
              maxlength="500"
              show-word-limit
              :ref="el => setInlineInputRef(moment.id, el)"
            />
            <div class="comment-actions">
              <el-popover placement="top" :width="340" trigger="click">
                <template #reference>
                  <el-button text><el-icon><ChatLineRound /></el-icon> 表情</el-button>
                </template>
                <EmojiPicker @pick="e => insertInlineEmoji(moment.id, e)" />
              </el-popover>
              <el-button type="primary" :loading="getMomentState(moment.id).posting" @click="submitMomentComment(moment)">发表评论</el-button>
            </div>
          </div>

          <div v-loading="getMomentState(moment.id).loading" class="moment-comments">
            <div v-if="getMomentState(moment.id).items.length === 0 && !getMomentState(moment.id).loading" class="empty-state">
              <el-empty description="暂无评论" />
            </div>
            <div v-else class="comment-items">
              <div v-for="comment in getMomentState(moment.id).items" :key="comment.id" class="comment-item">
                <el-avatar :src="comment.userAvatar || comment.user?.avatar" :size="32">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <div class="comment-main">
                  <div class="comment-header">
                    <span class="comment-username">{{ comment.username || comment.user?.nickname || '匿名用户' }}</span>
                    <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                  </div>
                  <div class="comment-content">{{ comment.content }}</div>
                </div>
              </div>
            </div>
            <div v-if="getMomentState(moment.id).hasMore" class="load-more">
              <el-button text :loading="getMomentState(moment.id).loading" @click="loadMomentComments(moment.id)">加载更多</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载更多 -->
    <div v-if="hasMore" class="load-more">
      <el-button text :loading="loading" @click="loadMore">
        加载更多
      </el-button>
    </div>

    <!-- 发布动态对话框 -->
    <el-dialog v-model="showPublishDialog" title="发布动态" width="600px">
      <el-form :model="momentForm" label-width="80px">
        <el-form-item label="动态类型">
          <el-radio-group v-model="momentForm.type">
            <el-radio value="text">纯文本</el-radio>
            <el-radio value="song">分享歌曲</el-radio>
            <el-radio value="playlist">分享歌单</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="内容">
          <el-input v-model="momentForm.content" type="textarea" :rows="4" placeholder="说点什么..." maxlength="500"
            show-word-limit ref="publishInputRef" />
          <div style="margin-top:8px; text-align:right;">
            <el-popover placement="top" :width="340" trigger="click">
              <template #reference>
                <el-button text><el-icon><ChatLineRound /></el-icon> 表情</el-button>
              </template>
              <EmojiPicker @pick="insertPublishEmoji" />
            </el-popover>
          </div>
        </el-form-item>

        <el-form-item label="可见范围">
          <el-radio-group v-model="momentForm.visibility">
            <el-radio value="public">公开</el-radio>
            <el-radio value="friends">仅好友</el-radio>
            <el-radio value="private">仅自己</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" @click="handlePublish" :loading="publishing">
          发布
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User,
  Plus,
  Star,
  ChatLineRound,
  Picture
} from '@element-plus/icons-vue'
import EmojiPicker from '@/components/Social/EmojiPicker.vue'
import { useUserStore } from '@/store/user'
import {
  getFriendsMoments,
  publishMoment,
  toggleMomentLike,
  deleteMoment,
  commentMoment,
  getMomentComments
} from '@/api/moment'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const moments = ref([])
const page = ref(0)
const size = ref(20)
const hasMore = ref(true)
const likeLoading = ref(null)
const deleteLoading = ref(null)

// 评论相关（内联）
const commentsByMoment = ref({})
const getMomentState = (id) => {
  if (!commentsByMoment.value[id]) {
    commentsByMoment.value[id] = {
      items: [],
      loading: false,
      hasMore: true,
      page: 0,
      size: 3,
      posting: false,
      input: ''
    }
  }
  return commentsByMoment.value[id]
}

// 发布动态相关
const showPublishDialog = ref(false)
const publishing = ref(false)
const momentForm = ref({
  type: 'text',
  content: '',
  relatedId: null,
  visibility: 'public'
})
const publishInputRef = ref()
const inlineInputRefs = ref({})
const setInlineInputRef = (id, el) => { inlineInputRefs.value[id] = el }

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

// 解析图片JSON
const parseImages = (images) => {
  if (!images) return []
  try {
    return JSON.parse(images)
  } catch {
    return []
  }
}

// 加载动态
const loadMoments = async (reset = false) => {
  if (loading.value) return

  if (reset) {
    page.value = 0
    moments.value = []
    hasMore.value = true
  }

  loading.value = true
  try {
    const res = await getFriendsMoments({
      page: page.value,
      size: size.value
    })

    if (res.code === 200) {
      const data = res.data
      if (reset) {
        moments.value = data.content || []
      } else {
        moments.value.push(...(data.content || []))
      }

      hasMore.value = !data.last
      // 预加载每条动态的少量评论（首次显示时加载）
      moments.value.forEach(m => {
        const st = commentsByMoment.value[m.id]
        if (!st || (st.items && st.items.length === 0)) {
          loadMomentComments(m.id, true)
        }
      })
    }
  } catch (error) {
    console.error('加载动态失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = () => {
  page.value++
  loadMoments()
}

// 点赞
const handleLike = async (moment) => {
  likeLoading.value = moment.id
  try {
    const res = await toggleMomentLike(moment.id)
    if (res.code === 200) {
      moment.likeCount = res.data
        ? (moment.likeCount || 0) + 1
        : Math.max(0, (moment.likeCount || 0) - 1)
    }
  } catch (error) {
    console.error('点赞失败:', error)
  } finally {
    likeLoading.value = null
  }
}

// 评论按钮（用于首次加载或聚焦，无弹窗）
const handleComment = async (moment) => {
  const st = getMomentState(moment.id)
  if (st.items.length === 0) {
    await loadMomentComments(moment.id, true)
  }
}

// 加载某条动态的评论
const loadMomentComments = async (momentId, reset = false) => {
  const st = getMomentState(momentId)
  if (st.loading) return
  if (!reset && st.items.length > 0) return
  if (reset) {
    st.page = 0
    st.items = []
    st.hasMore = true
  }
  st.loading = true
  try {
    const res = await getMomentComments(momentId, { page: st.page, size: st.size })
    if (res.code === 200) {
      const items = res.data?.content || []
      st.items = [...st.items, ...items]
      const last = res.data?.last ?? true
      st.hasMore = !last
      if (!last) st.page += 1
    }
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    st.loading = false
  }
}

// 发表评论（内联）
const submitMomentComment = async (moment) => {
  const st = getMomentState(moment.id)
  const text = (st.input || '').trim()
  if (!text) {
    ElMessage.warning('请输入评论内容')
    return
  }
  if (st.posting) return
  st.posting = true
  try {
    const res = await commentMoment({ momentId: moment.id, content: text })
    if (res.code === 200) {
      ElMessage.success('评论成功')
      st.items.unshift({
        id: res.data?.id || Date.now(),
        content: text,
        createTime: new Date().toISOString(),
        userId: userStore.userInfo?.id,
        username: userStore.userInfo?.nickname,
        userAvatar: userStore.userInfo?.avatar
      })
      moment.commentCount = (moment.commentCount || 0) + 1
      st.input = ''
    }
  } catch (error) {
    console.error('发表评论失败:', error)
  } finally {
    st.posting = false
  }
}

// 删除
const handleDelete = async (moment) => {
  try {
    await ElMessageBox.confirm('确定要删除这条动态吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    deleteLoading.value = moment.id
    const res = await deleteMoment(moment.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      moments.value = moments.value.filter(m => m.id !== moment.id)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  } finally {
    deleteLoading.value = null
  }
}

// 发布动态
const handlePublish = async () => {
  if (!momentForm.value.content.trim() && momentForm.value.type === 'text') {
    ElMessage.warning('请输入动态内容')
    return
  }

  publishing.value = true
  try {
    const res = await publishMoment(momentForm.value)
    if (res.code === 200) {
      ElMessage.success('发布成功')
      showPublishDialog.value = false
      momentForm.value = {
        type: 'text',
        content: '',
        relatedId: null,
        visibility: 'public'
      }
      loadMoments(true)
    }
  } catch (error) {
    console.error('发布动态失败:', error)
  } finally {
    publishing.value = false
  }
}

// 判断是否可以删除
const canDelete = (moment) => {
  return userStore.userInfo?.id === moment.userId
}

// 跳转到歌曲
const goToSong = (songId) => {
  // TODO: 跳转到歌曲详情页
  ElMessage.info('歌曲详情页开发中...')
}

// 跳转到歌单
const goToPlaylist = (playlistId) => {
  router.push(`/playlist/${playlistId}`)
}

onMounted(() => {
  loadMoments(true)
})

const insertPublishEmoji = (emoji) => {
  const el = publishInputRef.value?.textarea || publishInputRef.value?.$refs?.textarea
  if (!el) { momentForm.value.content += emoji; return }
  const start = el.selectionStart || 0, end = el.selectionEnd || 0
  const before = momentForm.value.content.slice(0, start)
  const after = momentForm.value.content.slice(end)
  momentForm.value.content = before + emoji + after
  nextTick(() => { try { const pos = start + emoji.length; el.focus(); el.setSelectionRange(pos, pos) } catch (_) {} })
}

const insertInlineEmoji = (id, emoji) => {
  const refEl = inlineInputRefs.value[id]
  const el = refEl?.textarea || refEl?.$refs?.textarea
  const st = getMomentState(id)
  if (!el) { st.input = (st.input || '') + emoji; return }
  const start = el.selectionStart || 0, end = el.selectionEnd || 0
  const before = (st.input || '').slice(0, start)
  const after = (st.input || '').slice(end)
  st.input = before + emoji + after
  nextTick(() => { try { const pos = start + emoji.length; el.focus(); el.setSelectionRange(pos, pos) } catch (_) {} })
}
</script>

<style scoped>
.moments-page {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.moments-list {
  min-height: 400px;
}

.empty-state {
  padding: 60px 0;
}

.moment-card {
  background: var(--card-bg, #fff);
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(0, 0, 0, 0.1));
}

.moment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.header-info {
  flex: 1;
}

.user-name {
  font-weight: 500;
  color: var(--text-primary, #333);
  margin-bottom: 4px;
}

.moment-time {
  font-size: 12px;
  color: var(--text-tertiary, #999);
}

.moment-content {
  margin-bottom: 16px;
}

.moment-text {
  color: var(--text-secondary, #666);
  line-height: 1.6;
  margin-bottom: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

.moment-share {
  margin-top: 12px;
}

.share-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: var(--bg-secondary, #f5f5f5);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.share-item:hover {
  background: var(--bg-hover, #eee);
}

.share-cover {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  flex-shrink: 0;
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: var(--bg-secondary, #f5f5f5);
  color: var(--text-tertiary, #999);
}

.share-info {
  flex: 1;
  min-width: 0;
}

.share-title {
  font-weight: 500;
  color: var(--text-primary, #333);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.share-type {
  font-size: 12px;
  color: var(--text-tertiary, #999);
}

.moment-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.moment-image {
  width: 100%;
  height: 100px;
  border-radius: 4px;
  cursor: pointer;
}

.moment-actions {
  display: flex;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color, #eee);
}

.load-more {
  text-align: center;
  margin-top: 24px;
}

.comment-dialog-body {
  padding-top: 8px;
}

.comment-actions {
  margin: 10px 0 16px;
  text-align: right;
}

.moment-comments {
  min-height: 160px;
}

.comment-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  display: flex;
  gap: 10px;
}

.comment-main {
  flex: 1;
}

.comment-header {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 13px;
  color: var(--text-tertiary, #999);
}

.comment-username {
  color: var(--text-primary, #333);
  font-weight: 500;
}

.comment-content {
  margin-top: 6px;
  line-height: 1.6;
  color: var(--text-secondary, #555);
  white-space: pre-wrap;
}
</style>


