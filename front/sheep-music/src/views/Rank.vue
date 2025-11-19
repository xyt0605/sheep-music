<template>
  <div class="rank-page">
    <div class="page-header">
      <h2>📊 {{ pageTitle }}</h2>
      <p class="subtitle">{{ pageSubtitle }}</p>
    </div>
    
    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" class="rank-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="🔥 热门歌曲" name="hot"></el-tab-pane>
      <el-tab-pane label="🎵 新歌速递" name="new"></el-tab-pane>
    </el-tabs>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading"><Loading /></el-icon>
      <p>加载中...</p>
    </div>
    
    <!-- 歌曲列表 -->
    <div v-else-if="songs.length > 0" class="song-list">
      <div 
        v-for="(song, index) in songs" 
        :key="song.id"
        class="song-item"
        :class="{ 'top-item': index < 3 }"
        @click="handlePlaySong(song)"
      >
        <div class="song-rank" :class="getRankClass(index)">
          {{ index + 1 }}
        </div>
        <img :src="song.cover || defaultCover" class="song-cover">
        <div class="song-info">
          <div class="song-name">{{ song.title }}</div>
          <div class="song-artist">
            <template v-for="(artist, idx) in song.artists || []" :key="artist.id">
              <span class="clickable" @click.stop="goToArtist(artist.id)">{{ artist.name }}</span>
              <span v-if="idx < (song.artists?.length || 0) - 1"> / </span>
            </template>
            <span v-if="!song.artists || song.artists.length === 0">未知歌手</span>
          </div>
        </div>
        <div class="song-stats">
          <div v-if="activeTab === 'hot'" class="stat-item">
            <el-icon><Headset /></el-icon>
            <span>{{ formatCount(song.playCount) }}</span>
          </div>
          <div class="stat-item">
            <el-icon><Clock /></el-icon>
            <span>{{ formatDuration(song.duration) }}</span>
          </div>
        </div>
        <div class="song-actions">
          <el-button 
            icon="CaretRight" 
            circle 
            size="small" 
            @click.stop="handlePlaySong(song)"
            title="播放"
          />
          <el-button 
            icon="Plus" 
            circle 
            size="small" 
            @click.stop="handleAddToPlaylist(song)"
            title="添加到播放列表"
          />
          <el-button 
            icon="FolderAdd" 
            circle 
            size="small" 
            @click.stop="showAddToPlaylistDialog(song.id)"
            title="添加到歌单"
          />
          <el-button 
            :icon="favoriteSongs[song.id] ? 'StarFilled' : 'Star'"
            circle 
            size="small" 
            :type="favoriteSongs[song.id] ? 'danger' : ''"
            @click.stop="handleToggleFavorite(song.id)"
            title="收藏"
          />
        </div>
    </div>
    </div>
    
    <!-- 空状态 -->
    <div v-else class="empty-state">
      <el-empty :description="`暂无${activeTab === 'hot' ? '热门' : '最新'}歌曲`" />
    </div>
    
    <!-- 分页 -->
    <el-pagination
      v-if="pagination.total > pagination.size"
      v-model:current-page="pagination.page"
      :page-size="pagination.size"
      :total="pagination.total"
      layout="prev, pager, next, total"
      class="pagination"
      @current-change="loadSongs"
    />
    
    <!-- 添加到歌单对话框 -->
    <PlaylistSelector 
      v-model="playlistSelectorVisible" 
      :song-id="selectedSongId"
      @success="handleAddSuccess"
    />
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePlayerStore } from '@/store/player'
import { useUserStore } from '@/store/user'
import { getHotSongs, getNewSongs } from '@/api/song'
import { toggleFavorite, batchCheckFavorites } from '@/api/favorite'
import { ElMessage } from 'element-plus'
import { Loading, Headset, Clock } from '@element-plus/icons-vue'
import PlaylistSelector from '@/components/PlaylistSelector.vue'

export default {
  name: 'Rank',
  components: {
    Loading,
    Headset,
    Clock,
    PlaylistSelector
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const playerStore = usePlayerStore()
    const userStore = useUserStore()
    
    const activeTab = ref('hot')
    const loading = ref(false)
    const songs = ref([])
  const defaultCover = '/default-cover.svg'
    const favoriteSongs = ref({}) // 收藏状态
    
    const pagination = reactive({
      page: 1,
      size: 50,
      total: 0
    })
    
    // 歌单选择器
    const playlistSelectorVisible = ref(false)
    const selectedSongId = ref(null)
    
    // 页面标题
    const pageTitle = computed(() => {
      return activeTab.value === 'hot' ? '🔥 热门歌曲排行榜' : '🎵 新歌速递排行榜'
    })
    
    const pageSubtitle = computed(() => {
      return activeTab.value === 'hot' ? '最受欢迎的热门歌曲' : '最新发布的音乐作品'
    })
    
    // 加载歌曲列表
    const loadSongs = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.page - 1,
          size: pagination.size
        }
        
        const res = activeTab.value === 'hot' 
          ? await getHotSongs(params)
          : await getNewSongs(params)
        
        if (res.code === 200) {
          songs.value = res.data.content || []
          pagination.total = res.data.totalElements || 0
          
          // 加载收藏状态
          await loadFavoriteStatus()
        }
      } catch (error) {
        console.error('加载排行榜失败:', error)
        ElMessage.error('加载排行榜失败')
      } finally {
        loading.value = false
      }
    }
    
    // 加载收藏状态
    const loadFavoriteStatus = async () => {
      if (!userStore.isLogin || songs.value.length === 0) return
      
      try {
        const songIds = songs.value.map(s => s.id)
        const res = await batchCheckFavorites(songIds)
        if (res.code === 200) {
          favoriteSongs.value = res.data || {}
        }
      } catch (error) {
        console.error('加载收藏状态失败:', error)
      }
    }
    
    // 切换收藏状态
    const handleToggleFavorite = async (songId) => {
      if (!userStore.isLogin) {
        ElMessage.warning('请先登录')
        router.push('/login')
        return
      }
      
      try {
        const res = await toggleFavorite(songId)
        if (res.code === 200) {
          favoriteSongs.value[songId] = res.data.isFavorite
          ElMessage.success(res.data.isFavorite ? '收藏成功' : '取消收藏成功')
        }
      } catch (error) {
        ElMessage.error('操作失败')
      }
    }
    
    // Tab 切换
    const handleTabChange = () => {
      pagination.page = 1
      loadSongs()
    }
    
    // 播放歌曲
    const handlePlaySong = (song) => {
      playerStore.play(song, songs.value)
    }
    
    // 添加到播放列表
    const handleAddToPlaylist = (song) => {
      playerStore.addToPlaylist(song)
      ElMessage.success(`已添加到播放列表: ${song.title}`)
    }
    
    // 跳转到歌手详情
    const goToArtist = (artistId) => {
      if (artistId) {
        router.push(`/artist/${artistId}`)
      }
    }
    
    // 显示添加到歌单对话框
    const showAddToPlaylistDialog = (songId) => {
      if (!userStore.isLogin) {
        ElMessage.warning('请先登录')
        router.push('/login')
        return
      }
      selectedSongId.value = songId
      playlistSelectorVisible.value = true
    }
    
    // 添加成功回调
    const handleAddSuccess = () => {
      // 可以在这里做一些额外的处理
    }
    
    // 获取排名样式
    const getRankClass = (index) => {
      if (index === 0) return 'rank-first'
      if (index === 1) return 'rank-second'
      if (index === 2) return 'rank-third'
      return ''
    }
    
    // 格式化数量
    const formatCount = (count) => {
      if (!count) return 0
      if (count >= 10000) {
        return (count / 10000).toFixed(1) + '万'
      }
      return count
    }
    
    // 格式化时长
    const formatDuration = (seconds) => {
      if (!seconds) return '--:--'
      const mins = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${mins}:${secs.toString().padStart(2, '0')}`
    }
    
    // 监听路由参数变化
    watch(() => route.query.type, (type) => {
      if (type === 'hot' || type === 'new') {
        activeTab.value = type
        pagination.page = 1
        loadSongs()
      }
    }, { immediate: true })
    
    onMounted(() => {
      // 如果没有指定类型，默认显示热门
      if (!route.query.type) {
        loadSongs()
      }
    })
    
    return {
      activeTab,
      loading,
      songs,
      defaultCover,
      favoriteSongs,
      pagination,
      pageTitle,
      pageSubtitle,
      playlistSelectorVisible,
      selectedSongId,
      handleTabChange,
      handlePlaySong,
      handleAddToPlaylist,
      handleToggleFavorite,
      goToArtist,
      showAddToPlaylistDialog,
      handleAddSuccess,
      getRankClass,
      formatCount,
      formatDuration,
      loadSongs
    }
  }
}
</script>

<style scoped>
.rank-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 32px;
  color: #333;
  margin-bottom: 10px;
  font-weight: bold;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

/* Tab 样式 */
.rank-tabs {
  margin-bottom: 30px;
}

.rank-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
  color: #909399;
}

.loading-state .el-icon {
  font-size: 50px;
  margin-bottom: 15px;
  color: #409eff;
}

/* 歌曲列表 */
.song-list {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.song-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  gap: 15px;
  margin-bottom: 10px;
}

.song-item:hover {
  background: linear-gradient(135deg, #f5f7fa 0%, #ecf0f5 100%);
  transform: translateX(5px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.song-item.top-item {
  background: linear-gradient(135deg, #fff9f0 0%, #fff5e6 100%);
}

.song-item.top-item:hover {
  background: linear-gradient(135deg, #fff5e6 0%, #ffecd1 100%);
}

/* 排名 */
.song-rank {
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  color: #999;
  background: #f0f0f0;
  border-radius: 8px;
  flex-shrink: 0;
}

.song-rank.rank-first {
  background: linear-gradient(135deg, #ffd700 0%, #ffed4e 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(255, 215, 0, 0.4);
  font-size: 24px;
}

.song-rank.rank-second {
  background: linear-gradient(135deg, #c0c0c0 0%, #e0e0e0 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(192, 192, 192, 0.4);
  font-size: 22px;
}

.song-rank.rank-third {
  background: linear-gradient(135deg, #cd7f32 0%, #e89968 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(205, 127, 50, 0.4);
  font-size: 22px;
}

/* 封面 */
.song-cover {
  width: 70px;
  height: 70px;
  border-radius: 8px;
  object-fit: cover;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
  flex-shrink: 0;
}

.song-item:hover .song-cover {
  transform: scale(1.05);
}

/* 歌曲信息 */
.song-info {
  flex: 1;
  min-width: 0;
}

.song-name {
  font-size: 16px;
  color: #333;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.song-artist {
  font-size: 14px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.clickable {
  cursor: pointer;
  transition: color 0.3s;
}

.clickable:hover {
  color: #409eff;
}

/* 统计信息 */
.song-stats {
  display: flex;
  gap: 20px;
  margin-right: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #999;
}

.stat-item .el-icon {
  font-size: 16px;
}

/* 操作按钮 */
.song-actions {
  display: flex;
  gap: 10px;
  opacity: 0;
  transition: opacity 0.3s;
  flex-shrink: 0;
}

.song-item:hover .song-actions {
  opacity: 1;
}

/* 空状态 */
.empty-state {
  padding: 80px 20px;
  text-align: center;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

/* 响应式 */
@media (max-width: 768px) {
  .rank-page {
    padding: 15px;
  }
  
  .page-header h2 {
    font-size: 24px;
  }
  
  .song-item {
    padding: 10px;
    gap: 10px;
  }
  
  .song-rank {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }
  
  .song-rank.rank-first,
  .song-rank.rank-second,
  .song-rank.rank-third {
    font-size: 18px;
  }
  
  .song-cover {
    width: 50px;
    height: 50px;
  }
  
  .song-stats {
    display: none;
  }
  
  .song-actions {
    opacity: 1;
  }
}
</style>

