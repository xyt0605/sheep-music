<template>
  <div class="share-square-page">
    <div class="page-header">
      <h2>分享广场</h2>
      <div class="header-controls">
        <el-radio-group v-model="shareTypeFilter" size="small" @change="handleTypeChange">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="playlist">歌单</el-radio-button>
          <el-radio-button value="song">歌曲</el-radio-button>
        </el-radio-group>
        <el-radio-group v-model="sortType" size="small" @change="handleSortChange" style="margin-left: 12px;">
          <el-radio-button value="latest">最新</el-radio-button>
          <el-radio-button value="hot">热门</el-radio-button>
        </el-radio-group>
      </div>
    </div>
    
    <div v-loading="loading" class="shares-list">
      <div v-if="shares.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无分享" />
      </div>
      
      <div v-else class="shares-grid">
        <div
          v-for="share in filteredShares"
          :key="share.id"
          class="share-card"
          @click="handleShareClick(share)"
        >
          <div class="share-cover">
            <el-image
              :src="getShareCover(share)"
              fit="cover"
              :lazy="true"
            >
              <template #error>
                <div class="image-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <div v-if="share.shareType" class="share-type-badge">
              <el-tag :type="share.shareType === 'song' ? 'success' : 'primary'" size="small">
                {{ share.shareType === 'song' ? '歌曲' : '歌单' }}
              </el-tag>
            </div>
          </div>
          
          <div class="share-content">
            <div class="share-title">{{ getShareTitle(share) }}</div>
            <div v-if="share.description" class="share-description">
              {{ share.description }}
            </div>
            
            <div class="share-meta">
              <div class="share-user">
                <el-avatar :src="share.userAvatar || share.user?.avatar" :size="24">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <span>{{ share.userName || share.user?.nickname }}</span>
              </div>
              
              <div class="share-stats">
                <span><el-icon><View /></el-icon> {{ share.viewCount || 0 }}</span>
                <span v-if="share.shareType === 'song'">
                  <el-icon><Star /></el-icon> {{ share.likeCount || 0 }}
                </span>
                <span v-else>
                  <el-icon><Star /></el-icon> {{ share.collectCount || 0 }}
                </span>
              </div>
            </div>
            
          <div class="share-time">
            {{ formatTime(share.createTime) }}
          </div>
          <div class="share-actions">
            <el-button text @click.stop="handleAction(share)">
              <el-icon><Star /></el-icon>
              {{ share.shareType === 'song' ? '点赞' : '收藏' }} {{ share.shareType === 'song' ? (share.likeCount || 0) : (share.collectCount || 0) }}
            </el-button>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, VideoPlay, User, View, Star } from '@element-plus/icons-vue'
import { 
  getShareSquare, 
  getHotShares, 
  getSongShareSquare,
  getHotSongShares,
  incrementShareView, 
  incrementSongShareView,
  collectSharedPlaylist,
  likeSongShare
} from '@/api/share'
import { getMusicDetail } from '@/api/song'
import { usePlayerStore } from '@/store/player'

const router = useRouter()
const playerStore = usePlayerStore()

const loading = ref(false)
const sortType = ref('latest')
const shareTypeFilter = ref('all')
const shares = ref([])
const page = ref(0)
const size = ref(20)
const hasMore = ref(true)

// 过滤后的分享列表
const filteredShares = computed(() => {
  if (shareTypeFilter.value === 'all') {
    return shares.value
  }
  return shares.value.filter(share => share.shareType === shareTypeFilter.value)
})

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) {
    return '今天'
  } else if (days === 1) {
    return '昨天'
  } else if (days < 7) {
    return `${days}天前`
  } else {
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  }
}

// 加载分享（同时加载歌单和歌曲分享）
const loadShares = async (reset = false) => {
  if (loading.value) return
  
  if (reset) {
    page.value = 0
    shares.value = []
    hasMore.value = true
  }
  
  loading.value = true
  try {
    // 同时加载歌单和歌曲分享
    const [playlistRes, songRes] = await Promise.allSettled([
      sortType.value === 'hot'
        ? getHotShares({ page: page.value, size: size.value })
        : getShareSquare({ page: page.value, size: size.value }),
      sortType.value === 'hot'
        ? getHotSongShares({ page: page.value, size: size.value })
        : getSongShareSquare({ page: page.value, size: size.value })
    ])
    
    let allShares = []
    
    // 处理歌单分享
    if (playlistRes.status === 'fulfilled' && playlistRes.value?.code === 200) {
      const playlistShares = (playlistRes.value.data?.content || []).map(share => ({
        ...share,
        shareType: 'playlist'
      }))
      allShares.push(...playlistShares)
    }
    
    // 处理歌曲分享
    if (songRes.status === 'fulfilled' && songRes.value?.code === 200) {
      const songShares = (songRes.value.data?.content || []).map(share => ({
        ...share,
        shareType: 'song'
      }))
      allShares.push(...songShares)
    }
    
    // 按创建时间排序
    allShares.sort((a, b) => {
      const timeA = new Date(a.createTime).getTime()
      const timeB = new Date(b.createTime).getTime()
      return sortType.value === 'hot' ? 0 : timeB - timeA
    })
    
    if (reset) {
      shares.value = allShares
    } else {
      shares.value.push(...allShares)
    }
    
    // 简化：如果两个都没有更多数据，则没有更多
    const playlistLast = playlistRes.status === 'fulfilled' ? playlistRes.value?.data?.last : true
    const songLast = songRes.status === 'fulfilled' ? songRes.value?.data?.last : true
    hasMore.value = !(playlistLast && songLast)
  } catch (error) {
    console.error('加载分享失败:', error)
    ElMessage.error('加载分享失败')
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = () => {
  page.value++
  loadShares()
}

// 排序改变
const handleSortChange = () => {
  loadShares(true)
}

// 类型过滤改变
const handleTypeChange = () => {
  // 过滤是前端进行的，不需要重新加载
}

// 获取分享封面
const getShareCover = (share) => {
  if (!share) return '/default-cover.svg'
  if (share.shareType === 'song') {
    return share.songCover || '/default-cover.svg'
  }
  return share.playlistCover || share.playlist?.cover || '/default-cover.svg'
}

// 获取分享标题
const getShareTitle = (share) => {
  if (!share) return '未知'
  if (share.shareType === 'song') {
    return share.songName || '未知歌曲'
  }
  return share.playlistName || share.playlist?.name || '未知歌单'
}

// 点击分享卡片
const handleShareClick = async (share) => {
  if (!share || !share.id) return
  
  try {
    // 增加浏览次数
    if (share.shareType === 'song') {
      await incrementSongShareView(share.id)
    } else {
      await incrementShareView(share.id)
    }
  } catch (error) {
    console.error('增加浏览次数失败:', error)
  }
  
  // 跳转到对应页面或播放歌曲
  if (share.shareType === 'song') {
    // 歌曲分享：直接播放歌曲
    if (share.songId) {
      try {
        // 获取歌曲完整信息
        const response = await getMusicDetail(share.songId)
        if (response.code === 200 && response.data) {
          // 播放歌曲
          await playerStore.play(response.data)
          ElMessage.success('开始播放')
        } else {
          ElMessage.error('获取歌曲信息失败')
        }
      } catch (error) {
        console.error('播放歌曲失败:', error)
        ElMessage.error('播放歌曲失败')
      }
    }
  } else if (share.playlistId) {
    router.push(`/playlist/${share.playlistId}`)
  }
}

// 处理点赞/收藏
const handleAction = async (share) => {
  if (!share || !share.id) return
  
  try {
    if (share.shareType === 'song') {
      await likeSongShare(share.id)
      share.likeCount = (share.likeCount || 0) + 1
      ElMessage.success('点赞成功')
    } else {
      await collectSharedPlaylist(share.id)
      share.collectCount = (share.collectCount || 0) + 1
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadShares(true)
})
</script>

<style scoped>
.share-square-page {
  padding: 20px;
  max-width: 1400px;
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

.shares-list {
  min-height: 400px;
}

.empty-state {
  padding: 60px 0;
}

.shares-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.share-card {
  background: var(--card-bg, #fff);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(0,0,0,0.1));
  transition: all 0.3s;
  cursor: pointer;
}

.share-card:hover {
  box-shadow: var(--shadow-md, 0 4px 12px rgba(0,0,0,0.15));
  transform: translateY(-2px);
}

.share-cover {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.share-cover .el-image {
  width: 100%;
  height: 100%;
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: var(--bg-secondary, #f5f5f5);
  color: var(--text-tertiary, #999);
  font-size: 40px;
}

.share-type-badge {
  position: absolute;
  top: 8px;
  right: 8px;
}

.header-controls {
  display: flex;
  align-items: center;
}

.share-content {
  padding: 16px;
}

.share-title {
  font-weight: 600;
  color: var(--text-primary, #333);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.share-description {
  font-size: 14px;
  color: var(--text-secondary, #666);
  margin-bottom: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.share-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.share-user {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary, #666);
}

.share-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--text-tertiary, #999);
}

.share-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.share-time {
  font-size: 12px;
  color: var(--text-tertiary, #999);
}

.share-actions { margin-top: 8px; }

.load-more {
  text-align: center;
  margin-top: 24px;
}
</style>

