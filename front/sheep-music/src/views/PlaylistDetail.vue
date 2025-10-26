<template>
  <div class="playlist-detail">
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <p>加载中...</p>
    </div>
    
    <div v-else-if="playlist" class="detail-container">
      <!-- 歌单头部信息 -->
      <div class="playlist-header">
        <div class="cover-section">
          <PlaylistCover 
            :cover="playlist.cover" 
            :size="250" 
            :default-cover="defaultCover"
            class="playlist-cover"
          />
        </div>
        
        <div class="info-section">
          <div class="playlist-tag">
            <el-tag v-if="playlist.category" type="primary" size="small">{{ playlist.category }}</el-tag>
            <el-tag v-if="playlist.isPublic" type="success" size="small">公开</el-tag>
            <el-tag v-else type="info" size="small">私有</el-tag>
          </div>
          
          <h1 class="playlist-name">{{ playlist.name }}</h1>
          
          <div class="creator-info">
            <el-icon><User /></el-icon>
            <span>创建者：{{ playlist.creator?.nickname || '未知' }}</span>
            <span class="separator">|</span>
            <span>{{ formatDate(playlist.createTime) }}</span>
          </div>
          
          <p class="playlist-description" v-if="playlist.description">
            {{ playlist.description }}
          </p>
          
          <div class="playlist-stats">
            <span class="stat-item">
              <el-icon><List /></el-icon>
              {{ playlist.songCount || 0 }} 首歌曲
            </span>
            <span class="stat-item">
              <el-icon><Headset /></el-icon>
              播放 {{ formatCount(playlist.playCount) }} 次
            </span>
          </div>
          
          <div class="action-buttons">
            <el-button type="primary" size="large" @click="playAll">
              <el-icon><CaretRight /></el-icon> 播放全部
            </el-button>
            <el-button size="large" @click="playAll(true)">
              <el-icon><Refresh /></el-icon> 随机播放
            </el-button>
            <el-button v-if="isOwner" size="large" @click="goToEdit">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
          </div>
        </div>
      </div>
      
      <!-- 歌曲列表 -->
      <div class="songs-section">
        <div class="section-header">
          <h3>歌曲列表</h3>
          <span class="song-count">共 {{ songs.length }} 首</span>
        </div>
        
        <div v-if="songs.length > 0" class="song-list">
          <div 
            v-for="(item, index) in songs" 
            :key="item.id"
            class="song-item"
            @click="playSong(item.song, index)"
          >
            <div class="song-index">{{ index + 1 }}</div>
            <img :src="item.song?.cover || defaultCover" class="song-cover" />
            <div class="song-info">
              <div class="song-name">{{ item.song?.title || '未知歌曲' }}</div>
              <div class="song-artist">
                <template v-if="item.song?.artists && item.song.artists.length > 0">
                  <span v-for="(artist, idx) in item.song.artists" :key="artist.id">
                    <span class="clickable" @click.stop="goToArtist(artist.id)">{{ artist.name }}</span>
                    <span v-if="idx < item.song.artists.length - 1"> / </span>
                  </span>
                </template>
                <span v-else>未知歌手</span>
              </div>
            </div>
            <div class="song-duration">{{ formatDuration(item.song?.duration) }}</div>
            <div class="song-actions" @click.stop>
              <el-button 
                icon="CaretRight" 
                circle 
                size="small" 
                @click="playSong(item.song, index)"
                title="播放"
              />
              <el-button 
                icon="Plus" 
                circle 
                size="small" 
                @click="addToPlaylist(item.song)"
                title="添加到播放列表"
              />
              <el-button 
                icon="FolderAdd" 
                circle 
                size="small" 
                @click="showAddToPlaylistDialog(item.song?.id)"
                title="添加到其他歌单"
              />
              <el-button 
                v-if="isOwner"
                icon="Delete" 
                circle 
                size="small" 
                @click="removeSong(item.songId)"
                title="从歌单移除"
              />
            </div>
          </div>
        </div>
        
        <div v-else class="empty-state">
          <el-empty description="歌单还没有歌曲">
            <el-button v-if="isOwner" type="primary" @click="router.push('/')">去首页添加歌曲</el-button>
          </el-empty>
        </div>
      </div>
    </div>
    
    <div v-else class="error-state">
      <el-empty description="歌单不存在或已被删除">
        <el-button type="primary" @click="$router.back()">返回</el-button>
      </el-empty>
    </div>
    
    <!-- 添加到歌单对话框 -->
    <PlaylistSelector 
      v-model="playlistSelectorVisible" 
      :song-id="selectedSongId"
      @success="handleAddSuccess"
    />
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { usePlayerStore } from '@/store/player'
import { useUserStore } from '@/store/user'
import { getPlaylistDetail, getPlaylistSongs, removeSongFromPlaylist, incrementPlayCount } from '@/api/playlist'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, User, List, Headset, CaretRight, Refresh, Edit, Plus, Delete } from '@element-plus/icons-vue'
import PlaylistCover from '@/components/PlaylistCover.vue'
import PlaylistSelector from '@/components/PlaylistSelector.vue'

export default {
  name: 'PlaylistDetail',
  components: {
    PlaylistCover,
    PlaylistSelector,
    Loading,
    User,
    List,
    Headset,
    CaretRight,
    Refresh,
    Edit,
    Plus,
    Delete
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const playerStore = usePlayerStore()
    const userStore = useUserStore()
    
    const loading = ref(false)
    const playlist = ref(null)
    const songs = ref([])
    const defaultCover = 'https://via.placeholder.com/200?text=Music'
    
    // 歌单选择器
    const playlistSelectorVisible = ref(false)
    const selectedSongId = ref(null)
    
    // 是否是歌单创建者
    const isOwner = computed(() => {
      return playlist.value && userStore.userInfo && playlist.value.userId === userStore.userInfo.id
    })
    
    // 加载歌单详情
    const loadPlaylistDetail = async () => {
      loading.value = true
      try {
        const playlistId = route.params.id
        const res = await getPlaylistDetail(playlistId)
        
        if (res.code === 200) {
          playlist.value = res.data
          await loadSongs(playlistId)
          
          // 增加播放次数
          incrementPlayCount(playlistId).catch(() => {})
        }
      } catch (error) {
        console.error('加载歌单失败:', error)
        ElMessage.error('加载歌单失败')
      } finally {
        loading.value = false
      }
    }
    
    // 加载歌单歌曲
    const loadSongs = async (playlistId) => {
      try {
        const res = await getPlaylistSongs(playlistId, { page: 0, size: 500 })
        if (res.code === 200) {
          songs.value = res.data.content || []
        }
      } catch (error) {
        console.error('加载歌曲列表失败:', error)
      }
    }
    
    // 播放全部
    const playAll = (random = false) => {
      if (songs.value.length === 0) {
        ElMessage.warning('歌单没有歌曲')
        return
      }
      
      const songList = songs.value.map(item => item.song).filter(s => s)
      
      if (random) {
        // 随机播放
        const shuffled = [...songList].sort(() => Math.random() - 0.5)
        playerStore.play(shuffled[0], shuffled)
      } else {
        // 顺序播放
        playerStore.play(songList[0], songList)
      }
      
      ElMessage.success(random ? '开始随机播放' : '开始播放全部')
    }
    
    // 播放单曲
    const playSong = (song, index) => {
      if (!song) return
      const songList = songs.value.map(item => item.song).filter(s => s)
      playerStore.play(song, songList)
    }
    
    // 添加到播放列表
    const addToPlaylist = (song) => {
      if (!song) return
      playerStore.addToPlaylist(song)
      ElMessage.success(`已添加到播放列表: ${song.title}`)
    }
    
    // 从歌单移除歌曲
    const removeSong = async (songId) => {
      try {
        await ElMessageBox.confirm(
          '确定要从歌单中移除这首歌吗？',
          '确认移除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        const res = await removeSongFromPlaylist(playlist.value.id, songId)
        if (res.code === 200) {
          ElMessage.success('移除成功')
          // 重新加载歌单详情（包含最新的封面和歌曲数量）
          await loadPlaylistDetail()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('移除失败')
        }
      }
    }
    
    // 跳转到编辑
    const goToEdit = () => {
      router.push(`/my-music?tab=playlists`)
    }
    
    // 跳转到歌手详情
    const goToArtist = (artistId) => {
      if (artistId) {
        router.push(`/artist/${artistId}`)
      }
    }
    
    // 格式化时长
    const formatDuration = (seconds) => {
      if (!seconds) return '--:--'
      const mins = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${mins}:${secs.toString().padStart(2, '0')}`
    }
    
    // 格式化日期
    const formatDate = (dateStr) => {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return date.toLocaleDateString('zh-CN')
    }
    
    // 格式化数量
    const formatCount = (count) => {
      if (!count) return 0
      if (count >= 10000) {
        return (count / 10000).toFixed(1) + '万'
      }
      return count
    }
    
    // 显示添加到歌单对话框
    const showAddToPlaylistDialog = (songId) => {
      if (!songId) return
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
    
    onMounted(() => {
      loadPlaylistDetail()
    })
    
    return {
      loading,
      playlist,
      songs,
      defaultCover,
      isOwner,
      playAll,
      playSong,
      addToPlaylist,
      removeSong,
      goToEdit,
      goToArtist,
      formatDuration,
      formatDate,
      formatCount,
      router,
      playlistSelectorVisible,
      selectedSongId,
      showAddToPlaylistDialog,
      handleAddSuccess
    }
  }
}
</script>

<style scoped>
.playlist-detail {
  min-height: 100vh;
  background: linear-gradient(to bottom, #f5f7fa 0%, #ffffff 300px);
  padding: 30px;
}

.loading-container,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  font-size: 32px;
  color: #409eff;
}

.loading-container p {
  margin-top: 20px;
  font-size: 14px;
  color: #999;
}

/* 歌单头部 */
.playlist-header {
  display: flex;
  gap: 40px;
  margin-bottom: 40px;
  background: white;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.cover-section {
  flex-shrink: 0;
}

.playlist-cover {
  width: 250px;
  height: 250px;
  border-radius: 12px;
  object-fit: cover;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.info-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.playlist-tag {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.playlist-name {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin-bottom: 15px;
  line-height: 1.3;
}

.creator-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
  margin-bottom: 15px;
}

.separator {
  color: #ddd;
}

.playlist-description {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 20px;
  max-width: 600px;
}

.playlist-stats {
  display: flex;
  gap: 30px;
  margin-bottom: 30px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #666;
}

.action-buttons {
  display: flex;
  gap: 15px;
}

/* 歌曲列表 */
.songs-section {
  background: white;
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.section-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.song-count {
  font-size: 14px;
  color: #999;
}

.song-list {
  display: flex;
  flex-direction: column;
}

.song-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.song-item:hover {
  background: #f5f7fa;
}

.song-index {
  width: 40px;
  text-align: center;
  font-size: 14px;
  color: #999;
  font-weight: 500;
}

.song-cover {
  width: 50px;
  height: 50px;
  border-radius: 6px;
  object-fit: cover;
  margin-right: 15px;
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

.song-artist {
  font-size: 13px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.clickable {
  cursor: pointer;
  transition: color 0.2s;
}

.clickable:hover {
  color: #409eff;
}

.song-duration {
  width: 80px;
  text-align: center;
  font-size: 14px;
  color: #999;
}

.song-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.song-item:hover .song-actions {
  opacity: 1;
}

.empty-state {
  padding: 60px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .playlist-detail {
    padding: 15px;
  }
  
  .playlist-header {
    flex-direction: column;
    padding: 20px;
    gap: 20px;
  }
  
  .playlist-cover {
    width: 100%;
    height: auto;
    max-width: 250px;
    margin: 0 auto;
  }
  
  .playlist-name {
    font-size: 24px;
  }
  
  .action-buttons {
    flex-wrap: wrap;
  }
  
  .songs-section {
    padding: 15px;
  }
  
  .song-item {
    padding: 8px;
  }
  
  .song-index {
    width: 30px;
  }
  
  .song-cover {
    width: 40px;
    height: 40px;
  }
  
  .song-duration {
    display: none;
  }
  
  .song-actions {
    opacity: 1;
  }
}
</style>
