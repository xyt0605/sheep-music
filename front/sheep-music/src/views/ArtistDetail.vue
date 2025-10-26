<template>
  <div class="artist-detail">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <!-- 歌手详情 -->
    <div v-else-if="artist" class="artist-content">
      <!-- 歌手信息卡片 -->
      <div class="artist-card">
        <div class="artist-avatar">
          <img :src="artist.avatar || defaultAvatar" alt="歌手头像">
        </div>
        <div class="artist-info">
          <h1 class="artist-name">{{ artist.name }}</h1>
          <div class="artist-meta">
            <el-tag v-if="artist.region" type="info" size="large">
              <el-icon><LocationInformation /></el-icon>
              {{ artist.region }}
            </el-tag>
            <el-tag type="success" size="large">
              <el-icon><Headset /></el-icon>
              {{ songList.length }} 首歌曲
            </el-tag>
          </div>
          <p v-if="artist.description" class="artist-description">
            {{ artist.description }}
          </p>
          <div class="artist-actions">
            <el-button 
              type="primary" 
              size="large"
              :disabled="songList.length === 0"
              @click="playAll"
            >
              <el-icon><CaretRight /></el-icon>
              播放全部
            </el-button>
            <el-button size="large" @click="goBack">
              <el-icon><Back /></el-icon>
              返回
            </el-button>
          </div>
        </div>
      </div>

      <!-- 歌曲列表 -->
      <div class="songs-section">
        <div class="section-header">
          <h2>
            <el-icon><List /></el-icon>
            歌曲列表
          </h2>
        </div>

        <div v-if="songList.length > 0" class="song-list">
          <div 
            v-for="(song, index) in songList" 
            :key="song.id"
            class="song-item"
            @click="handlePlaySong(song, index)"
          >
            <div class="song-index">{{ index + 1 }}</div>
            <img :src="song.cover || defaultCover" class="song-cover">
            <div class="song-info">
              <div class="song-name">{{ song.title }}</div>
              <div class="song-meta">
                <span v-if="song.albumName" class="album-name">
                  <el-icon><Document /></el-icon>
                  {{ song.albumName }}
                </span>
                <span class="song-duration">{{ formatDuration(song.duration) }}</span>
              </div>
            </div>
            <div class="song-play-count">
              <el-icon><Headset /></el-icon>
              {{ formatPlayCount(song.playCount) }}
            </div>
            <div class="song-actions">
              <el-button 
                icon="CaretRight" 
                circle 
                size="small" 
                @click.stop="handlePlaySong(song, index)"
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

        <!-- 无歌曲 -->
        <div v-else class="empty-songs">
          <el-empty description="该歌手暂无歌曲" />
        </div>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else class="error-state">
      <el-result icon="error" title="加载失败" sub-title="无法获取歌手信息">
        <template #extra>
          <el-button type="primary" @click="goBack">返回</el-button>
        </template>
      </el-result>
    </div>
    
    <!-- 添加到歌单对话框 -->
    <PlaylistSelector 
      v-model="playlistSelectorVisible" 
      :song-id="selectedSongId"
      @success="handleAddSuccess"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePlayerStore } from '@/store/player'
import { useUserStore } from '@/store/user'
import { getArtistDetail } from '@/api/artist'
import { getSongsByArtist } from '@/api/song'
import { toggleFavorite, batchCheckFavorites } from '@/api/favorite'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Loading,
  LocationInformation,
  Headset,
  CaretRight,
  Back,
  List,
  Document
} from '@element-plus/icons-vue'
import PlaylistSelector from '@/components/PlaylistSelector.vue'

const route = useRoute()
const router = useRouter()
const playerStore = usePlayerStore()
const userStore = useUserStore()

// 数据
const loading = ref(true)
const artist = ref(null)
const songList = ref([])
const favoriteSongs = ref({}) // 收藏状态 { songId: true/false }
const defaultAvatar = 'https://via.placeholder.com/200?text=Artist'
const defaultCover = 'https://via.placeholder.com/60?text=Music'

// 歌单选择器
const playlistSelectorVisible = ref(false)
const selectedSongId = ref(null)

// 加载歌手信息
const loadArtist = async () => {
  try {
    const res = await getArtistDetail(route.params.id)
    if (res.code === 200) {
      artist.value = res.data
    }
  } catch (error) {
    console.error('加载歌手信息失败:', error)
    ElMessage.error('加载歌手信息失败')
  }
}

// 加载歌曲列表
const loadSongs = async () => {
  try {
    const res = await getSongsByArtist(route.params.id)
    if (res.code === 200) {
      songList.value = res.data || []
    }
  } catch (error) {
    console.error('加载歌曲列表失败:', error)
    ElMessage.error('加载歌曲列表失败')
  }
}

// 加载收藏状态
const loadFavoriteStatus = async () => {
  if (!userStore.isLogin || songList.value.length === 0) return
  
  try {
    const songIds = songList.value.map(song => song.id)
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
      const isFavorite = res.data.isFavorite
      favoriteSongs.value[songId] = isFavorite
      ElMessage.success(isFavorite ? '收藏成功' : '取消收藏成功')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('收藏操作失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  }
}

// 初始化数据
const init = async () => {
  loading.value = true
  await Promise.all([loadArtist(), loadSongs()])
  await loadFavoriteStatus() // 加载收藏状态
  loading.value = false
}

// 播放全部
const playAll = () => {
  if (songList.value.length > 0) {
    playerStore.play(songList.value[0], songList.value)
    ElMessage.success('开始播放')
  }
}

// 播放歌曲
const handlePlaySong = (song) => {
  playerStore.play(song, songList.value)
}

// 添加到播放列表
const handleAddToPlaylist = (song) => {
  playerStore.addToPlaylist(song)
  ElMessage.success(`已添加到播放列表: ${song.title}`)
}

// 格式化时长
const formatDuration = (seconds) => {
  if (!seconds) return '--:--'
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 格式化播放次数
const formatPlayCount = (count) => {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万'
  }
  return count || 0
}

// 返回
const goBack = () => {
  router.back()
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

// 初始化
onMounted(() => {
  init()
})
</script>

<style scoped>
.artist-detail {
  min-height: calc(100vh - 160px);
  padding-bottom: 20px;
}

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
  color: #909399;
}

.loading-container .el-icon {
  font-size: 40px;
  margin-bottom: 15px;
}

/* 歌手信息卡片 */
.artist-card {
  display: flex;
  gap: 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px;
  border-radius: 12px;
  color: white;
  margin-bottom: 30px;
}

.artist-avatar {
  flex-shrink: 0;
}

.artist-avatar img {
  width: 200px;
  height: 200px;
  border-radius: 12px;
  object-fit: cover;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3);
}

.artist-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.artist-name {
  font-size: 36px;
  font-weight: bold;
  margin: 0;
}

.artist-meta {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.artist-meta .el-tag {
  display: flex;
  align-items: center;
  gap: 5px;
}

.artist-description {
  font-size: 16px;
  line-height: 1.6;
  opacity: 0.9;
  margin: 0;
}

.artist-actions {
  display: flex;
  gap: 15px;
  margin-top: auto;
}

/* 歌曲列表部分 */
.songs-section {
  margin-top: 30px;
}

.section-header {
  margin-bottom: 20px;
}

.section-header h2 {
  font-size: 22px;
  color: #333;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
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
  padding: 12px;
  border-radius: 8px;
  transition: background 0.3s;
  gap: 15px;
  cursor: pointer;
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
  min-width: 0;
}

.song-name {
  font-size: 15px;
  color: #333;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  font-size: 13px;
  color: #999;
}

.album-name {
  display: flex;
  align-items: center;
  gap: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-duration {
  flex-shrink: 0;
}

.song-play-count {
  font-size: 13px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 5px;
  margin-right: 20px;
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

/* 空状态 */
.empty-songs {
  padding: 60px 20px;
}

.error-state {
  padding: 60px 20px;
}

/* 响应式 */
@media (max-width: 768px) {
  .artist-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 30px 20px;
  }

  .artist-avatar img {
    width: 150px;
    height: 150px;
  }

  .artist-name {
    font-size: 28px;
  }

  .artist-meta {
    justify-content: center;
  }

  .artist-actions {
    justify-content: center;
    width: 100%;
  }

  .artist-actions .el-button {
    flex: 1;
  }

  .song-play-count {
    display: none;
  }
}
</style>

