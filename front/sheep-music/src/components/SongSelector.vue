<template>
  <el-dialog
    v-model="visible"
    title="选择歌曲"
    width="600px"
    @close="handleClose"
  >
    <div class="song-selector">
      <!-- 搜索框 -->
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索歌曲名称或歌手"
          prefix-icon="Search"
          clearable
          @input="handleSearch"
        />
      </div>
      
      <!-- 标签页 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="当前播放" name="current">
          <div v-if="currentSong" class="current-song">
            <div class="song-item" @click="selectSong(currentSong)">
              <el-image 
                :src="currentSong.cover" 
                fit="cover"
                class="song-cover"
              >
                <template #error>
                  <div class="image-error">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="song-info">
                <div class="song-name">{{ currentSong.name }}</div>
                <div class="song-artist">{{ currentSong.artist }}</div>
              </div>
              <el-button type="primary" size="small">选择</el-button>
            </div>
          </div>
          <div v-else class="empty-state">
            <el-empty description="当前没有播放歌曲" />
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="播放列表" name="playlist">
          <div v-loading="loading" class="song-list">
            <div v-if="playlistSongs.length === 0 && !loading" class="empty-state">
              <el-empty description="播放列表为空" />
            </div>
            <div
              v-for="song in playlistSongs"
              :key="song.id"
              class="song-item"
              @click="selectSong(song)"
            >
              <el-image 
                :src="song.cover" 
                fit="cover"
                class="song-cover"
              >
                <template #error>
                  <div class="image-error">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="song-info">
                <div class="song-name">{{ song.name }}</div>
                <div class="song-artist">{{ song.artist }}</div>
              </div>
              <el-button type="primary" size="small">选择</el-button>
            </div>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="搜索" name="search">
          <div v-loading="loading" class="song-list">
            <div v-if="searchResults.length === 0 && !loading" class="empty-state">
              <el-empty :description="searchKeyword ? '没有找到相关歌曲' : '请输入关键词搜索'" />
            </div>
            <div
              v-for="song in searchResults"
              :key="song.id"
              class="song-item"
              @click="selectSong(song)"
            >
              <el-image 
                :src="song.cover" 
                fit="cover"
                class="song-cover"
              >
                <template #error>
                  <div class="image-error">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="song-info">
                <div class="song-name">{{ song.name }}</div>
                <div class="song-artist">{{ song.artist }}</div>
              </div>
              <el-button type="primary" size="small">选择</el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { usePlayerStore } from '@/store/player'
import { searchSongs } from '@/api/song'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'select'])

const playerStore = usePlayerStore()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const activeTab = ref('current')
const searchKeyword = ref('')
const loading = ref(false)
const searchResults = ref([])
let searchTimer = null

// 当前播放的歌曲
const currentSong = computed(() => {
  const song = playerStore.currentSong
  if (!song) return null
  
  let artistName = '未知歌手'
  if (song.artist) {
    artistName = Array.isArray(song.artist) ? song.artist.join(', ') : song.artist
  } else if (song.artists) {
    artistName = Array.isArray(song.artists) ? song.artists.join(', ') : song.artists
  } else if (song.artistName) {
    artistName = song.artistName
  }
  
  return {
    ...song,
    cover: song.cover || song.avatar || song.picUrl || '',
    artist: artistName
  }
})

// 播放列表
const playlistSongs = computed(() => {
  const playlist = playerStore.playlist || []
  return playlist.map(song => {
    let artistName = '未知歌手'
    if (song.artist) {
      artistName = Array.isArray(song.artist) ? song.artist.join(', ') : song.artist
    } else if (song.artists) {
      artistName = Array.isArray(song.artists) ? song.artists.join(', ') : song.artists
    } else if (song.artistName) {
      artistName = song.artistName
    }
    
    return {
      ...song,
      cover: song.cover || song.avatar || song.picUrl || '',
      artist: artistName
    }
  })
})

// 处理标签页切换
const handleTabChange = (tab) => {
  if (tab === 'search' && searchKeyword.value) {
    performSearch()
  }
}

// 处理搜索输入
const handleSearch = () => {
  if (activeTab.value !== 'search') {
    activeTab.value = 'search'
  }
  
  // 防抖搜索
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    return
  }
  
  searchTimer = setTimeout(() => {
    performSearch()
  }, 500)
}

// 执行搜索
const performSearch = async () => {
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    return
  }
  
  loading.value = true
  try {
    const res = await searchSongs({
      keyword: searchKeyword.value,
      page: 0,
      size: 20
    })
    
    if (res.code === 200) {
      const songs = res.data?.content || []
      
      // 处理数据，确保有 cover 和 artist 字段
      const processed = songs.map(song => {
        
        
        let artistName = '未知歌手'
        
        // 处理 artist 字段，可能是字符串或数组
        if (song.artist) {
          if (Array.isArray(song.artist)) {
          
            // 如果数组元素是对象，提取 name 字段
            artistName = song.artist.map(a => {
              if (typeof a === 'object' && a !== null) {
                return a.name || a.artistName || String(a)
              }
              return String(a)
            }).join(', ')
          } else {
            artistName = song.artist
          }
        } else if (song.artists) {
          if (Array.isArray(song.artists)) {
            artistName = song.artists.map(a => {
              if (typeof a === 'object' && a !== null) {
                return a.name || a.artistName || String(a)
              }
              return String(a)
            }).join(', ')
          } else {
            artistName = song.artists
          }
        } else if (song.artistName) {
          artistName = song.artistName
        }
        
        // 处理歌曲名称字段
        const songName = song.name || song.songName || song.title || '未知歌曲'
        
        return {
          id: song.id,
          name: songName,
          cover: song.cover || song.avatar || song.picUrl || '',
          artist: artistName
        }
      })   
      searchResults.value = processed
    }
  } catch (error) {
    console.error('搜索歌曲失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

// 选择歌曲
const selectSong = (song) => {
  if (!song) return
  emit('select', song)
  visible.value = false
}

// 关闭对话框
const handleClose = () => {
  searchKeyword.value = ''
  searchResults.value = []
  activeTab.value = 'current'
}

// 监听对话框打开
watch(visible, (val) => {
  if (!val) {
    handleClose()
  }
})
</script>

<style scoped>
.song-selector {
  min-height: 400px;
  max-height: 600px;
}

.search-box {
  margin-bottom: 16px;
}

.current-song {
  padding: 12px 0;
}

.song-list {
  max-height: 450px;
  overflow-y: auto;
}

.song-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  gap: 12px;
  margin-bottom: 8px;
}

.song-item:hover {
  background: #f5f7fa;
}

.song-cover {
  width: 50px;
  height: 50px;
  border-radius: 6px;
  flex-shrink: 0;
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
}

.song-info {
  flex: 1;
  min-width: 0;
}

.song-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
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

.empty-state {
  padding: 60px 0;
  text-align: center;
}

:deep(.el-tabs__content) {
  padding: 0;
}
</style>
