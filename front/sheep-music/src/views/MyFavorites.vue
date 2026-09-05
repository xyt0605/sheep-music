<template>
  <div class="my-favorites">
    <div class="page-header">
      <h2>我的收藏</h2>
      <p class="count-text">
        共 {{ pagination.total }} 首歌曲
      </p>
    </div>
    
    <!-- 收藏列表 -->
    <div class="favorites-list">
      <div 
        v-for="favorite in favorites" 
        :key="favorite.id"
        class="favorite-item"
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
      <el-empty description="暂无收藏">
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
      v-if="pagination.total > pagination.size"
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.size"
      :total="pagination.total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pagination"
      @size-change="loadFavorites"
      @current-change="loadFavorites"
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePlayerStore } from '@/store/player'
import { getMyFavorites, removeFavorite } from '@/api/favorite'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import PlaylistSelector from '@/components/PlaylistSelector.vue'

import { ossThumb } from '@/utils/image'

export default {
  name: 'MyFavorites',
  components: {
    Loading,
    PlaylistSelector
  },
  setup() {
    const router = useRouter()
    const playerStore = usePlayerStore()
    const favorites = ref([])
    const loading = ref(false)
  const defaultCover = '/default-cover.svg'
    
    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })
    
    // 歌单选择器
    const playlistSelectorVisible = ref(false)
    const selectedSongId = ref(null)
    
    // 加载收藏列表
    const loadFavorites = async () => {
      loading.value = true
      try {
        const res = await getMyFavorites({
          page: pagination.page - 1,
          size: pagination.size
        })
        
        if (res.code === 200) {
          favorites.value = res.data.content || []
          pagination.total = res.data.totalElements || 0
        }
      } catch (error) {
        console.error('加载收藏失败:', error)
        ElMessage.error('加载收藏失败')
      } finally {
        loading.value = false
      }
    }
    
    // 播放歌曲
    const handlePlaySong = (song) => {
      if (!song) return
      const songList = favorites.value.map(f => f.song)
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
    
    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      const now = new Date()
      const diff = now - date
      
      // 1分钟内
      if (diff < 60 * 1000) {
        return '刚刚'
      }
      // 1小时内
      if (diff < 60 * 60 * 1000) {
        return `${Math.floor(diff / (60 * 1000))}分钟前`
      }
      // 1天内
      if (diff < 24 * 60 * 60 * 1000) {
        return `${Math.floor(diff / (60 * 60 * 1000))}小时前`
      }
      // 7天内
      if (diff < 7 * 24 * 60 * 60 * 1000) {
        return `${Math.floor(diff / (24 * 60 * 60 * 1000))}天前`
      }
      // 超过7天显示具体日期
      return date.toLocaleDateString('zh-CN')
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
    
    onMounted(() => {
      loadFavorites()
    })
    
    return {
      favorites,
      loading,
      pagination,
      defaultCover,
      handlePlaySong,
      handleAddToPlaylist,
      handleRemoveFavorite,
      goToArtist,
      formatDuration,
      formatDate,
      loadFavorites,
      playlistSelectorVisible,
      selectedSongId,
      showAddToPlaylistDialog,
      handleAddSuccess
    }
  }
}
</script>

<style scoped>
.my-favorites {
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

.count-text {
  color: #666;
  font-size: 14px;
}

.favorites-list {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.favorite-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  gap: 15px;
}

.favorite-item:hover {
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

.favorite-item:hover .song-actions {
  opacity: 1;
}

.empty-state,
.loading-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
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

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

@media (max-width: 768px) {
  .favorite-item {
    flex-wrap: wrap;
  }
  
  .song-duration,
  .favorite-time {
    width: auto;
  }
  
  .song-actions {
    opacity: 1;
  }
}
</style>

