<template>
  <div class="home">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <h2>你好，{{ userStore.userInfo?.nickname || '音乐爱好者' }}</h2>
      <p class="welcome-text">
        欢迎来到 Sheep Music，开始你的音乐之旅
      </p>
    </div>
    
    <!-- 个性化推荐入口 -->
    <div class="recommend-banner" @click="goToDiscover">
      <div class="banner-content">
        <div class="banner-icon">
          <el-icon><MagicStick /></el-icon>
        </div>
        <div class="banner-text">
          <h3>✨ 发现你的专属音乐</h3>
          <p>基于你的喜好，为你推荐精选歌曲和歌单</p>
        </div>
        <div class="banner-action">
          <el-button type="primary" round>
            立即发现
            <el-icon class="ml-5"><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
    
    <!-- 热门歌曲 -->
    <section class="content-section">
      <div class="section-header">
        <h3>🔥 热门歌曲</h3>
        <el-link type="primary" @click="goToRank('hot')">查看更多 ›</el-link>
      </div>
      <div class="song-list">
        <div 
          v-for="(song, index) in hotSongs" 
          :key="song.id" 
          class="song-item"
          @click="handlePlaySong(song)"
        >
          <div class="song-index" :class="{ 'top-three': index < 3 }">{{ index + 1 }}</div>
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
          <div class="song-play-count">
            <el-icon><Headset /></el-icon>
            {{ formatPlayCount(song.playCount) }}
          </div>
          <div class="song-actions">
            <el-button icon="CaretRight" circle size="small" @click.stop="handlePlaySong(song)" title="播放" />
            <el-button icon="Plus" circle size="small" @click.stop="handleAddToPlaylist(song)" title="添加到播放列表" />
            <el-button icon="FolderAdd" circle size="small" @click.stop="showAddToPlaylistDialog(song.id)" title="添加到歌单" />
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
      <div v-if="hotSongs.length === 0" class="empty-state">
        <el-empty description="暂无热门歌曲" />
      </div>
    </section>
    
    <!-- 新歌速递 -->
    <section class="content-section">
      <div class="section-header">
        <h3>🎵 新歌速递</h3>
        <el-link type="primary" @click="goToRank('new')">查看更多 ›</el-link>
      </div>
      <div class="song-list">
        <div 
          v-for="(song, index) in newSongs" 
          :key="song.id" 
          class="song-item"
          @click="handlePlaySong(song)"
        >
          <div class="song-index">{{ index + 1 }}</div>
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
          <div class="song-time">{{ formatDuration(song.duration) }}</div>
          <div class="song-actions">
            <el-button icon="CaretRight" circle size="small" @click.stop="handlePlaySong(song)" title="播放" />
            <el-button icon="Plus" circle size="small" @click.stop="handleAddToPlaylist(song)" title="添加到播放列表" />
            <el-button icon="FolderAdd" circle size="small" @click.stop="showAddToPlaylistDialog(song.id)" title="添加到歌单" />
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
      <div v-if="newSongs.length === 0" class="empty-state">
        <el-empty description="暂无最新歌曲" />
      </div>
    </section>
    
    <!-- 添加到歌单对话框 -->
    <PlaylistSelector 
      v-model="playlistSelectorVisible" 
      :song-id="selectedSongId"
      @success="handleAddSuccess"
    />
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { usePlayerStore } from '@/store/player'
import { getHotSongs, getNewSongs } from '@/api/song'
import { toggleFavorite, batchCheckFavorites } from '@/api/favorite'
import { ElMessage } from 'element-plus'
import { Headset, MagicStick, ArrowRight } from '@element-plus/icons-vue'
import PlaylistSelector from '@/components/PlaylistSelector.vue'

export default {
  name: 'Home',
  components: {
    Headset,
    MagicStick,
    ArrowRight,
    PlaylistSelector
  },
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    const playerStore = usePlayerStore()
    const hotSongs = ref([])
    const newSongs = ref([])
    const defaultCover = 'https://via.placeholder.com/60?text=Music'
    const favoriteSongs = ref({}) // 收藏状态 { songId: true/false }
    
    // 歌单选择器
    const playlistSelectorVisible = ref(false)
    const selectedSongId = ref(null)
    
    // 加载热门歌曲
    const loadHotSongs = async () => {
      try {
        const res = await getHotSongs({ page: 0, size: 10 })
        if (res.code === 200) {
          hotSongs.value = res.data.content || []
        }
      } catch (error) {
        console.error('加载热门歌曲失败:', error)
      }
    }
    
    // 加载最新歌曲
    const loadNewSongs = async () => {
      try {
        const res = await getNewSongs({ page: 0, size: 10 })
        if (res.code === 200) {
          newSongs.value = res.data.content || []
        }
      } catch (error) {
        console.error('加载最新歌曲失败:', error)
      }
    }
    
    // 播放歌曲
    const handlePlaySong = (song) => {
      // 使用播放器 store 播放歌曲
      playerStore.play(song, hotSongs.value.concat(newSongs.value))
    }
    
    // 添加到播放列表
    const handleAddToPlaylist = (song) => {
      playerStore.addToPlaylist(song)
      ElMessage.success(`已添加到播放列表: ${song.title}`)
    }
    
    // 格式化播放次数
    const formatPlayCount = (count) => {
      if (count >= 10000) {
        return (count / 10000).toFixed(1) + '万'
      }
      return count || 0
    }
    
    // 格式化时长
    const formatDuration = (seconds) => {
      if (!seconds) return '--:--'
      const mins = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${mins}:${secs.toString().padStart(2, '0')}`
    }
    
    // 跳转到歌手详情页
    const goToArtist = (artistId) => {
      if (artistId) {
        router.push(`/artist/${artistId}`)
      }
    }
    
    // 跳转到排行榜页面
    const goToRank = (type) => {
      router.push({ path: '/rank', query: { type } })
    }
    
    // 跳转到发现页面
    const goToDiscover = () => {
      router.push('/discover')
    }
    
    // 加载收藏状态
    const loadFavoriteStatus = async () => {
      if (!userStore.isLogin) return
      
      try {
        const allSongs = [...hotSongs.value, ...newSongs.value]
        const songIds = [...new Set(allSongs.map(s => s.id))] // 去重
        
        if (songIds.length > 0) {
          const res = await batchCheckFavorites(songIds)
          if (res.code === 200) {
            favoriteSongs.value = res.data || {}
          }
        }
      } catch (error) {
        console.error('加载收藏状态失败:', error)
        ElMessage.error('查询失败: ' + (error.response?.data?.message || error.message))
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
          ElMessage.success(res.message || (res.data.isFavorite ? '收藏成功' : '取消收藏成功'))
        }
      } catch (error) {
        ElMessage.error('操作失败')
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
    
    onMounted(async () => {
      await loadHotSongs()
      await loadNewSongs()
      await loadFavoriteStatus()
    })
    
    return {
      userStore,
      hotSongs,
      newSongs,
      defaultCover,
      favoriteSongs,
      handlePlaySong,
      handleAddToPlaylist,
      handleToggleFavorite,
      showAddToPlaylistDialog,
      handleAddSuccess,
      playlistSelectorVisible,
      selectedSongId,
      formatPlayCount,
      formatDuration,
      goToArtist,
      goToRank,
      goToDiscover
    }
  }
}
</script>

<style scoped>
.home {
  padding-bottom: 20px;
}

/* 欢迎区域 */
.welcome-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px;
  border-radius: 12px;
  color: white;
  margin-bottom: 30px;
}

.welcome-section h2 {
  font-size: 28px;
  margin-bottom: 10px;
}

.welcome-text {
  font-size: 16px;
  opacity: 0.9;
}

/* 推荐Banner */
.recommend-banner {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  border-radius: 16px;
  padding: 30px;
  margin-bottom: 30px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 20px rgba(245, 87, 108, 0.3);
}

.recommend-banner:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 30px rgba(245, 87, 108, 0.4);
}

.banner-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.banner-icon {
  font-size: 50px;
  color: white;
  background: rgba(255, 255, 255, 0.2);
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.banner-text {
  flex: 1;
  color: white;
}

.banner-text h3 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
}

.banner-text p {
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
}

.banner-action {
  flex-shrink: 0;
}

.ml-5 {
  margin-left: 5px;
}

/* 内容区块 */
.content-section {
  margin-bottom: 40px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  font-size: 22px;
  color: #333;
  font-weight: bold;
}

/* 歌单网格 */
.playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.playlist-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.playlist-card:hover {
  transform: translateY(-5px);
}

.playlist-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
}

.playlist-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.play-btn {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 50px;
  height: 50px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  opacity: 0;
  transition: opacity 0.3s;
}

.playlist-card:hover .play-btn {
  opacity: 1;
}

.playlist-info {
  margin-top: 10px;
}

.playlist-name {
  font-size: 14px;
  color: #333;
  font-weight: bold;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playlist-desc {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

/* 歌曲列表 */
.song-list {
  background: white;
  border-radius: 8px;
  padding: 10px;
}

.song-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border-radius: 8px;
  transition: background 0.3s;
  gap: 15px;
}

.song-item:hover {
  background: #f5f5f5;
}

.song-index {
  width: 30px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.song-cover {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  object-fit: cover;
}

.song-info {
  flex: 1;
}

.song-name {
  font-size: 15px;
  color: #333;
  margin-bottom: 5px;
}

.song-artist {
  font-size: 13px;
  color: #999;
}

.song-artist.clickable {
  cursor: pointer;
  transition: color 0.3s;
}

.song-artist.clickable:hover {
  color: #409eff;
}

.song-play-count,
.song-time {
  font-size: 13px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 5px;
  margin-right: 20px;
}

.song-index.top-three {
  color: #ff6b6b;
  font-weight: bold;
  font-size: 16px;
}

.song-actions {
  display: flex;
  gap: 10px;
  opacity: 0;
  transition: opacity 0.3s;
}

.song-item:hover .song-actions {
  opacity: 1;
}

.empty-state {
  padding: 40px 0;
}

/* 响应式 - 全局样式已覆盖，这里只保留页面特有的样式 */
@media (max-width: 768px) {
  .home {
    padding: 0 10px 20px;
  }
  
  .content-section {
    margin-bottom: 30px;
  }
}
</style>