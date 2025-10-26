<template>
  <div class="search-page">
    <!-- 搜索框 -->
    <div class="search-header">
      <div class="search-container">
        <!-- 搜索类型选择 -->
        <el-segmented 
          v-model="searchType" 
          :options="searchTypeOptions" 
          size="large"
          class="search-type-selector"
        />
        
        <!-- 搜索输入框 -->
        <el-input
          v-model="keyword"
          :placeholder="searchPlaceholder"
          clearable
          size="large"
          class="search-input"
          @input="handleSearch"
          @keyup.enter="performSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </div>

    <!-- 初始状态：热门搜索 + 搜索历史 -->
    <div v-if="!keyword && !hasSearched" class="initial-state">
      <!-- 热门搜索 -->
      <div class="hot-search-section">
        <div class="section-header">
          <el-icon class="header-icon"><TrendCharts /></el-icon>
          <h3>热门搜索</h3>
        </div>
        <div class="hot-search-list">
          <div 
            v-for="(item, index) in hotSearches" 
            :key="index"
            class="hot-search-item"
            @click="handleHotClick(item)"
          >
            <span 
              class="hot-rank" 
              :class="{ 'top-three': index < 3 }"
            >
              {{ index + 1 }}
            </span>
            <span class="hot-keyword">{{ item }}</span>
            <el-icon v-if="index < 3" class="hot-icon"><Trophy /></el-icon>
          </div>
        </div>
      </div>

      <!-- 搜索历史 -->
      <div v-if="searchHistory.length > 0" class="search-history-section">
        <div class="section-header">
          <el-icon class="header-icon"><Clock /></el-icon>
          <h3>搜索历史</h3>
          <el-button 
            text 
            type="danger" 
            size="small" 
            @click="clearHistory"
          >
            清空历史
          </el-button>
        </div>
        <div class="history-list">
          <div 
            v-for="item in searchHistory" 
            :key="item.id"
            class="history-item"
            @click="handleHistoryClick(item.keyword)"
          >
            <span class="history-keyword">{{ item.keyword }}</span>
            <el-icon 
              class="delete-icon" 
              @click.stop="removeHistory(item.id)"
            >
              <Close />
            </el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading"><Loading /></el-icon>
      <p>搜索中...</p>
    </div>

    <!-- 搜索结果 - 单曲 -->
    <transition name="fade">
      <div v-if="!loading && keyword && searchType === 'songs'" class="search-results">
        <div v-if="songResults.length > 0" class="songs-container">
          <div class="result-header">
            <h3>找到 {{ totalSongs }} 首歌曲</h3>
          </div>

          <div class="song-list">
            <div 
              v-for="(song, index) in songResults" 
              :key="song.id"
              class="song-item"
              @click="handlePlaySong(song)"
            >
              <div class="song-index">{{ (currentPage - 1) * pageSize + index + 1 }}</div>
              <img :src="song.cover || defaultCover" class="song-cover">
              <div class="song-info">
                <div class="song-name" v-html="highlightKeyword(song.title)"></div>
                <div class="song-artist">
                  <template v-for="(artist, idx) in song.artists || []" :key="artist.id">
                    <span class="clickable" @click.stop="goToArtist(artist.id)" v-html="highlightKeyword(artist.name)"></span>
                    <span v-if="idx < (song.artists?.length || 0) - 1"> / </span>
                  </template>
                  <span v-if="!song.artists || song.artists.length === 0">未知歌手</span>
                </div>
              </div>
              <div class="song-duration">{{ formatDuration(song.duration) }}</div>
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
    
          <!-- 分页 -->
          <el-pagination
            v-if="totalSongs > pageSize"
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="totalSongs"
            layout="prev, pager, next"
            class="pagination"
            @current-change="handlePageChange"
      />
    </div>
    
        <!-- 空状态 - 单曲 -->
        <div v-else class="empty-state">
          <el-empty description="没有找到相关歌曲">
            <template #image>
              <el-icon class="empty-icon"><FolderOpened /></el-icon>
            </template>
          </el-empty>
        </div>
      </div>
    </transition>

    <!-- 搜索结果 - 歌手 -->
    <transition name="fade">
      <div v-if="!loading && keyword && searchType === 'artists'" class="search-results">
        <div v-if="artistResults.length > 0" class="artists-container">
          <div class="result-header">
            <h3>找到 {{ totalArtists }} 位歌手</h3>
          </div>

          <div class="artist-grid">
            <div 
              v-for="artist in artistResults" 
              :key="artist.id"
              class="artist-card"
              @click="goToArtist(artist.id)"
            >
              <div class="artist-avatar-wrapper">
                <img :src="artist.avatar || defaultAvatar" class="artist-avatar">
                <div class="artist-overlay">
                  <el-icon class="play-icon"><CaretRight /></el-icon>
                </div>
              </div>
              <div class="artist-info">
                <div class="artist-name" v-html="highlightKeyword(artist.name)"></div>
                <div class="artist-region">{{ artist.region || '未知地区' }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 - 歌手 -->
        <div v-else class="empty-state">
          <el-empty description="没有找到相关歌手">
            <template #image>
              <el-icon class="empty-icon"><UserFilled /></el-icon>
            </template>
      </el-empty>
    </div>
      </div>
    </transition>
    
    <!-- 添加到歌单对话框 -->
    <PlaylistSelector 
      v-model="playlistSelectorVisible" 
      :song-id="selectedSongId"
      @success="handleAddSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { usePlayerStore } from '@/store/player'
import { useUserStore } from '@/store/user'
import { searchSongs } from '@/api/song'
import { getArtists } from '@/api/artist'
import { 
  getSearchHistory, 
  addSearchHistory, 
  clearSearchHistory as clearSearchHistoryAPI,
  deleteSearchHistory,
  getHotSearchKeywords 
} from '@/api/searchHistory'
import { toggleFavorite, batchCheckFavorites } from '@/api/favorite'
import { ElMessage } from 'element-plus'
import { 
  Search, Loading, FolderOpened, Headset, User, 
  TrendCharts, Clock, Trophy, Close, CaretRight, UserFilled 
} from '@element-plus/icons-vue'
import PlaylistSelector from '@/components/PlaylistSelector.vue'

const router = useRouter()
const playerStore = usePlayerStore()
const userStore = useUserStore()

// 搜索相关
const keyword = ref('')
const searchType = ref('songs') // 'songs' | 'artists' - 搜索类型
const loading = ref(false)
const hasSearched = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)

// 搜索类型选项
const searchTypeOptions = [
  {
    label: '🎵 单曲',
    value: 'songs'
  },
  {
    label: '👤 歌手',
    value: 'artists'
  }
]

// 单曲搜索结果
const songResults = ref([])
const totalSongs = ref(0)

// 歌手搜索结果
const artistResults = ref([])
const totalArtists = ref(0)
const allArtists = ref([]) // 缓存所有歌手

const defaultCover = 'https://via.placeholder.com/60?text=Music'
const defaultAvatar = 'https://via.placeholder.com/200?text=Artist'

// 热门搜索（从后端获取）
const hotSearches = ref([])

// 搜索历史（从数据库获取）
const searchHistory = ref([])

// 收藏状态
const favoriteSongs = ref({}) // { songId: true/false }

// 歌单选择器
const playlistSelectorVisible = ref(false)
const selectedSongId = ref(null)

// 防抖定时器
let searchTimer = null

// 计算属性：搜索框占位符
const searchPlaceholder = computed(() => {
  return searchType.value === 'songs' ? '搜索歌曲名称' : '搜索歌手名称'
})

// ========== 初始化 ==========
onMounted(() => {
  loadHotSearches()
  loadSearchHistory()
  loadAllArtists()
})

// 加载热门搜索
const loadHotSearches = async () => {
  try {
    const res = await getHotSearchKeywords(10)
    if (res.code === 200) {
      hotSearches.value = res.data || []
    }
  } catch (error) {
    console.error('加载热门搜索失败:', error)
    // 失败时使用默认数据
    hotSearches.value = ['周杰伦', '七里香', '林俊杰', '薛之谦', '演员']
  }
}

// 加载搜索历史（从数据库）
const loadSearchHistory = async () => {
  try {
    const res = await getSearchHistory()
    if (res.code === 200) {
      searchHistory.value = res.data || []
    }
  } catch (error) {
    console.error('加载搜索历史失败:', error)
    searchHistory.value = []
  }
}

// 保存搜索历史（到数据库）
const saveSearchHistory = async (keyword) => {
  if (!keyword.trim()) return

  try {
    // 调用后端 API 添加搜索记录
    await addSearchHistory(keyword)
    
    // 重新加载搜索历史（后端会自动去重和排序）
    await loadSearchHistory()
  } catch (error) {
    console.error('保存搜索历史失败:', error)
  }
}

// 清空搜索历史（数据库）
const clearHistory = async () => {
  try {
    const res = await clearSearchHistoryAPI()
    if (res.code === 200) {
      searchHistory.value = []
      ElMessage.success('已清空搜索历史')
    } else {
      ElMessage.error(res.message || '清空失败')
    }
  } catch (error) {
    console.error('清空搜索历史失败:', error)
    ElMessage.error('清空失败，请稍后重试')
  }
}

// 删除单条历史（数据库）
const removeHistory = async (historyId) => {
  try {
    const res = await deleteSearchHistory(historyId)
    if (res.code === 200) {
      // 从本地数组中移除
      searchHistory.value = searchHistory.value.filter(item => item.id !== historyId)
      ElMessage.success('已删除')
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    console.error('删除搜索历史失败:', error)
    ElMessage.error('删除失败，请稍后重试')
  }
}

// 加载所有歌手（用于前端过滤）
const loadAllArtists = async () => {
  try {
    const res = await getArtists({ page: 0, size: 100 }) // 获取前100个歌手
    if (res.code === 200) {
      allArtists.value = res.data.content || []
    }
  } catch (error) {
    console.error('加载歌手列表失败:', error)
  }
}

// ========== 搜索类型切换 ==========
// 当搜索类型改变时，如果已经有搜索结果，重新搜索
watch(searchType, () => {
  if (keyword.value.trim() && hasSearched.value) {
    currentPage.value = 1
    performSearch()
  }
})

// ========== 搜索功能 ==========

// 执行搜索
const performSearch = async () => {
  if (!keyword.value.trim()) {
    songResults.value = []
    artistResults.value = []
    hasSearched.value = false
    return
  }

  loading.value = true
  hasSearched.value = true

  // 保存搜索历史
  saveSearchHistory(keyword.value.trim())

  try {
    if (searchType.value === 'songs') {
      await searchSongsData()
    } else {
      await searchArtistsData()
    }
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 搜索歌曲
const searchSongsData = async () => {
  try {
    const res = await searchSongs({
      keyword: keyword.value.trim(),
      page: currentPage.value - 1,
      size: pageSize.value
    })

    if (res.code === 200) {
      songResults.value = res.data.content || []
      totalSongs.value = res.data.totalElements || 0
      // 加载收藏状态
      await loadFavoriteStatus()
    }
  } catch (error) {
    console.error('搜索歌曲失败:', error)
    throw error
  }
}

// 加载收藏状态
const loadFavoriteStatus = async () => {
  if (!userStore.isLogin || songResults.value.length === 0) return
  
  try {
    const songIds = songResults.value.map(song => song.id)
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

// 搜索歌手（前端过滤）
const searchArtistsData = async () => {
  try {
    const kw = keyword.value.trim().toLowerCase()
    const filtered = allArtists.value.filter(artist => 
      artist.name.toLowerCase().includes(kw) ||
      (artist.region && artist.region.toLowerCase().includes(kw))
    )
    
    artistResults.value = filtered
    totalArtists.value = filtered.length
  } catch (error) {
    console.error('搜索歌手失败:', error)
    throw error
  }
}

// 防抖搜索
const handleSearch = () => {
  if (!keyword.value.trim()) {
    songResults.value = []
    artistResults.value = []
    hasSearched.value = false
    totalSongs.value = 0
    totalArtists.value = 0
    return
  }

  // 清除之前的定时器
  if (searchTimer) {
    clearTimeout(searchTimer)
  }

  // 设置新的定时器（500ms 后执行搜索）
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    performSearch()
  }, 500)
}

// 点击热门搜索
const handleHotClick = (keyword_text) => {
  keyword.value = keyword_text
  searchType.value = 'songs' // 默认搜索单曲
  performSearch()
}

// 点击搜索历史
const handleHistoryClick = (keyword_text) => {
  keyword.value = keyword_text
  searchType.value = 'songs' // 默认搜索单曲
  performSearch()
}

// ========== 歌曲操作 ==========

// 播放歌曲
const handlePlaySong = (song) => {
  playerStore.play(song, songResults.value)
}

// 添加到播放列表
const handleAddToPlaylist = (song) => {
  playerStore.addToPlaylist(song)
  ElMessage.success(`已添加到播放列表: ${song.title}`)
}

// ========== 工具函数 ==========

// 格式化时长
const formatDuration = (seconds) => {
  if (!seconds) return '--:--'
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 高亮关键词
const highlightKeyword = (text) => {
  if (!keyword.value || !text) return text
  const regex = new RegExp(`(${keyword.value})`, 'gi')
  return text.replace(regex, '<span class="highlight">$1</span>')
}

// 分页切换
const handlePageChange = () => {
  performSearch()
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 跳转到歌手详情页
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
</script>

<style scoped>
.search-page {
  min-height: calc(100vh - 160px);
  padding-bottom: 20px;
}

/* ========== 搜索框 ========== */
.search-header {
  margin-bottom: 30px;
  position: sticky;
  top: 0;
  background: #f8f9fa;
  padding: 20px 0;
  z-index: 10;
}

.search-container {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  gap: 15px;
  align-items: center;
}

.search-type-selector {
  flex-shrink: 0;
}

.search-type-selector :deep(.el-segmented__item) {
  padding: 8px 20px;
  font-size: 14px;
  font-weight: 500;
}

.search-type-selector :deep(.el-segmented__item-selected) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.search-input {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  border-radius: 25px;
  padding: 5px 15px;
}

.search-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.3);
}

/* ========== 初始状态 ========== */
.initial-state {
  max-width: 800px;
  margin: 0 auto;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  position: relative;
}

.section-header h3 {
  font-size: 18px;
  color: #333;
  font-weight: 600;
  flex: 1;
}

.header-icon {
  font-size: 24px;
  color: #409eff;
}

/* ========== 热门搜索 ========== */
.hot-search-section {
  background: white;
  padding: 25px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 25px;
}

.hot-search-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.hot-search-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 15px;
  background: #f8f9fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.hot-search-item:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  transform: translateX(5px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.hot-rank {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ddd;
  border-radius: 6px;
  font-size: 14px;
  font-weight: bold;
  color: #666;
  transition: all 0.3s;
}

.hot-rank.top-three {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(245, 87, 108, 0.3);
}

.hot-search-item:hover .hot-rank {
  background: rgba(255, 255, 255, 0.3);
  color: white;
}

.hot-keyword {
  flex: 1;
  font-size: 14px;
  color: #333;
  transition: color 0.3s;
}

.hot-search-item:hover .hot-keyword {
  color: white;
}

.hot-icon {
  font-size: 18px;
  color: #fbbf24;
  transition: all 0.3s;
}

.hot-search-item:hover .hot-icon {
  color: white;
  transform: rotate(15deg);
}

/* ========== 搜索历史 ========== */
.search-history-section {
  background: white;
  padding: 25px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.history-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #f0f2f5;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.history-item:hover {
  background: #409eff;
  color: white;
  transform: translateY(-2px);
}

.history-keyword {
  font-size: 14px;
  color: #606266;
  transition: color 0.3s;
}

.history-item:hover .history-keyword {
  color: white;
}

.delete-icon {
  font-size: 14px;
  color: #999;
  transition: all 0.3s;
}

.history-item:hover .delete-icon {
  color: white;
  transform: rotate(90deg);
}

/* ========== 加载状态 ========== */
.loading-state {
  text-align: center;
  padding: 80px 20px;
  color: #909399;
}

.loading-state .el-icon {
  font-size: 50px;
  margin-bottom: 15px;
  color: #409eff;
}

/* ========== 搜索结果 ========== */
.search-results {
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.result-header {
  margin-bottom: 20px;
}

.result-header h3 {
  font-size: 18px;
  color: #333;
  font-weight: 600;
}

/* ========== 单曲列表 ========== */
.songs-container {
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

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

.song-cover {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
}

.song-item:hover .song-cover {
  transform: scale(1.05);
}

.song-info {
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

.song-artist {
  font-size: 13px;
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

/* 高亮关键词 */
.song-name :deep(.highlight),
.song-artist :deep(.highlight),
.artist-name :deep(.highlight) {
  color: #409eff;
  font-weight: bold;
  background: linear-gradient(135deg, #ecf5ff 0%, #e3f2fd 100%);
  padding: 2px 6px;
  border-radius: 4px;
}

.song-duration {
  font-size: 13px;
  color: #999;
  margin-right: 20px;
  font-weight: 500;
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

/* ========== 歌手网格 ========== */
.artists-container {
  animation: fadeIn 0.4s ease-out;
}

.artist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.artist-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.artist-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 12px 24px rgba(102, 126, 234, 0.2);
}

.artist-avatar-wrapper {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 0 auto 15px;
  border-radius: 50%;
  overflow: hidden;
}

.artist-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.artist-card:hover .artist-avatar {
  transform: scale(1.1);
}

.artist-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.artist-card:hover .artist-overlay {
  opacity: 1;
}

.play-icon {
  font-size: 50px;
  color: white;
}

.artist-info {
  text-align: center;
}

.artist-name {
  font-size: 16px;
  color: #333;
  margin-bottom: 6px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.artist-region {
  font-size: 13px;
  color: #999;
}

/* ========== 分页 ========== */
.pagination {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

/* ========== 空状态 ========== */
.empty-state {
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 100px;
  color: #dcdfe6;
}

/* ========== 过渡动画 ========== */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .search-container {
    flex-direction: column;
    gap: 10px;
  }

  .search-type-selector {
    width: 100%;
  }

  .search-type-selector :deep(.el-segmented) {
    width: 100%;
  }

  .hot-search-list {
    grid-template-columns: 1fr;
  }

  .artist-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 15px;
  }

  .artist-avatar-wrapper {
    width: 100px;
    height: 100px;
  }

  .song-duration {
    display: none;
  }

  .song-index {
    font-size: 12px;
  }
}
</style>
