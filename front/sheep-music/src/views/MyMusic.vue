<template>
  <div class="my-music">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>我的音乐</h2>
      <p class="subtitle">
        我的收藏、播放历史和歌单管理
      </p>
    </div>
    
    <!-- Tab 标签页 -->
    <el-tabs
      v-model="activeTab"
      class="music-tabs"
      @tab-click="handleTabClick"
    >
      <!-- 我的收藏 -->
      <el-tab-pane
        label="我的收藏"
        name="favorites"
      >
        <div class="tab-content">
          <!-- 收藏统计 -->
          <div class="stats-bar">
            <span class="stats-item">
              <el-icon><Star /></el-icon>
              共 {{ favoriteCount }} 首收藏
            </span>
          </div>
          
          <!-- 收藏列表 -->
          <div class="favorites-list">
            <div 
              v-for="favorite in favorites" 
              :key="favorite.id"
              class="song-item"
              @click="handlePlaySong(favorite.song)"
            >
              <img 
                :src="ossThumb(favorite.song?.cover, 200) || defaultCover" 
                alt="封面" 
                class="song-cover"
              >
              <div class="song-info">
                <div class="song-name">
                  {{ favorite.song?.title }}
                </div>
                <div class="song-artist">
                  <template
                    v-for="(artist, idx) in favorite.song?.artists || []"
                    :key="artist.id"
                  >
                    <span
                      class="clickable"
                      @click.stop="goToArtist(artist.id)"
                    >{{ artist.name }}</span>
                    <span v-if="idx < (favorite.song?.artists?.length || 0) - 1"> / </span>
                  </template>
                  <span v-if="!favorite.song?.artists || favorite.song.artists.length === 0">未知歌手</span>
                </div>
              </div>
              <div class="song-duration">
                {{ formatDuration(favorite.song?.duration) }}
              </div>
              <div class="favorite-time">
                {{ formatDate(favorite.createTime) }}
              </div>
              <div class="song-actions">
                <el-button 
                  icon="CaretRight" 
                  circle 
                  size="small" 
                  title="播放" 
                  @click.stop="handlePlaySong(favorite.song)"
                />
                <el-button 
                  icon="Plus" 
                  circle 
                  size="small" 
                  title="添加到播放列表" 
                  @click.stop="handleAddToPlaylist(favorite.song)"
                />
                <el-button 
                  icon="FolderAdd" 
                  circle 
                  size="small" 
                  title="添加到歌单" 
                  @click.stop="showAddToPlaylistDialog(favorite.song.id)"
                />
                <el-button 
                  icon="Delete" 
                  circle 
                  size="small" 
                  type="danger"
                  title="取消收藏" 
                  @click.stop="handleRemoveFavorite(favorite.song.id)"
                />
              </div>
            </div>
          </div>
          
          <!-- 空状态 -->
          <div
            v-if="favorites.length === 0 && !loading"
            class="empty-state"
          >
            <el-empty description="还没有收藏歌曲">
              <el-button
                type="primary"
                @click="$router.push('/home')"
              >
                去首页逛逛
              </el-button>
            </el-empty>
          </div>
          
          <!-- 加载状态 -->
          <div
            v-if="loading"
            class="loading-state"
          >
            <el-icon class="is-loading">
              <Loading />
            </el-icon>
            <p>加载中...</p>
          </div>
          
          <!-- 分页 -->
          <el-pagination
            v-if="favoritePagination.total > favoritePagination.size"
            v-model:current-page="favoritePagination.page"
            v-model:page-size="favoritePagination.size"
            :total="favoritePagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            class="pagination"
            @size-change="loadFavorites"
            @current-change="loadFavorites"
          />
        </div>
      </el-tab-pane>
      
      <!-- 播放历史 -->
      <el-tab-pane
        label="播放历史"
        name="history"
      >
        <div class="tab-content">
          <!-- 统计栏 -->
          <div class="stats-bar">
            <span class="stats-item">
              <el-icon><Clock /></el-icon>
              共 {{ historyCount }} 条播放记录
            </span>
            <el-button 
              v-if="playHistory.length > 0"
              type="danger" 
              size="small"
              @click="handleClearHistory"
            >
              清空历史
            </el-button>
          </div>
          
          <!-- 播放历史列表 -->
          <div class="history-list">
            <div 
              v-for="item in playHistory" 
              :key="item.id"
              class="history-item"
              @click="handlePlaySong(item.song)"
            >
              <img 
                :src="ossThumb(item.song?.cover, 200) || defaultCover" 
                alt="封面" 
                class="song-cover"
              >
              <div class="song-info">
                <div class="song-name">
                  {{ item.song?.title }}
                </div>
                <div class="song-artist">
                  <template
                    v-for="(artist, idx) in item.song?.artists || []"
                    :key="artist.id"
                  >
                    <span
                      class="clickable"
                      @click.stop="goToArtist(artist.id)"
                    >{{ artist.name }}</span>
                    <span v-if="idx < (item.song?.artists?.length || 0) - 1"> / </span>
                  </template>
                  <span v-if="!item.song?.artists || item.song.artists.length === 0">未知歌手</span>
                </div>
              </div>
              <div class="play-info">
                <div class="play-time">
                  {{ formatDateTime(item.playTime) }}
                </div>
                <div
                  v-if="item.playCount && item.playCount > 1"
                  class="play-count"
                >
                  播放 {{ item.playCount }} 次
                </div>
              </div>
              <div class="song-actions">
                <el-button 
                  icon="CaretRight" 
                  circle 
                  size="small" 
                  title="播放" 
                  @click.stop="handlePlaySong(item.song)"
                />
                <el-button 
                  icon="Plus" 
                  circle 
                  size="small" 
                  title="添加到播放列表" 
                  @click.stop="handleAddToPlaylist(item.song)"
                />
                <el-button 
                  icon="FolderAdd" 
                  circle 
                  size="small" 
                  title="添加到歌单" 
                  @click.stop="showAddToPlaylistDialog(item.song.id)"
                />
                <el-button 
                  icon="Delete" 
                  circle 
                  size="small" 
                  type="danger"
                  title="删除记录" 
                  @click.stop="handleDeleteHistory(item.id)"
                />
              </div>
            </div>
          </div>
          
          <!-- 空状态 -->
          <div
            v-if="playHistory.length === 0 && !loading"
            class="empty-state"
          >
            <el-empty description="还没有播放记录">
              <el-button
                type="primary"
                @click="$router.push('/home')"
              >
                去首页听歌
              </el-button>
            </el-empty>
          </div>
          
          <!-- 加载状态 -->
          <div
            v-if="loading"
            class="loading-state"
          >
            <el-icon class="is-loading">
              <Loading />
            </el-icon>
            <p>加载中...</p>
          </div>
    
          <!-- 分页 -->
          <el-pagination
            v-if="historyPagination.total > historyPagination.size"
            v-model:current-page="historyPagination.page"
            v-model:page-size="historyPagination.size"
            :total="historyPagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            class="pagination"
            @size-change="loadPlayHistory"
            @current-change="loadPlayHistory"
          />
        </div>
      </el-tab-pane>
      
      <!-- 我的歌单 -->
      <el-tab-pane
        label="我的歌单"
        name="playlists"
      >
        <div class="tab-content">
          <!-- 头部操作区 -->
          <div class="playlist-header">
            <div class="header-info">
              <h3>我的歌单</h3>
              <p class="count-info">
                共 {{ playlistTotal }} 个歌单
              </p>
            </div>
            <el-button
              type="primary"
              @click="showCreatePlaylistDialog"
            >
              <el-icon><Plus /></el-icon> 创建歌单
            </el-button>
          </div>

          <!-- 歌单网格 -->
          <div
            v-if="playlists.length > 0"
            class="playlist-grid"
          >
            <div 
              v-for="playlist in playlists" 
              :key="playlist.id"
              class="playlist-card"
              @click="goToPlaylistDetail(playlist.id)"
            >
              <div class="playlist-cover-wrapper">
                <PlaylistCover 
                  :cover="playlist.cover" 
                  :size="200" 
                  :default-cover="defaultPlaylistCover"
                  class="playlist-cover"
                />
                <div class="playlist-overlay">
                  <el-icon class="play-icon">
                    <CaretRight />
                  </el-icon>
                </div>
              </div>
              <div class="playlist-info">
                <h4
                  class="playlist-name"
                  :title="playlist.name"
                >
                  {{ playlist.name }}
                </h4>
                <p class="playlist-meta">
                  <span>{{ playlist.songCount || 0 }} 首歌曲</span>
                  <el-tag
                    v-if="playlist.isPublic"
                    size="small"
                    type="success"
                  >
                    公开
                  </el-tag>
                  <el-tag
                    v-else
                    size="small"
                    type="info"
                  >
                    私有
                  </el-tag>
                </p>
                <p
                  v-if="playlist.description"
                  class="playlist-desc"
                >
                  {{ playlist.description }}
                </p>
              </div>
              <div
                class="playlist-actions"
                @click.stop
              >
                <el-dropdown trigger="click">
                  <el-button
                    circle
                    size="small"
                  >
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="editPlaylist(playlist)">
                        <el-icon><Edit /></el-icon> 编辑
                      </el-dropdown-item>
                      <el-dropdown-item @click="togglePlaylistVisibility(playlist)">
                        <el-icon><View /></el-icon> {{ playlist.isPublic ? '设为私有' : '设为公开' }}
                      </el-dropdown-item>
                      <el-dropdown-item
                        divided
                        @click="confirmDeletePlaylist(playlist)"
                      >
                        <el-icon><Delete /></el-icon> 删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div
            v-else
            class="empty-state"
          >
            <el-empty description="还没有创建歌单">
              <el-button
                type="primary"
                @click="showCreatePlaylistDialog"
              >
                创建我的第一个歌单
              </el-button>
            </el-empty>
          </div>

          <!-- 分页 -->
          <el-pagination
            v-if="playlistTotal > playlistPageSize"
            v-model:current-page="playlistPage"
            :page-size="playlistPageSize"
            :total="playlistTotal"
            layout="prev, pager, next"
            class="pagination"
            @current-change="loadPlaylists"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
    
    <!-- 创建/编辑歌单对话框 -->
    <el-dialog
      v-model="playlistDialogVisible"
      :title="playlistDialogTitle"
      width="500px"
      @close="playlistDialogVisible = false"
    >
      <el-form
        :model="playlistForm"
        label-width="80px"
      >
        <el-form-item
          label="歌单名称"
          required
        >
          <el-input 
            v-model="playlistForm.name" 
            placeholder="请输入歌单名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="歌单简介">
          <el-input 
            v-model="playlistForm.description" 
            type="textarea"
            :rows="3"
            placeholder="介绍一下这个歌单吧~"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="分类">
          <el-select
            v-model="playlistForm.category"
            placeholder="选择歌单分类"
          >
            <el-option 
              v-for="cat in playlistCategories" 
              :key="cat" 
              :label="cat" 
              :value="cat"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="封面图片">
          <el-input 
            v-model="playlistForm.cover" 
            placeholder="请输入封面图片URL（选填）"
          />
        </el-form-item>
        
        <el-form-item label="是否公开">
          <el-switch 
            v-model="playlistForm.isPublic"
            active-text="公开到歌单广场"
            inactive-text="仅自己可见"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="playlistDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="savePlaylist"
          >保存</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 添加到歌单对话框 -->
    <PlaylistSelector 
      v-model="playlistSelectorVisible" 
      :song-id="selectedSongId"
      @success="handleAddSuccess"
    />
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { usePlayerStore } from '@/store/player'
import { getMyFavorites, removeFavorite, countMyFavorites } from '@/api/favorite'
import { getPlayHistoryList, clearPlayHistory, deletePlayHistory, getPlayHistoryCount } from '@/api/playHistory'
import { getMyPlaylists, createPlaylist, updatePlaylist, deletePlaylist, setPlaylistVisibility } from '@/api/playlist'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Star, Clock, Plus, CaretRight, MoreFilled, Edit, View, Delete } from '@element-plus/icons-vue'
import PlaylistCover from '@/components/PlaylistCover.vue'
import PlaylistSelector from '@/components/PlaylistSelector.vue'

import { ossThumb } from '@/utils/image'

export default {
  name: 'MyMusic',
  components: {
    PlaylistCover,
    PlaylistSelector,
    Loading,
    Star,
    Clock,
    Plus,
    CaretRight,
    MoreFilled,
    Edit,
    View,
    Delete
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const playerStore = usePlayerStore()
    
    const activeTab = ref('favorites')  // 默认显示收藏
    const favorites = ref([])
    const playHistory = ref([])  // 播放历史列表
    const loading = ref(false)
    const favoriteCount = ref(0)
    const historyCount = ref(0)  // 播放历史数量
    const defaultCover = '/default-cover.svg'
    
    const favoritePagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })
    
    const historyPagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })
    
    // 歌单相关状态
    const playlists = ref([])
    const playlistPage = ref(1)
    const playlistPageSize = ref(12)
    const playlistTotal = ref(0)
    const defaultPlaylistCover = '/default-playlist.svg'
    const playlistDialogVisible = ref(false)
    const playlistDialogTitle = ref('创建歌单')
    const playlistForm = reactive({
      id: null,
      name: '',
      description: '',
      cover: '',
      category: '',
      isPublic: false
    })
    const playlistCategories = ['流行', '摇滚', '民谣', '电子', '纯音乐', '其他']
    
    // 歌单选择器
    const playlistSelectorVisible = ref(false)
    const selectedSongId = ref(null)
    
    // 加载收藏列表
    const loadFavorites = async () => {
      loading.value = true
      try {
        const res = await getMyFavorites({
          page: favoritePagination.page - 1,
          size: favoritePagination.size
        })
        
        if (res.code === 200) {
          favorites.value = res.data.content || []
          favoritePagination.total = res.data.totalElements || 0
        }
      } catch (error) {
        console.error('加载收藏失败:', error)
        ElMessage.error('加载收藏失败')
      } finally {
        loading.value = false
      }
    }
    
    // 加载收藏数量
    const loadFavoriteCount = async () => {
      try {
        const res = await countMyFavorites()
        if (res.code === 200) {
          favoriteCount.value = res.data.count || 0
        }
      } catch (error) {
        console.error('加载收藏数量失败:', error)
      }
    }
    
    // 加载播放历史
    const loadPlayHistory = async () => {
      loading.value = true
      try {
        const res = await getPlayHistoryList({
          page: historyPagination.page - 1,
          size: historyPagination.size
        })
        
        if (res.code === 200) {
          playHistory.value = res.data.content || []
          historyPagination.total = res.data.totalElements || 0
        }
      } catch (error) {
        console.error('加载播放历史失败:', error)
        ElMessage.error('加载播放历史失败')
      } finally {
        loading.value = false
      }
    }
    
    // 加载播放历史数量
    const loadHistoryCount = async () => {
      try {
        const res = await getPlayHistoryCount()
        if (res.code === 200) {
          historyCount.value = res.data.count || 0
        }
      } catch (error) {
        console.error('加载播放历史数量失败:', error)
      }
    }
    
    // 清空播放历史
    const handleClearHistory = async () => {
      try {
        await ElMessageBox.confirm('确定要清空所有播放历史吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const res = await clearPlayHistory()
        if (res.code === 200) {
          ElMessage.success('清空成功')
          loadPlayHistory()
          loadHistoryCount()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('清空失败')
        }
      }
    }
    
    // 删除单条播放历史
    const handleDeleteHistory = async (historyId) => {
      try {
        const res = await deletePlayHistory(historyId)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          loadPlayHistory()
          loadHistoryCount()
        }
      } catch (error) {
        ElMessage.error('删除失败')
      }
    }
    
    // Tab 切换
    const handleTabClick = (tab) => {
      console.log('切换到:', tab.props.name)
      
      // 根据不同Tab加载不同数据
      if (tab.props.name === 'favorites') {
        loadFavorites()
        loadFavoriteCount()
      } else if (tab.props.name === 'history') {
        loadPlayHistory()
        loadHistoryCount()
      } else if (tab.props.name === 'playlists') {
        loadPlaylists()
      }
    }
    
    // 播放歌曲
    const handlePlaySong = (song) => {
      if (!song) return
      // 按当前 Tab 选用对应列表作为播放队列（历史 Tab 不应使用收藏列表）
      const songList = activeTab.value === 'history'
        ? playHistory.value.map(h => h.song).filter(Boolean)
        : favorites.value.map(f => f.song).filter(Boolean)
      playerStore.play(song, songList)
    }
    
    // 添加到播放列表
    const handleAddToPlaylist = (song) => {
      if (!song) return
      playerStore.addToPlaylist(song)
      ElMessage.success('已添加到播放列表')
    }
    
    // 取消收藏
    const handleRemoveFavorite = async (songId) => {
      try {
        await ElMessageBox.confirm('确定要取消收藏这首歌曲吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const res = await removeFavorite(songId)
        if (res.code === 200) {
          ElMessage.success('取消收藏成功')
          loadFavorites()
          loadFavoriteCount()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('取消收藏失败')
        }
      }
    }
    
    // 跳转到歌手详情
    const goToArtist = (artistId) => {
      if (artistId) {
        router.push(`/artist/${artistId}`)
      }
    }
    
    // 格式化时长
    const formatDuration = (seconds) => {
      if (!seconds) return '00:00'
      const mins = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${mins}:${secs.toString().padStart(2, '0')}`
    }
    
    // 格式化日期（用于收藏时间）
    const formatDate = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      const now = new Date()
      const diff = now - date
      
      if (diff < 60 * 1000) return '刚刚'
      if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`
      if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))}小时前`
      if (diff < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diff / (24 * 60 * 60 * 1000))}天前`
      return date.toLocaleDateString('zh-CN')
    }
    
    // 格式化日期时间（用于播放历史）
    const formatDateTime = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      const now = new Date()
      const diff = now - date
      
      // 今天
      if (date.toDateString() === now.toDateString()) {
        return '今天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      }
      
      // 昨天
      const yesterday = new Date(now)
      yesterday.setDate(yesterday.getDate() - 1)
      if (date.toDateString() === yesterday.toDateString()) {
        return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      }
      
      // 一周内
      if (diff < 7 * 24 * 60 * 60 * 1000) {
        const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
        return days[date.getDay()] + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      }
      
      // 更早
      return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    }
    
    // ==================== 歌单相关方法 ====================
    
    // 加载歌单列表
    const loadPlaylists = async () => {
      loading.value = true
      try {
        const res = await getMyPlaylists({
          page: playlistPage.value - 1,
          size: playlistPageSize.value
        })
        
        if (res.code === 200) {
          playlists.value = res.data.content || []
          playlistTotal.value = res.data.totalElements || 0
        }
      } catch (error) {
        console.error('加载歌单失败:', error)
        ElMessage.error('加载歌单失败')
      } finally {
        loading.value = false
      }
    }
    
    // 显示创建歌单对话框
    const showCreatePlaylistDialog = () => {
      playlistDialogTitle.value = '创建歌单'
      playlistForm.id = null
      playlistForm.name = ''
      playlistForm.description = ''
      playlistForm.cover = ''
      playlistForm.category = ''
      playlistForm.isPublic = false
      playlistDialogVisible.value = true
    }
    
    // 编辑歌单
    const editPlaylist = (playlist) => {
      playlistDialogTitle.value = '编辑歌单'
      playlistForm.id = playlist.id
      playlistForm.name = playlist.name
      playlistForm.description = playlist.description || ''
      playlistForm.cover = playlist.cover || ''
      playlistForm.category = playlist.category || ''
      playlistForm.isPublic = playlist.isPublic || false
      playlistDialogVisible.value = true
    }
    
    // 保存歌单（创建或更新）
    const savePlaylist = async () => {
      if (!playlistForm.name.trim()) {
        ElMessage.warning('请输入歌单名称')
        return
      }
      
      try {
        const data = {
          name: playlistForm.name,
          description: playlistForm.description,
          cover: playlistForm.cover,
          category: playlistForm.category,
          isPublic: playlistForm.isPublic
        }
        
        let res
        if (playlistForm.id) {
          // 更新
          res = await updatePlaylist(playlistForm.id, data)
        } else {
          // 创建
          res = await createPlaylist(data)
        }
        
        if (res.code === 200) {
          ElMessage.success(res.message || (playlistForm.id ? '更新成功' : '创建成功'))
          playlistDialogVisible.value = false
          loadPlaylists()
        }
      } catch (error) {
        ElMessage.error(playlistForm.id ? '更新失败' : '创建失败')
      }
    }
    
    // 确认删除歌单
    const confirmDeletePlaylist = async (playlist) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除歌单"${playlist.name}"吗？删除后将无法恢复！`,
          '删除确认',
          {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        // 执行删除
        await handleDeletePlaylist(playlist.id)
      } catch {
        // 用户取消
      }
    }
    
    // 删除歌单
    const handleDeletePlaylist = async (playlistId) => {
      try {
        const res = await deletePlaylist(playlistId)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          loadPlaylists()
        }
      } catch (error) {
        ElMessage.error('删除失败')
      }
    }
    
    // 切换歌单公开/私有
    const togglePlaylistVisibility = async (playlist) => {
      try {
        const res = await setPlaylistVisibility(playlist.id, !playlist.isPublic)
        if (res.code === 200) {
          ElMessage.success(res.message || '设置成功')
          loadPlaylists()
        }
      } catch (error) {
        ElMessage.error('设置失败')
      }
    }
    
    // 跳转到歌单详情
    const goToPlaylistDetail = (playlistId) => {
      router.push(`/playlist/${playlistId}`)
    }
    
    // 显示添加到歌单对话框
    const showAddToPlaylistDialog = (songId) => {
      if (!songId) return
      selectedSongId.value = songId
      playlistSelectorVisible.value = true
    }
    
    // 添加成功回调
    const handleAddSuccess = () => {
      // 可以在这里做一些额外的处理
    }
    
    // 初始化
    onMounted(() => {
      // 支持通过URL参数切换Tab
      const tab = route?.query?.tab
      if (tab && ['favorites', 'history', 'playlists'].includes(tab)) {
        activeTab.value = tab
      }

      // 按 Tab 加载对应数据（只加载 favorites 会导致 ?tab=playlists 深链进入时页面为空）
      if (activeTab.value === 'favorites') {
        loadFavorites()
        loadFavoriteCount()
      } else if (activeTab.value === 'history') {
        loadPlayHistory()
        loadHistoryCount()
      } else if (activeTab.value === 'playlists') {
        loadPlaylists()
      }
    })
    
    return {
      activeTab,
      favorites,
      playHistory,
      loading,
      favoriteCount,
      historyCount,
      favoritePagination,
      historyPagination,
      defaultCover,
      handleTabClick,
      handlePlaySong,
      handleAddToPlaylist,
      handleRemoveFavorite,
      handleClearHistory,
      handleDeleteHistory,
      goToArtist,
      formatDuration,
      formatDate,
      formatDateTime,
      loadFavorites,
      loadPlayHistory,
      // 歌单相关
      playlists,
      playlistPage,
      playlistPageSize,
      playlistTotal,
      defaultPlaylistCover,
      playlistDialogVisible,
      playlistDialogTitle,
      playlistForm,
      playlistCategories,
      loadPlaylists,
      showCreatePlaylistDialog,
      editPlaylist,
      savePlaylist,
      confirmDeletePlaylist,
      togglePlaylistVisibility,
      goToPlaylistDetail,
      // 歌单选择器
      playlistSelectorVisible,
      selectedSongId,
      showAddToPlaylistDialog,
      handleAddSuccess
    }
  }
}
</script>

<style scoped>
.my-music {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h2 {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

.music-tabs {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.tab-content {
  padding-top: 20px;
}

/* 统计栏 */
.stats-bar {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stats-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.stats-item .el-icon {
  color: #409eff;
}

/* 收藏列表 */
.favorites-list {
  margin-top: 20px;
}

.song-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  gap: 15px;
}

.song-item:hover {
  background: #f5f7fa;
}

.song-cover {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.song-info {
  flex: 1;
  min-width: 0;
}

.song-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-artist {
  font-size: 14px;
  color: #666;
}

.clickable {
  cursor: pointer;
  transition: color 0.3s;
}

.clickable:hover {
  color: #409eff;
}

.song-duration {
  width: 60px;
  text-align: center;
  color: #999;
  font-size: 14px;
  flex-shrink: 0;
}

.favorite-time {
  width: 100px;
  text-align: center;
  color: #999;
  font-size: 12px;
  flex-shrink: 0;
}

.song-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
  flex-shrink: 0;
}

.song-item:hover .song-actions {
  opacity: 1;
}

/* 空状态和加载状态 */
.empty-state,
.loading-state,
.coming-soon {
  text-align: center;
  padding: 60px 20px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
  color: #666;
}

.loading-state .el-icon {
  font-size: 32px;
}

/* 即将上线 */
.coming-soon {
  max-width: 500px;
  margin: 0 auto;
}

.feature-desc {
  font-size: 16px;
  color: #666;
  margin: 20px 0 15px;
}

.feature-list {
  list-style: none;
  padding: 0;
  text-align: left;
  margin: 0 auto 30px;
  max-width: 300px;
}

.feature-list li {
  padding: 8px 0;
  color: #666;
  font-size: 14px;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

/* 播放历史列表 */
.history-list {
  margin-top: 20px;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  gap: 15px;
}

.history-item:hover {
  background: #f5f7fa;
}

.play-info {
  width: 150px;
  text-align: center;
  flex-shrink: 0;
}

.play-time {
  color: #999;
  font-size: 12px;
  margin-bottom: 2px;
}

.play-count {
  color: #409eff;
  font-size: 11px;
}

.clickable {
  cursor: pointer;
  transition: color 0.3s;
}

.clickable:hover {
  color: #409eff;
}

/* 响应式 */
/* 歌单相关样式 */
.playlist-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.header-info h3 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 5px;
}

.count-info {
  font-size: 14px;
  color: #999;
}

.playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.playlist-card {
  position: relative;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.playlist-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.12);
}

.playlist-cover-wrapper {
  position: relative;
  width: 100%;
  padding-top: 100%;
  overflow: hidden;
  background: #f5f5f5;
}

.playlist-cover {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.playlist-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.playlist-card:hover .playlist-overlay {
  opacity: 1;
}

.playlist-overlay .play-icon {
  font-size: 48px;
  color: white;
}

.playlist-info {
  padding: 15px;
}

.playlist-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playlist-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #999;
  margin-bottom: 5px;
}

.playlist-desc {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.playlist-actions {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 10;
  opacity: 0;
  transition: opacity 0.3s;
}

.playlist-card:hover .playlist-actions {
  opacity: 1;
}

/* 响应式 - 全局样式已覆盖 */
@media (max-width: 768px) {
  .my-music {
    padding: 15px 10px;
  }
  
  .song-item,
  .history-item {
    flex-wrap: wrap;
  }
  
  .favorite-time,
  .play-info {
    width: auto;
    font-size: 12px;
  }
  
  .play-count {
    display: none;
  }
  
  .playlist-actions {
    opacity: 1;
  }
  
  .playlist-header {
    flex-direction: column;
    align-items: flex-start !important;
    gap: 15px;
  }
  
  .playlist-header .el-button {
    width: 100%;
  }
}
</style>
