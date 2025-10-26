<template>
  <div class="discover-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>🎵 发现音乐</h2>
      <p class="subtitle">基于你的喜好，为你推荐精选内容</p>
    </div>

    <!-- 猜你喜欢 -->
    <div class="recommendation-section">
      <div class="section-header">
        <div class="title-group">
          <el-icon class="section-icon"><MagicStick /></el-icon>
          <h3>猜你喜欢</h3>
          <span class="section-desc">每天为你更新</span>
        </div>
        <el-button 
          text 
          icon="Refresh" 
          @click="refreshGuessYouLike"
          :loading="guessYouLikeLoading"
        >
          换一批
        </el-button>
      </div>

      <div v-loading="guessYouLikeLoading" class="song-grid">
        <div 
          v-for="song in guessYouLikeSongs" 
          :key="song.id"
          class="song-card"
          @click="handlePlaySong(song)"
        >
          <div class="song-cover-wrapper">
            <img :src="song.cover || defaultCover" class="song-cover" alt="封面">
            <div class="cover-overlay">
              <el-icon class="play-icon"><CaretRight /></el-icon>
            </div>
            <div class="song-play-count">
              <el-icon><Headset /></el-icon>
              {{ formatCount(song.playCount) }}
            </div>
          </div>
          <div class="song-info">
            <div class="song-title">{{ song.title }}</div>
            <div class="song-artist">{{ getArtistNames(song) }}</div>
          </div>
          <div class="song-actions-hover">
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

      <div v-if="!guessYouLikeLoading && guessYouLikeSongs.length === 0" class="empty-hint">
        <el-empty description="暂无推荐，试试多听听歌曲吧~">
          <template #image>
            <el-icon style="font-size: 80px; color: #ddd;"><Headset /></el-icon>
          </template>
        </el-empty>
      </div>
    </div>

    <!-- 个性化推荐 -->
    <div class="recommendation-section">
      <div class="section-header">
        <div class="title-group">
          <el-icon class="section-icon"><UserFilled /></el-icon>
          <h3>为你推荐</h3>
          <span class="section-desc">根据你的收藏和播放习惯</span>
        </div>
        <el-button 
          text 
          icon="Refresh" 
          @click="refreshPersonalized"
          :loading="personalizedLoading"
        >
          换一批
        </el-button>
      </div>

      <div v-loading="personalizedLoading" class="song-list">
        <div 
          v-for="(song, index) in personalizedSongs" 
          :key="song.id"
          class="song-item"
          @click="handlePlaySong(song)"
        >
          <div class="song-index">{{ index + 1 }}</div>
          <img :src="song.cover || defaultCover" class="song-cover-small" alt="封面">
          <div class="song-info-inline">
            <div class="song-name">{{ song.title }}</div>
            <div class="song-artist-name">{{ getArtistNames(song) }}</div>
          </div>
          <div class="song-stats">
            <el-icon><Headset /></el-icon>
            {{ formatCount(song.playCount) }}
          </div>
          <div class="song-duration">{{ formatDuration(song.duration) }}</div>
          <div class="song-actions">
            <el-button 
              icon="CaretRight" 
              circle 
              size="small"
              @click.stop="handlePlaySong(song)"
            />
            <el-button 
              icon="Plus" 
              circle 
              size="small"
              @click.stop="handleAddToPlaylist(song)"
            />
            <el-button 
              icon="FolderAdd" 
              circle 
              size="small"
              @click.stop="showAddToPlaylistDialog(song.id)"
            />
            <el-button 
              :icon="favoriteSongs[song.id] ? 'StarFilled' : 'Star'"
              circle 
              size="small"
              :type="favoriteSongs[song.id] ? 'danger' : ''"
              @click.stop="handleToggleFavorite(song.id)"
            />
          </div>
        </div>
      </div>

      <div v-if="!personalizedLoading && personalizedSongs.length === 0" class="empty-hint">
        <el-empty description="暂无推荐">
          <template #image>
            <el-icon style="font-size: 80px; color: #ddd;"><Document /></el-icon>
          </template>
        </el-empty>
      </div>
    </div>

    <!-- 推荐歌单 -->
    <div class="recommendation-section">
      <div class="section-header">
        <div class="title-group">
          <el-icon class="section-icon"><Collection /></el-icon>
          <h3>精选歌单</h3>
          <span class="section-desc">发现更多好歌</span>
        </div>
        <el-button 
          text 
          icon="Refresh" 
          @click="refreshPlaylists"
          :loading="playlistsLoading"
        >
          换一批
        </el-button>
      </div>

      <div v-loading="playlistsLoading" class="playlist-grid">
        <div 
          v-for="playlist in recommendedPlaylists" 
          :key="playlist.id"
          class="playlist-card"
          @click="goToPlaylist(playlist.id)"
        >
          <div class="playlist-cover-wrapper">
            <img :src="playlist.cover || defaultPlaylistCover" class="playlist-cover" alt="封面">
            <div class="cover-overlay">
              <el-icon class="play-icon"><CaretRight /></el-icon>
            </div>
            <div class="playlist-song-count">
              <el-icon><Headset /></el-icon>
              {{ playlist.songCount }} 首
            </div>
          </div>
          <div class="playlist-info">
            <div class="playlist-name">{{ playlist.name }}</div>
            <div class="playlist-creator">
              by {{ playlist.userName || '未知' }}
            </div>
          </div>
        </div>
      </div>

      <div v-if="!playlistsLoading && recommendedPlaylists.length === 0" class="empty-hint">
        <el-empty description="暂无推荐歌单">
          <template #image>
            <el-icon style="font-size: 80px; color: #ddd;"><FolderOpened /></el-icon>
          </template>
        </el-empty>
      </div>
    </div>

    <!-- 歌单选择器 -->
    <PlaylistSelector 
      v-model="playlistSelectorVisible" 
      :song-id="selectedSongId"
      @success="handleAddSuccess"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePlayerStore } from '@/store/player'
import { useUserStore } from '@/store/user'
import { 
  getGuessYouLike, 
  getPersonalizedSongs, 
  getRecommendedPlaylists 
} from '@/api/recommendation'
import { toggleFavorite, batchCheckFavorites } from '@/api/favorite'
import { ElMessage } from 'element-plus'
import { 
  MagicStick, UserFilled, Collection, CaretRight, 
  Headset, Plus, FolderAdd, Star, StarFilled, 
  Refresh, Document, FolderOpened 
} from '@element-plus/icons-vue'
import PlaylistSelector from '@/components/PlaylistSelector.vue'

const router = useRouter()
const playerStore = usePlayerStore()
const userStore = useUserStore()

// 猜你喜欢
const guessYouLikeSongs = ref([])
const guessYouLikeLoading = ref(false)

// 个性化推荐
const personalizedSongs = ref([])
const personalizedLoading = ref(false)

// 推荐歌单
const recommendedPlaylists = ref([])
const playlistsLoading = ref(false)

// 收藏状态
const favoriteSongs = ref({})

// 歌单选择器
const playlistSelectorVisible = ref(false)
const selectedSongId = ref(null)

const defaultCover = 'https://via.placeholder.com/200?text=Music'
const defaultPlaylistCover = 'https://via.placeholder.com/200?text=Playlist'

// ========== 初始化 ==========
onMounted(() => {
  loadGuessYouLike()
  loadPersonalized()
  loadRecommendedPlaylists()
})

// ========== 加载推荐数据 ==========

// 加载猜你喜欢
const loadGuessYouLike = async () => {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录查看推荐')
    return
  }

  guessYouLikeLoading.value = true
  try {
    const res = await getGuessYouLike({ limit: 12 })
    if (res.code === 200) {
      guessYouLikeSongs.value = res.data || []
      await loadFavoriteStatus()
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (error) {
    console.error('加载猜你喜欢失败:', error)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    guessYouLikeLoading.value = false
  }
}

// 加载个性化推荐
const loadPersonalized = async () => {
  if (!userStore.isLogin) {
    return
  }

  personalizedLoading.value = true
  try {
    const res = await getPersonalizedSongs({ limit: 20 })
    if (res.code === 200) {
      personalizedSongs.value = res.data || []
      await loadFavoriteStatus()
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (error) {
    console.error('加载个性化推荐失败:', error)
  } finally {
    personalizedLoading.value = false
  }
}

// 加载推荐歌单
const loadRecommendedPlaylists = async () => {
  if (!userStore.isLogin) {
    return
  }

  playlistsLoading.value = true
  try {
    const res = await getRecommendedPlaylists({ limit: 12 })
    if (res.code === 200) {
      recommendedPlaylists.value = res.data || []
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (error) {
    console.error('加载推荐歌单失败:', error)
  } finally {
    playlistsLoading.value = false
  }
}

// 加载收藏状态
const loadFavoriteStatus = async () => {
  const allSongs = [...guessYouLikeSongs.value, ...personalizedSongs.value]
  if (allSongs.length === 0) return

  try {
    const songIds = allSongs.map(song => song.id)
    const res = await batchCheckFavorites(songIds)
    if (res.code === 200) {
      favoriteSongs.value = res.data || {}
    }
  } catch (error) {
    console.error('加载收藏状态失败:', error)
  }
}

// ========== 刷新操作 ==========

const refreshGuessYouLike = () => {
  loadGuessYouLike()
}

const refreshPersonalized = () => {
  loadPersonalized()
}

const refreshPlaylists = () => {
  loadRecommendedPlaylists()
}

// ========== 歌曲操作 ==========

// 播放歌曲
const handlePlaySong = (song) => {
  const allSongs = [...guessYouLikeSongs.value, ...personalizedSongs.value]
  playerStore.play(song, allSongs)
}

// 添加到播放列表
const handleAddToPlaylist = (song) => {
  playerStore.addToPlaylist(song)
  ElMessage.success(`已添加: ${song.title}`)
}

// 切换收藏
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
  // 可以做一些额外处理
}

// 跳转到歌单详情
const goToPlaylist = (playlistId) => {
  router.push(`/playlist/${playlistId}`)
}

// ========== 工具函数 ==========

const getArtistNames = (song) => {
  return song.artists?.map(a => a.name).join(' / ') || '未知歌手'
}

const formatCount = (count) => {
  if (!count) return '0'
  if (count >= 100000000) return (count / 100000000).toFixed(1) + '亿'
  if (count >= 10000) return (count / 10000).toFixed(1) + '万'
  return count.toString()
}

const formatDuration = (seconds) => {
  if (!seconds) return '--:--'
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}
</script>

<style scoped>
.discover-page {
  padding-bottom: 30px;
}

/* ========== 页面标题 ========== */
.page-header {
  margin-bottom: 30px;
  text-align: center;
  padding: 30px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  color: white;
}

.page-header h2 {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 10px;
}

.subtitle {
  font-size: 16px;
  opacity: 0.9;
}

/* ========== 推荐区块 ========== */
.recommendation-section {
  margin-bottom: 40px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-icon {
  font-size: 28px;
  color: #667eea;
}

.title-group h3 {
  font-size: 22px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.section-desc {
  font-size: 14px;
  color: #999;
  margin-left: 10px;
}

/* ========== 歌曲网格（猜你喜欢） ========== */
.song-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.song-card {
  background: white;
  border-radius: 12px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  position: relative;
}

.song-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(102, 126, 234, 0.2);
}

.song-cover-wrapper {
  position: relative;
  width: 100%;
  padding-bottom: 100%;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 12px;
}

.song-cover {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.song-card:hover .song-cover {
  transform: scale(1.1);
}

.cover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.song-card:hover .cover-overlay {
  opacity: 1;
}

.play-icon {
  font-size: 50px;
  color: white;
}

.song-play-count {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.song-info {
  text-align: center;
}

.song-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-artist {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-actions-hover {
  display: none;
  position: absolute;
  bottom: 15px;
  left: 50%;
  transform: translateX(-50%);
  gap: 8px;
  background: rgba(255, 255, 255, 0.95);
  padding: 8px;
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.song-card:hover .song-actions-hover {
  display: flex;
}

/* ========== 歌曲列表（个性化推荐） ========== */
.song-list {
  background: white;
  border-radius: 12px;
  padding: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.song-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.3s;
  gap: 15px;
  cursor: pointer;
}

.song-item:hover {
  background: linear-gradient(135deg, #f5f7fa 0%, #ecf0f5 100%);
  transform: translateX(5px);
}

.song-index {
  width: 30px;
  text-align: center;
  color: #999;
  font-size: 14px;
  font-weight: 600;
}

.song-cover-small {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.song-info-inline {
  flex: 1;
  min-width: 0;
}

.song-name {
  font-size: 15px;
  color: #333;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.song-artist-name {
  font-size: 13px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-stats {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #999;
  font-size: 13px;
  margin-right: 20px;
}

.song-duration {
  font-size: 13px;
  color: #999;
  margin-right: 20px;
}

.song-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.song-item:hover .song-actions {
  opacity: 1;
}

/* ========== 歌单网格 ========== */
.playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.playlist-card {
  background: white;
  border-radius: 12px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.playlist-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(102, 126, 234, 0.2);
}

.playlist-cover-wrapper {
  position: relative;
  width: 100%;
  padding-bottom: 100%;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 12px;
}

.playlist-cover {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.playlist-card:hover .playlist-cover {
  transform: scale(1.1);
}

.playlist-song-count {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.playlist-info {
  text-align: center;
}

.playlist-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playlist-creator {
  font-size: 12px;
  color: #999;
}

/* ========== 空状态 ========== */
.empty-hint {
  padding: 60px 20px;
  text-align: center;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .page-header h2 {
    font-size: 24px;
  }

  .song-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 15px;
  }

  .playlist-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 15px;
  }

  .song-stats,
  .song-duration {
    display: none;
  }
}
</style>

