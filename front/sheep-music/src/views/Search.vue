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
    <div
      v-if="!keyword && !hasSearched"
      class="initial-state"
    >
      <!-- 热门搜索 -->
      <div class="hot-search-section">
        <div class="section-header">
          <el-icon class="header-icon">
            <TrendCharts />
          </el-icon>
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
            <el-icon
              v-if="index < 3"
              class="hot-icon"
            >
              <Trophy />
            </el-icon>
          </div>
        </div>
      </div>

      <!-- 搜索历史 -->
      <div
        v-if="searchHistory.length > 0"
        class="search-history-section"
      >
        <div class="section-header">
          <el-icon class="header-icon">
            <Clock />
          </el-icon>
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
    <div
      v-if="loading"
      class="loading-state"
    >
      <el-icon class="is-loading">
        <Loading />
      </el-icon>
      <p>搜索中...</p>
    </div>

    <!-- 搜索结果 - 单曲（按来源分区：本地曲库 + 开放曲库，曲库供应链 v1） -->
    <transition name="fade">
      <div
        v-if="!loading && keyword && searchType === 'songs'"
        class="search-results"
      >
        <!-- 分区一：本地曲库 -->
        <div class="source-section">
          <div class="result-header source-header">
            <el-icon class="header-icon">
              <Headset />
            </el-icon>
            <h3>本地曲库</h3>
            <span class="source-count">{{ totalSongs }} 首</span>
          </div>

          <div
            v-if="songResults.length > 0"
            class="songs-container"
          >
            <div class="song-list">
              <div
                v-for="(song, index) in songResults"
                :key="song.id"
                class="song-item"
                @click="handlePlaySong(song)"
              >
                <div class="song-index">
                  {{ (currentPage - 1) * pageSize + index + 1 }}
                </div>
                <img
                  :src="ossThumb(song.cover, 200) || defaultCover"
                  class="song-cover"
                >
                <div class="song-info">
                  <div
                    class="song-name"
                    v-html="highlightKeyword(song.title)"
                  />
                  <div class="song-artist">
                    <template
                      v-for="(artist, idx) in song.artists || []"
                      :key="artist.id"
                    >
                      <span
                        class="clickable"
                        @click.stop="goToArtist(artist.id)"
                        v-html="highlightKeyword(artist.name)"
                      />
                      <span v-if="idx < (song.artists?.length || 0) - 1"> / </span>
                    </template>
                    <span v-if="!song.artists || song.artists.length === 0">未知歌手</span>
                  </div>
                </div>
                <div class="song-duration">
                  {{ formatDuration(song.duration) }}
                </div>
                <div class="song-actions">
                  <el-button
                    icon="CaretRight"
                    circle
                    size="small"
                    title="播放"
                    @click.stop="handlePlaySong(song)"
                  />
                  <el-button
                    icon="Plus"
                    circle
                    size="small"
                    title="添加到播放列表"
                    @click.stop="handleAddToPlaylist(song)"
                  />
                  <el-button
                    icon="FolderAdd"
                    circle
                    size="small"
                    title="添加到歌单"
                    @click.stop="showAddToPlaylistDialog(song.id)"
                  />
                  <el-button
                    :icon="favoriteSongs[song.id] ? 'StarFilled' : 'Star'"
                    circle
                    size="small"
                    :type="favoriteSongs[song.id] ? 'danger' : ''"
                    title="收藏"
                    @click.stop="handleToggleFavorite(song.id)"
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

          <!-- 空状态 - 本地曲库 -->
          <div
            v-else
            class="empty-state"
          >
            <el-empty description="本地曲库中没有找到相关歌曲">
              <template #image>
                <el-icon class="empty-icon">
                  <FolderOpened />
                </el-icon>
              </template>
            </el-empty>
          </div>
        </div>

        <!-- 分区二+：开放曲库（每个已启用音源一区，CC 授权，试听不入库） -->
        <div
          v-for="section in extSections"
          :key="section.source"
          class="source-section"
        >
          <div class="result-header source-header">
            <el-icon class="header-icon">
              <Connection />
            </el-icon>
            <h3>{{ section.label }}</h3>
            <el-tag
              v-if="section.enabled && section.openLicense"
              size="small"
              type="success"
              effect="plain"
            >
              CC 授权
            </el-tag>
            <el-tag
              v-else-if="section.enabled"
              size="small"
              type="warning"
              effect="plain"
            >
              聚合试听
            </el-tag>
            <el-tag
              v-else
              size="small"
              type="info"
              effect="plain"
            >
              未启用
            </el-tag>
            <span
              v-if="section.enabled && section.total > 0"
              class="source-count"
            >
              {{ section.total }} 首
            </span>
          </div>

          <div
            v-if="section.loading"
            class="ext-tip"
          >
            <el-icon class="is-loading">
              <Loading />
            </el-icon>
            正在搜索{{ section.label }}...
          </div>

          <template v-else-if="section.enabled">
            <div
              v-if="section.songs.length > 0"
              class="song-list"
            >
              <div
                v-for="song in section.songs"
                :key="song.id"
                class="song-item"
                @click="handlePlayExternal(section, song)"
              >
                <div class="song-index ext-index">
                  <el-icon><Connection /></el-icon>
                </div>
                <img
                  :src="song.cover || defaultCover"
                  class="song-cover"
                  @error="song.cover = ''"
                >
                <div class="song-info">
                  <div
                    class="song-name"
                    v-html="highlightKeyword(song.title)"
                  />
                  <div
                    class="song-artist"
                    v-html="highlightKeyword(song.artists[0]?.name || '未知歌手')"
                  />
                </div>
                <a
                  v-if="song.licenseUrl"
                  :href="song.licenseUrl"
                  target="_blank"
                  rel="noopener"
                  class="license-badge"
                  :title="'授权协议：' + (song.licenseName || 'CC')"
                  @click.stop
                >{{ song.licenseName || 'CC' }}</a>
                <div class="song-duration">
                  {{ formatDuration(song.duration) }}
                </div>
                <div class="song-actions">
                  <el-button
                    icon="CaretRight"
                    circle
                    size="small"
                    title="播放"
                    @click.stop="handlePlayExternal(section, song)"
                  />
                  <el-button
                    icon="Plus"
                    circle
                    size="small"
                    title="添加到播放列表"
                    @click.stop="handleAddExternal(section, song)"
                  />
                </div>
              </div>
            </div>

            <div
              v-else-if="section.message"
              class="ext-tip"
            >
              {{ section.message }}
            </div>
            <div
              v-else
              class="ext-tip"
            >
              {{ section.label }}中没有找到相关歌曲
            </div>

            <el-pagination
              v-if="section.total > extPageSize"
              v-model:current-page="section.page"
              :page-size="extPageSize"
              :total="section.total"
              layout="prev, pager, next"
              class="pagination"
              @current-change="handleExtPageChange(section)"
            />
          </template>

          <div
            v-else
            class="ext-tip"
          >
            {{ section.message || '音源未启用' }}
          </div>
        </div>
      </div>
    </transition>

    <!-- 搜索结果 - 歌手 -->
    <transition name="fade">
      <div
        v-if="!loading && keyword && searchType === 'artists'"
        class="search-results"
      >
        <div
          v-if="artistResults.length > 0"
          class="artists-container"
        >
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
                <img
                  :src="ossThumb(artist.avatar, 100) || defaultAvatar"
                  class="artist-avatar"
                >
                <div class="artist-overlay">
                  <el-icon class="play-icon">
                    <CaretRight />
                  </el-icon>
                </div>
              </div>
              <div class="artist-info">
                <div
                  class="artist-name"
                  v-html="highlightKeyword(artist.name)"
                />
                <div class="artist-region">
                  {{ artist.region || '未知地区' }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 - 歌手 -->
        <div
          v-else
          class="empty-state"
        >
          <el-empty description="没有找到相关歌手">
            <template #image>
              <el-icon class="empty-icon">
                <UserFilled />
              </el-icon>
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
import { ossThumb } from '@/utils/image'
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { usePlayerStore } from '@/store/player'
import { useUserStore } from '@/store/user'
import { searchSongs } from '@/api/song'
import { searchExternalSongs, getExternalSources, getExternalCover } from '@/api/externalMusic'
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
  Search, Loading, FolderOpened, Headset, User, Connection,
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
    label: '单曲',
    value: 'songs'
  },
  {
    label: '歌手',
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

// 开放曲库（外源）搜索——按音源分区，独立 loading 旁路加载，不阻塞本地结果（曲库供应链 v1）
// section 结构：{ source, label, enabled, message, total, songs, page, loading }
const extSections = ref([])
const extPageSize = ref(20)
let extSourcesPromise = null

const loadSources = () => {
  if (extSourcesPromise) return extSourcesPromise
  extSourcesPromise = getExternalSources()
    .then(res => {
      extSections.value = (res.code === 200 ? res.data || [] : []).map(s => ({
        source: s.source,
        label: s.label,
        enabled: s.enabled,
        openLicense: s.openLicense,
        message: '',
        total: 0,
        songs: [],
        page: 1,
        loading: false
      }))
    })
    .catch(error => {
      console.error('加载音源列表失败:', error)
      extSections.value = []
    })
  return extSourcesPromise
}

  const defaultCover = '/default-cover.svg'
  const defaultAvatar = '/default-artist.svg'

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
// 组件卸载时清掉未触发的防抖定时器，避免卸载后仍发起请求
onUnmounted(() => {
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }
})

onMounted(() => {
  loadHotSearches()
  loadSearchHistory()
  loadAllArtists()
  loadSources()
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
    resetExternal()
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
      // 开放曲库异步旁路搜索：先确保音源分区就绪，再并行搜索各源
      loadSources().then(() => searchExternalData())
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

// ========== 开放曲库（外源）搜索 ==========

// 重置外源分区状态（保留分区骨架）
const resetExternal = () => {
  extSections.value.forEach(s => {
    s.songs = []
    s.total = 0
    s.page = 1
    s.message = ''
    s.loading = false
  })
}

// 后端 VO → player 歌曲对象（外源歌曲写操作不适用，见规格 FR-5）
const mapExternalSong = (vo, source) => ({
  id: `ext:${source}:${vo.sourceTrackId}`,
  title: vo.title,
  artists: [{ name: vo.artist }],
  cover: vo.cover,
  duration: vo.duration,
  url: vo.streamUrl,
  lyric: '',
  isExternal: true,
  source,
  sourceTrackId: vo.sourceTrackId,
  licenseName: vo.licenseName,
  licenseUrl: vo.licenseUrl
})

// 搜索单个音源分区（异常只影响本分区展示）
const searchSection = async (section, kw) => {
  section.loading = true
  try {
    const res = await searchExternalSongs({
      source: section.source,
      keyword: kw,
      page: section.page - 1,
      size: extPageSize.value
    })
    if (res.code === 200 && res.data) {
      section.enabled = res.data.enabled
      section.message = res.data.message || ''
      section.total = Math.max(0, res.data.total || 0)
      section.songs = (res.data.items || []).map(vo => mapExternalSong(vo, res.data.source))
      // 搜索页行不带封面：异步回填（响应式，加载完自动替换占位图）
      fillCovers(section.songs)
    } else {
      section.songs = []
      section.total = 0
      section.message = '搜索失败，请稍后重试'
    }
  } catch (error) {
    console.error(`开放曲库[${section.source}]搜索失败:`, error)
    section.songs = []
    section.total = 0
    section.message = '搜索失败，请稍后重试'
  } finally {
    section.loading = false
  }
}

// 并行搜索所有分区（旁路，不阻塞本地结果）
const searchExternalData = () => {
  const kw = keyword.value.trim()
  extSections.value.forEach(section => {
    section.page = 1
    searchSection(section, kw)
  })
}

// 外源分区翻页
const handleExtPageChange = (section) => {
  searchSection(section, keyword.value.trim())
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 播放外源歌曲（整区入队，队列内可切歌）
const handlePlayExternal = (section, song) => {
  playerStore.play(song, section.songs)
}

// 外源歌曲加入播放队列
const handleAddExternal = (section, song) => {
  playerStore.addToPlaylist(song)
  ElMessage.success(`已添加到播放列表: ${song.title}`)
}

// 外源列表封面异步回填：搜索页行本身不带封面，逐首向后端解析（后端 5 分钟页面缓存，3 并发防打爆上游）
const fillCovers = (songs) => {
  const queue = songs.filter(s => !s.cover && s.source && s.sourceTrackId)
  const worker = async () => {
    while (queue.length) {
      const song = queue.shift()
      try {
        const res = await getExternalCover({ source: song.source, trackId: song.sourceTrackId })
        if (res.code === 200 && res.data) song.cover = res.data
      } catch (error) {
        // 单曲封面失败保持占位图
      }
    }
  }
  for (let i = 0; i < 3 && i < queue.length; i++) worker()
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
    resetExternal()
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
const escapeHtml = (str) => String(str)
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#39;')

const escapeRegExp = (str) => String(str).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const highlightKeyword = (text) => {
  if (!keyword.value || !text) return escapeHtml(text ?? '')
  // 关键词做正则转义（否则输入 "(" 等元字符直接抛异常，渲染崩溃）、
  // 文本做 HTML 转义（v-html 存在 XSS 注入风险）
  const regex = new RegExp(`(${escapeRegExp(keyword.value)})`, 'gi')
  return escapeHtml(text).replace(regex, '<span class="highlight">$1</span>')
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
  background: var(--bg-glass);
  backdrop-filter: var(--glass-blur);
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
  background: var(--gradient-primary);
  color: #161812;
}

.search-input {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  box-shadow: var(--shadow-sm);
  border-radius: 25px;
  padding: 5px 15px;
}

.search-input :deep(.el-input__wrapper:hover) {
  box-shadow: var(--shadow-glow);
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
  color: var(--text-primary);
  font-weight: 600;
  flex: 1;
}

.header-icon {
  font-size: 24px;
  color: var(--color-primary-light);
}

/* ========== 热门搜索 ========== */
.hot-search-section {
  background: var(--surface-raised);
  border: 1px solid var(--border-color-light);
  padding: 25px;
  border-radius: var(--radius-xl);
  box-shadow: none;
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
  background: var(--bg-tertiary);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.3s;
}

.hot-search-item:hover {
  background: var(--surface-elevated);
  border-color: var(--border-strong);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.hot-rank {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: bold;
  color: var(--text-secondary);
  transition: all 0.3s;
}

.hot-rank.top-three {
  background: var(--color-accent);
  color: #fff;
  box-shadow: none;
}

.hot-search-item:hover .hot-rank {
  background: var(--color-accent);
  color: #fff;
}

.hot-keyword {
  flex: 1;
  font-size: 14px;
  color: var(--text-primary);
  transition: color 0.3s;
}

.hot-search-item:hover .hot-keyword {
  color: var(--text-primary);
}

.hot-icon {
  font-size: 18px;
  color: var(--color-accent);
  transition: all 0.3s;
}

.hot-search-item:hover .hot-icon {
  color: var(--color-accent);
  transform: rotate(15deg);
}

/* ========== 搜索历史 ========== */
.search-history-section {
  background: var(--surface-raised);
  border: 1px solid var(--border-color-light);
  padding: 25px;
  border-radius: var(--radius-xl);
  box-shadow: none;
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
  background: var(--surface-elevated);
  border: 1px solid var(--border-color-light);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all 0.3s;
}

.history-item:hover {
  background: var(--gradient-primary);
  border-color: transparent;
  transform: translateY(-2px);
}

.history-keyword {
  font-size: 14px;
  color: var(--text-secondary);
  transition: color 0.3s;
}

.history-item:hover .history-keyword {
  color: #161812;
}

.delete-icon {
  font-size: 14px;
  color: var(--text-tertiary);
  transition: all 0.3s;
}

.history-item:hover .delete-icon {
  color: #161812;
  transform: rotate(90deg);
}

/* ========== 加载状态 ========== */
.loading-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--text-secondary);
}

.loading-state .el-icon {
  font-size: 50px;
  margin-bottom: 15px;
  color: var(--color-primary-light);
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
  color: var(--text-primary);
  font-weight: 600;
}

/* ========== 来源分区（曲库供应链 v1） ========== */
.source-section {
  margin-bottom: 30px;
}

.source-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.source-count {
  font-size: 13px;
  color: var(--text-tertiary);
  font-weight: normal;
}

.ext-tip {
  background: var(--surface-raised);
  border: 1px solid var(--border-color-light);
  border-radius: var(--radius-lg);
  padding: 24px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
  box-shadow: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.ext-index {
  color: #67c23a;
  display: flex;
  justify-content: center;
}

.license-badge {
  font-size: 12px;
  color: #67c23a;
  border: 1px solid #b3e19d;
  border-radius: 10px;
  padding: 2px 10px;
  white-space: nowrap;
  margin-right: 10px;
  text-decoration: none;
  transition: all 0.3s;
}

.license-badge:hover {
  background: #f0f9eb;
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
  background: var(--surface-raised);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 10px;
  box-shadow: none;
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
  background: var(--card-hover-bg);
}

.song-index {
  width: 30px;
  text-align: center;
  color: var(--text-tertiary);
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
  color: var(--text-primary);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.song-artist {
  font-size: 13px;
  color: var(--text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.clickable {
  cursor: pointer;
  transition: color 0.3s;
}

.clickable:hover {
  color: var(--color-primary-light);
}

/* 高亮关键词 */
.song-name :deep(.highlight),
.song-artist :deep(.highlight),
.artist-name :deep(.highlight) {
  color: var(--color-primary-dark);
  font-weight: bold;
  background: rgba(201, 255, 69, 0.16);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.song-duration {
  font-size: 13px;
  color: var(--text-tertiary);
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
  background: var(--surface-raised);
  border-radius: var(--radius-xl);
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
  color: var(--text-primary);
  margin-bottom: 6px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.artist-region {
  font-size: 13px;
  color: var(--text-tertiary);
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
  color: var(--border-strong);
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

/* ========== 响应式 - 全局样式已覆盖 ========== */
@media (max-width: 768px) {
  .search-page {
    padding: 0 0 20px;
  }
  
  .search-results {
    padding: 0 10px;
  }
  
  /* 适配Segmented组件 */
  .search-type-selector :deep(.el-segmented) {
    width: 100%;
  }
}
</style>
