<template>
  <div class="music-player" v-show="playerStore.showPlayer">
    <!-- 隐藏的 audio 元素 -->
    <audio ref="audioRef"></audio>
    
    <!-- 播放器主体 -->
    <div class="player-main">
      <!-- 左侧：歌曲信息 -->
      <div class="player-left">
        <img 
          :src="playerStore.currentSong?.cover || defaultCover" 
          alt="封面" 
          class="song-cover"
          @click="toggleLyric"
        >
        <div class="song-info">
          <div class="song-name">{{ playerStore.currentSong?.title || '暂无播放' }}</div>
          <div class="song-artist">{{ getArtistsName(playerStore.currentSong) }}</div>
        </div>
      </div>
      
      <!-- 中间：播放控制 -->
      <div class="player-center">
        <!-- 控制按钮 -->
        <div class="player-controls">
          <el-button 
            circle 
            size="small" 
            :disabled="!playerStore.hasPrevSong"
            @click="playerStore.prev"
          >
            <el-icon><DArrowLeft /></el-icon>
          </el-button>
          
          <el-button 
            circle 
            type="primary" 
            size="large"
            @click="playerStore.togglePlay"
            :disabled="!playerStore.currentSong"
          >
            <el-icon v-if="playerStore.isPlaying"><VideoPause /></el-icon>
            <el-icon v-else><VideoPlay /></el-icon>
          </el-button>
          
          <el-button 
            circle 
            size="small"
            :disabled="!playerStore.hasNextSong"
            @click="playerStore.next"
          >
            <el-icon><DArrowRight /></el-icon>
          </el-button>
        </div>
        
        <!-- 进度条 -->
        <div class="player-progress">
          <span class="time-text">{{ formatTime(playerStore.currentTime) }}</span>
          <el-slider 
            v-model="sliderValue" 
            :show-tooltip="false"
            @change="handleSeek"
            class="progress-slider"
          />
          <span class="time-text">{{ formatTime(playerStore.duration) }}</span>
        </div>
      </div>
      
      <!-- 右侧：其他控制 -->
      <div class="player-right">
        <!-- 播放模式切换 -->
        <el-tooltip :content="playModeText" placement="top">
          <el-button 
            circle 
            size="small"
            @click="playerStore.togglePlayMode"
          >
            <el-icon>
              <Refresh v-if="playerStore.playMode === 'list'" />
              <Promotion v-if="playerStore.playMode === 'random'" />
              <RefreshLeft v-if="playerStore.playMode === 'single'" />
            </el-icon>
          </el-button>
        </el-tooltip>
        
        <!-- 歌词显示 -->
        <el-button 
          circle 
          size="small"
          :type="playerStore.showLyric ? 'primary' : ''"
          @click="toggleLyric"
        >
          <el-icon><Reading /></el-icon>
        </el-button>
        
        <!-- 音量控制 -->
        <div class="volume-control">
          <el-icon><Headset /></el-icon>
          <el-slider 
            v-model="volumeValue" 
            :show-tooltip="false"
            @input="handleVolumeChange"
            style="width: 100px; margin-left: 10px;"
          />
        </div>
        
        <!-- 播放列表 -->
        <el-button 
          circle 
          size="small"
          @click="showPlaylistDialog = true"
        >
          <el-icon><List /></el-icon>
        </el-button>
      </div>
    </div>
    
    <!-- 歌词面板 -->
    <transition name="lyric-slide">
      <div class="lyric-panel" v-if="playerStore.showLyric">
        <div class="lyric-header">
          <h3>{{ playerStore.currentSong?.title }}</h3>
          <el-button circle size="small" @click="playerStore.toggleLyric">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="lyric-content" ref="lyricContentRef">
          <div 
            v-for="(line, index) in parsedLyrics" 
            :key="index"
            class="lyric-line"
            :class="{ active: index === currentLyricIndex }"
          >
            {{ line.text }}
          </div>
          <div v-if="!playerStore.currentSong?.lyric" class="no-lyric">
            暂无歌词
          </div>
        </div>
      </div>
    </transition>
    
    <!-- 播放列表对话框 -->
    <el-dialog 
      v-model="showPlaylistDialog" 
      title="播放列表" 
      width="500px"
    >
      <div class="playlist-content">
        <div 
          v-for="(song, index) in playerStore.playlist" 
          :key="song.id"
          class="playlist-item"
          :class="{ active: index === playerStore.currentIndex }"
          @click="playerStore.play(song)"
        >
          <div class="playlist-item-info">
            <span class="playlist-item-name">{{ song.title }}</span>
            <span class="playlist-item-artist">{{ getArtistsName(song) }}</span>
          </div>
          <el-button 
            link 
            type="danger" 
            size="small"
            @click.stop="playerStore.removeFromPlaylist(song.id)"
          >
            删除
          </el-button>
        </div>
        <div v-if="playerStore.playlist.length === 0" class="empty-playlist">
          播放列表为空
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { usePlayerStore } from '@/store/player'
import { 
  VideoPlay, 
  VideoPause, 
  DArrowLeft, 
  DArrowRight, 
  Headset,
  Reading,
  List,
  Close,
  Refresh,
  Promotion,
  RefreshLeft
} from '@element-plus/icons-vue'

const playerStore = usePlayerStore()
const audioRef = ref(null)
const lyricContentRef = ref(null)
const showPlaylistDialog = ref(false)
const defaultCover = 'https://via.placeholder.com/60?text=Music'

// 进度条值（0-100）
const sliderValue = computed({
  get: () => {
    if (playerStore.duration > 0) {
      return (playerStore.currentTime / playerStore.duration) * 100
    }
    return 0
  },
  set: (val) => {
    const time = (val / 100) * playerStore.duration
    playerStore.seek(time)
  }
})

// 音量值（0-100）
const volumeValue = ref(playerStore.volume * 100)

// 处理进度条拖动
const handleSeek = (value) => {
  const time = (value / 100) * playerStore.duration
  playerStore.seek(time)
}

// 处理音量变化
const handleVolumeChange = (value) => {
  playerStore.setVolume(value / 100)
}

// 格式化时间
const formatTime = (seconds) => {
  if (!seconds || isNaN(seconds)) return '00:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 获取歌手名称（支持多歌手）
const getArtistsName = (song) => {
  if (!song) return '未知歌手'
  if (song.artists && song.artists.length > 0) {
    return song.artists.map(a => a.name).join(' / ')
  }
  // 兼容旧数据
  if (song.artistName) {
    return song.artistName
  }
  return '未知歌手'
}

// 播放模式文本
const playModeText = computed(() => {
  const modeMap = {
    'list': '列表循环',
    'random': '随机播放',
    'single': '单曲循环'
  }
  return modeMap[playerStore.playMode] || '列表循环'
})

// 解析歌词
const parsedLyrics = computed(() => {
  const lyric = playerStore.currentSong?.lyric
  if (!lyric) return []
  
  const lines = lyric.split('\n')
  const parsed = []
  
  lines.forEach(line => {
    // 匹配 LRC 格式：[00:00.00]歌词
    const match = line.match(/\[(\d{2}):(\d{2})\.?(\d{2,3})?\](.*)/);
    if (match) {
      const minutes = parseInt(match[1])
      const seconds = parseInt(match[2])
      const milliseconds = match[3] ? parseInt(match[3].padEnd(3, '0')) : 0
      const time = minutes * 60 + seconds + milliseconds / 1000
      const text = match[4].trim()
      
      if (text) {
        parsed.push({ time, text })
      }
    } else if (line.trim() && !line.startsWith('[')) {
      // 纯文本歌词
      parsed.push({ time: 0, text: line.trim() })
    }
  })
  
  return parsed.sort((a, b) => a.time - b.time)
})

// 当前歌词索引
const currentLyricIndex = computed(() => {
  const currentTime = playerStore.currentTime
  for (let i = parsedLyrics.value.length - 1; i >= 0; i--) {
    if (currentTime >= parsedLyrics.value[i].time) {
      return i
    }
  }
  return -1
})

// 自动滚动歌词
watch(currentLyricIndex, async (newIndex) => {
  if (newIndex >= 0 && lyricContentRef.value) {
    await nextTick()
    const activeLine = lyricContentRef.value.querySelector('.lyric-line.active')
    if (activeLine) {
      activeLine.scrollIntoView({
        behavior: 'smooth',
        block: 'center'
      })
    }
  }
})

// 切换歌词显示
const toggleLyric = () => {
  playerStore.toggleLyric()
}

// 初始化
onMounted(() => {
  if (audioRef.value) {
    playerStore.initAudio(audioRef.value)
  }
})
</script>

<style scoped>
.music-player {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #e0e0e0;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

.player-main {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  gap: 20px;
}

/* 左侧 */
.player-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 250px;
}

.song-cover {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.3s;
}

.song-cover:hover {
  transform: scale(1.05);
}

.song-info {
  flex: 1;
  min-width: 0;
}

.song-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-artist {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

/* 中间 */
.player-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.player-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
}

.player-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}

.time-text {
  font-size: 12px;
  color: #999;
  min-width: 45px;
  text-align: center;
}

.progress-slider {
  flex: 1;
}

/* 右侧 */
.player-right {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 250px;
  justify-content: flex-end;
}

.volume-control {
  display: flex;
  align-items: center;
}

/* 歌词面板 */
.lyric-panel {
  border-top: 1px solid #e0e0e0;
  background: #fafafa;
  max-height: 300px;
  overflow: hidden;
}

.lyric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #e0e0e0;
}

.lyric-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.lyric-content {
  height: 250px;
  overflow-y: auto;
  padding: 20px;
  scroll-behavior: smooth;
}

.lyric-line {
  text-align: center;
  padding: 8px 0;
  color: #999;
  font-size: 14px;
  line-height: 1.8;
  transition: all 0.3s;
}

.lyric-line.active {
  color: #409eff;
  font-size: 16px;
  font-weight: 500;
  transform: scale(1.1);
}

.no-lyric {
  text-align: center;
  color: #999;
  padding: 60px 0;
  font-size: 14px;
}

/* 歌词面板动画 */
.lyric-slide-enter-active,
.lyric-slide-leave-active {
  transition: all 0.3s ease;
}

.lyric-slide-enter-from,
.lyric-slide-leave-to {
  max-height: 0;
  opacity: 0;
}

/* 播放列表 */
.playlist-content {
  max-height: 400px;
  overflow-y: auto;
}

.playlist-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
}

.playlist-item:hover {
  background: #f5f5f5;
}

.playlist-item.active {
  background: #e3f2fd;
}

.playlist-item.active .playlist-item-name {
  color: #409eff;
  font-weight: 500;
}

.playlist-item-info {
  flex: 1;
  min-width: 0;
}

.playlist-item-name {
  display: block;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playlist-item-artist {
  display: block;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.empty-playlist {
  text-align: center;
  padding: 60px 0;
  color: #999;
}

/* 响应式 */
@media (max-width: 768px) {
  .player-main {
    flex-wrap: wrap;
  }
  
  .player-left {
    min-width: auto;
  }
  
  .player-right {
    min-width: auto;
    width: 100%;
    justify-content: center;
  }
  
  .volume-control {
    display: none;
  }
}
</style>

