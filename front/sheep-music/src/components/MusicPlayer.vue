<template>
  <div
    v-show="playerStore.showPlayer"
    class="music-player"
  >
    <!-- 隐藏的 audio 元素 -->
    <audio
      ref="audioRef"
      preload="metadata"
      crossorigin="anonymous"
    />
    
    <!-- 播放器主体 -->
    <div class="player-main">
      <!-- 左侧：歌曲信息 -->
      <div class="player-left">
        <div
          class="album-cover-wrapper"
          @click="showFullscreenLyric"
        >
          <img 
            :src="ossThumb(processImageUrl(playerStore.currentSong?.cover), 200) || defaultCover" 
            alt="封面" 
            :class="['song-cover', { 'playing': playerStore.isPlaying }]"
            @error="handleCoverError"
          >
          <div class="cover-overlay">
            <el-icon class="expand-icon">
              <FullScreen />
            </el-icon>
          </div>
        </div>
        <div class="song-info">
          <div class="song-name">
            {{ playerStore.currentSong?.title || '暂无播放' }}
          </div>
          <div class="song-artist">
            {{ getArtistsName(playerStore.currentSong) }}
          </div>
        </div>
      </div>
      
      <!-- 中间：播放控制 -->
      <div class="player-center">
        <!-- 控制按钮 -->
        <div class="player-controls">
          <el-button 
            circle 
            class="control-btn"
            :disabled="!playerStore.hasPrevSong"
            @click="playerStore.prev"
          >
            <el-icon><CaretLeft /></el-icon>
          </el-button>
          
          <el-button 
            circle 
            type="primary" 
            class="play-btn"
            :disabled="!playerStore.currentSong"
            @click="playerStore.togglePlay"
          >
            <el-icon v-if="playerStore.isPlaying">
              <VideoPause />
            </el-icon>
            <el-icon v-else>
              <VideoPlay />
            </el-icon>
          </el-button>
          
          <el-button 
            circle 
            class="control-btn"
            :disabled="!playerStore.hasNextSong"
            @click="playerStore.next"
          >
            <el-icon><CaretRight /></el-icon>
          </el-button>
        </div>
        
        <!-- 进度条 -->
        <div class="player-progress">
          <span class="time-text">{{ formatTime(playerStore.currentTime) }}</span>
          <el-slider 
            v-model="sliderValue" 
            :show-tooltip="false"
            class="progress-slider"
            @change="handleSeek"
          />
          <span class="time-text">{{ formatTime(playerStore.duration) }}</span>
        </div>
      </div>
      
      <!-- 右侧：其他控制 -->
      <div class="player-right">
        <!-- 音量控制（图标随音量分级，点击静音/恢复，借鉴 Spotify） -->
        <div class="volume-control">
          <el-icon
            class="volume-icon"
            :title="isMuted ? '取消静音' : '静音'"
            @click="toggleMute"
          >
            <Mute v-if="isMuted" />
            <Headset v-else />
          </el-icon>
          <el-slider
            v-model="volumeValue"
            :show-tooltip="false"
            class="volume-slider"
            @input="handleVolumeChange"
          />
        </div>

        <div class="divider" />

        <!-- 播放模式切换 -->
        <el-tooltip
          :content="playModeText"
          placement="top"
        >
          <el-button 
            class="action-btn" 
            circle
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
        <el-tooltip
          content="歌词"
          placement="top"
        >
          <el-button 
            class="action-btn" 
            :class="{ 'is-active': playerStore.showLyric }"
            circle
            @click="toggleLyric"
          >
            <el-icon><Reading /></el-icon>
          </el-button>
        </el-tooltip>
        
        <!-- 播放列表 -->
        <el-tooltip
          content="播放列表"
          placement="top"
        >
          <el-button 
            class="action-btn" 
            circle
            @click="showPlaylistDialog = true"
          >
            <el-icon><List /></el-icon>
          </el-button>
        </el-tooltip>

        <!-- 歌曲评论入口 -->
        <el-tooltip
          content="评论"
          placement="top"
        >
          <el-button
            class="action-btn"
            circle
            :disabled="!playerStore.currentSong?.id"
            @click="showCommentsDialog = true"
          >
            <el-icon><ChatLineRound /></el-icon>
          </el-button>
        </el-tooltip>
        
        <!-- 分享按钮 -->
        <el-tooltip
          content="分享"
          placement="top"
        >
          <el-button
            class="action-btn"
            circle
            :disabled="!playerStore.currentSong?.id"
            @click="handleShareSong"
          >
            <el-icon><Share /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>
    
    <!-- 歌词面板 -->
    <transition name="lyric-slide">
      <div
        v-if="playerStore.showLyric"
        class="lyric-panel"
      >
        <div class="lyric-header">
          <h3>{{ playerStore.currentSong?.title }}</h3>
          <el-button
            circle
            size="small"
            @click="playerStore.toggleLyric"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div
          ref="lyricContentRef"
          class="lyric-content"
        >
          <div 
            v-for="(line, index) in parsedLyrics" 
            :key="index"
            class="lyric-line"
            :class="{ active: index === currentLyricIndex }"
          >
            {{ line.text }}
          </div>
          <div
            v-if="!playerStore.currentSong?.lyric"
            class="no-lyric"
          >
            暂无歌词
          </div>
        </div>
      </div>
    </transition>
    
    <!-- 播放队列抽屉（借鉴 Spotify / 网易云：当前高亮、点行切歌、行内移除、一键清空） -->
    <el-drawer
      v-model="showPlaylistDialog"
      direction="rtl"
      size="380px"
      append-to-body
      class="queue-drawer"
      :with-header="false"
    >
      <div class="queue-header">
        <div class="playlist-title">
          <span>播放队列</span>
          <span class="playlist-count">({{ playerStore.playlist.length }})</span>
        </div>
        <el-button
          link
          type="info"
          :disabled="!playerStore.playlist.length"
          @click="clearPlaylist"
        >
          <el-icon><Delete /></el-icon> 清空
        </el-button>
      </div>

      <div class="playlist-content">
        <div
          v-for="(song, index) in playerStore.playlist"
          :key="song.id"
          class="playlist-item"
          :class="{ active: index === playerStore.currentIndex }"
          @click="playerStore.play(song)"
        >
          <!-- 序号/状态 -->
          <div class="song-index-col">
            <div
              v-if="index === playerStore.currentIndex"
              class="playing-indicator"
            >
              <span /><span /><span />
            </div>
            <span
              v-else
              class="index-num"
            >{{ index + 1 }}</span>
          </div>

          <!-- 信息 -->
          <div class="song-info-col">
            <div
              class="song-title"
              :title="song.title"
            >
              {{ song.title }}
            </div>
            <div
              class="song-artist"
              :title="getArtistsName(song)"
            >
              {{ getArtistsName(song) }}
            </div>
          </div>

          <!-- 操作 -->
          <div class="song-actions-col">
            <el-button
              link
              type="danger"
              class="delete-btn"
              @click.stop="playerStore.removeFromPlaylist(song.id)"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <div
          v-if="playerStore.playlist.length === 0"
          class="empty-playlist"
        >
          <el-empty
            description="播放队列为空"
            :image-size="100"
          />
        </div>
      </div>
    </el-drawer>

    <!-- 歌曲评论对话框 -->
    <el-dialog
      v-model="showCommentsDialog"
      title="歌曲评论"
      width="600px"
      append-to-body
    >
      <CommentList
        v-if="playerStore.currentSong?.id"
        :key="playerStore.currentSong.id"
        :song-id="playerStore.currentSong.id"
        :show-rating="false"
      />
      <template #footer>
        <el-button @click="showCommentsDialog = false">
          关闭
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 分享弹窗：首次打开才挂载，届时才拉取异步代码块 -->
    <ShareDialog
      v-if="shareDialogRendered"
      v-model="showShareDialog"
      type="song"
      :share-data="shareData"
      @success="handleShareSuccess"
    />
  </div>
</template>

<script setup>
import { ossThumb } from '@/utils/image'
import { ref, computed, watch, onMounted, nextTick, inject, defineAsyncComponent } from 'vue'
import { usePlayerStore } from '@/store/player'
import { 
  VideoPlay, 
  VideoPause, 
  CaretLeft, 
  CaretRight, 
  Headset,
  Reading,
  List,
  Close,
  Refresh,
  Promotion,
  RefreshLeft,
  ChatLineRound,
  Share,
  FullScreen,
  Delete,
  Mute
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
// 评论与分享弹窗首次打开才加载（内部含 emoji 选择器及其数据包，避免挤占首屏）
const CommentList = defineAsyncComponent(() => import('@/components/Social/CommentList.vue'))
const ShareDialog = defineAsyncComponent(() => import('@/components/ShareDialog.vue'))

const playerStore = usePlayerStore()
const userStore = useUserStore()
const router = useRouter()
const audioRef = ref(null)
const lyricContentRef = ref(null)
const showPlaylistDialog = ref(false)
const showCommentsDialog = ref(false)
const showShareDialog = ref(false)
const shareDialogRendered = ref(false) // 首次点分享时才真正加载 ShareDialog
const shareData = ref({})
const defaultCover = '/default-cover.svg'

const handleCoverError = (event) => {
  if (event.target?.src && !event.target.src.endsWith(defaultCover)) {
    event.target.src = defaultCover
  }
}

// 注入全屏歌词显示方法
const showFullscreenLyric = inject('showFullscreenLyric', () => {
  console.log('全屏歌词功能未初始化')
})

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

// 静音切换（借鉴 Spotify：点击音量图标静音/恢复）
const prevVolume = ref(volumeValue.value > 0 ? volumeValue.value : 70)
const isMuted = computed(() => volumeValue.value === 0)
const toggleMute = () => {
  if (isMuted.value) {
    volumeValue.value = prevVolume.value > 0 ? prevVolume.value : 70
  } else {
    prevVolume.value = volumeValue.value
    volumeValue.value = 0
  }
  handleVolumeChange(volumeValue.value)
}

// 处理 OSS URL，使用服务器代理避免跨域
const processImageUrl = (url) => {
  if (!url) return ''
  
  const ossHost = 'https://sheepmusic.oss-cn-hangzhou.aliyuncs.com'
  if (url.startsWith(ossHost)) {
    const ossPath = url.substring(ossHost.length)
    return '/api/oss' + (ossPath.startsWith('/') ? ossPath : '/' + ossPath)
  }
  
  return url
}

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

// 分享歌曲
const handleShareSong = () => {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  const song = playerStore.currentSong
  if (!song || !song.id) {
    ElMessage.error('歌曲信息无效')
    return
  }
  
  shareData.value = {
    id: song.id,
    name: song.title || song.name,
    cover: song.cover,
    subtitle: getArtistsName(song)
  }
  showShareDialog.value = true
  shareDialogRendered.value = true
}

// 分享成功回调
const handleShareSuccess = () => {
  ElMessage.success('分享成功，已发布到分享广场')
}

// 清空播放列表
const clearPlaylist = () => {
  playerStore.playlist = []
  playerStore.currentSong = null
  playerStore.isPlaying = false
  playerStore.currentIndex = -1
  // 同步清空进度与时长，避免进度条残留上一首的值
  playerStore.currentTime = 0
  playerStore.duration = 0
  if (audioRef.value) {
    audioRef.value.pause()
    audioRef.value.currentTime = 0
  }
}

// 初始化
onMounted(() => {
  if (audioRef.value) {
    playerStore.initAudio(audioRef.value)
  }
})


</script>

<style scoped>
/* 播放列表弹窗深度定制 */
.playlist-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color-light);
}

.playlist-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.playlist-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.playlist-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
}

.playlist-count {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: normal;
  margin-left: 6px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 列表内容区 */
.playlist-content {
  max-height: 450px;
  overflow-y: auto;
  padding: 8px 0;
}

.playlist-item {
  display: flex;
  align-items: center;
  padding: 10px 24px;
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
  border-radius: var(--radius-sm);
  margin: 0 8px;
}

.playlist-item:hover {
  background-color: var(--bg-secondary);
}

.playlist-item.active {
  background-color: rgba(99, 102, 241, 0.08); /* Indigo tint */
}

/* 左侧序号/波形 */
.song-index-col {
  width: 30px;
  margin-right: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: var(--text-tertiary);
  font-size: 13px;
  font-family: monospace;
}

/* 播放中波形动画 */
.playing-indicator {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 12px;
}

.playing-indicator span {
  width: 2px;
  background-color: var(--color-primary);
  animation: bounce 1s infinite ease-in-out;
}

.playing-indicator span:nth-child(1) { animation-delay: -0.4s; height: 6px; }
.playing-indicator span:nth-child(2) { animation-delay: -0.2s; height: 12px; }
.playing-indicator span:nth-child(3) { animation-delay: -0.5s; height: 8px; }

@keyframes bounce {
  0%, 100% { transform: scaleY(0.5); }
  50% { transform: scaleY(1); }
}

/* 歌曲信息 */
.song-info-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.song-title {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 400;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-artist {
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playlist-item.active .song-title {
  color: var(--color-primary);
  font-weight: 600;
}

.playlist-item.active .song-artist {
  color: var(--color-primary);
  opacity: 0.8;
}

/* 操作区 */
.song-actions-col {
  opacity: 0;
  transition: opacity var(--transition-fast);
  margin-left: 10px;
}

.playlist-item:hover .song-actions-col {
  opacity: 1;
}

.delete-btn {
  font-size: 16px;
  padding: 4px;
  color: var(--text-secondary);
}

.delete-btn:hover {
  color: var(--color-accent);
  background: transparent;
}

/* 空状态 */
.empty-playlist {
  padding: 40px 0;
  display: flex;
  justify-content: center;
}

.music-player {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  width: calc(100% - 48px);
  max-width: 1200px;
  
  /* Glassmorphism */
  background: var(--bg-glass-strong);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  
  z-index: 1000;
  transition: all var(--transition-base);
}

.music-player:hover {
  transform: translateX(-50%) translateY(-4px);
  box-shadow: var(--shadow-xl), var(--shadow-glow);
  border-color: var(--color-primary-light);
}

.player-main {
  display: flex;
  align-items: center;
  padding: 12px 24px;
  gap: 24px;
  height: 88px;
}

/* 左侧 */
.player-left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 260px;
}

.album-cover-wrapper {
  position: relative;
  width: 56px;
  height: 56px;
  cursor: pointer;
  overflow: hidden;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  transition: transform var(--transition-base);
}

.album-cover-wrapper:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow-lg);
}

.song-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: var(--radius-md);
}

.cover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity var(--transition-base);
  pointer-events: none;
  border-radius: var(--radius-md);
}

.album-cover-wrapper:hover .cover-overlay {
  opacity: 1;
}

.expand-icon {
  color: white;
  font-size: 24px;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));
}

.song-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.song-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-artist {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 中间 */
.player-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  align-items: center;
  justify-content: center;
}

.player-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 24px;
  margin-bottom: 4px;
}

/* 普通控制按钮 */
.control-btn {
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 22px;
  width: 36px;
  height: 36px;
  transition: all var(--transition-fast);
}

.control-btn:hover {
  color: var(--color-primary);
  background: rgba(0,0,0,0.05);
  transform: scale(1.1);
}

.control-btn:disabled {
  color: var(--text-tertiary);
  cursor: not-allowed;
  transform: none;
}

/* 播放按钮 */
.play-btn {
  font-size: 28px;
  width: 48px;
  height: 48px;
  background: var(--gradient-primary) !important;
  border: none !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
  transition: all var(--transition-base);
}

.play-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.4);
}

.play-btn:disabled {
  background: var(--text-tertiary) !important;
  box-shadow: none;
  cursor: not-allowed;
  transform: none;
}

/* 进度条 */
.player-progress {
  width: 100%;
  max-width: 480px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.time-text {
  font-size: 11px;
  color: var(--text-secondary);
  width: 35px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.progress-slider {
  flex: 1;
  --el-slider-main-bg-color: var(--color-primary);
  --el-slider-runway-bg-color: rgba(0,0,0,0.05);
  --el-slider-button-size: 12px;
}

:deep(.el-slider__button) {
  border: 2px solid var(--color-primary);
  background-color: #fff;
  transition: transform var(--transition-fast);
}

:deep(.el-slider__button:hover),
:deep(.el-slider__button.dragging) {
  transform: scale(1.2);
}

/* 右侧 */
.player-right {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 260px;
  justify-content: flex-end;
}

.volume-control {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100px;
  color: var(--text-secondary);
}

.volume-slider {
  flex: 1;
  --el-slider-main-bg-color: var(--text-secondary);
  --el-slider-runway-bg-color: rgba(0,0,0,0.05);
  --el-slider-button-size: 10px;
}

.divider {
  width: 1px;
  height: 16px;
  background-color: var(--border-color);
  margin: 0 4px;
}

.action-btn {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 18px;
  width: 32px;
  height: 32px;
  transition: all var(--transition-fast);
}

.action-btn:hover {
  color: var(--text-primary);
  background: rgba(0,0,0,0.05);
}

.action-btn.is-active {
  color: var(--color-primary);
  background: rgba(99, 102, 241, 0.1);
}

/* 歌词面板 */
.lyric-panel {
  position: fixed;
  bottom: 120px;
  right: 24px;
  width: 360px;
  height: 500px;
  background: var(--bg-glass-strong);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: var(--glass-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  z-index: 999;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.lyric-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(0,0,0,0.05);
}

.lyric-header h3 {
  margin: 0;
  font-size: 16px;
  color: var(--text-primary);
  font-weight: 600;
}

.lyric-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  text-align: center;
  /* 隐藏滚动条但保留功能 */
  scrollbar-width: none;
}

.lyric-content::-webkit-scrollbar {
  display: none;
}

.lyric-line {
  padding: 8px 0;
  color: var(--text-secondary);
  font-size: 14px;
  transition: all var(--transition-base);
  cursor: pointer;
  border-radius: 4px;
}

.lyric-line:hover {
  background: rgba(0,0,0,0.02);
}

.lyric-line.active {
  color: var(--color-primary);
  font-size: 18px;
  font-weight: 600;
  text-shadow: 0 2px 8px rgba(99, 102, 241, 0.2);
  transform: scale(1.05);
}

.no-lyric {
  color: var(--text-tertiary);
  margin-top: 100px;
}

/* 动画 */
.lyric-slide-enter-active,
.lyric-slide-leave-active {
  transition: all var(--transition-base);
}

.lyric-slide-enter-from,
.lyric-slide-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .music-player {
    width: 100%;
    bottom: 0;
    left: 0;
    transform: none;
    border-radius: 20px 20px 0 0;
    padding-bottom: env(safe-area-inset-bottom);
  }
  
  .music-player:hover {
    transform: none;
  }
  
  .player-main {
    padding: 10px 16px;
    height: auto;
    flex-wrap: wrap;
  }
  
  .player-left {
    width: 100%;
    min-width: 0;
    margin-bottom: 8px;
  }
  
  .player-center {
    width: 100%;
    order: 2;
  }
  
  .player-right {
    display: none; /* 移动端简化 */
  }
  
  .lyric-panel {
    width: 100%;
    right: 0;
    bottom: 0;
    height: 100%;
    border-radius: 0;
    z-index: 2000;
  }
}

/* ========== 播放队列抽屉（借鉴 Spotify / 网易云） ========== */
.queue-drawer :deep(.el-drawer__body) {
  padding: 0;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  overflow: hidden;
}

.queue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 18px 12px;
  border-bottom: 1px solid var(--border-color-light);
}

.queue-header .playlist-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.queue-header .playlist-count {
  margin-left: 6px;
  font-size: 13px;
  font-weight: 400;
  color: var(--text-secondary);
}

.queue-drawer .playlist-content {
  flex: 1;
  overflow-y: auto;
  padding: 10px 12px 16px;
}

/* ========== 播放条细节（借鉴 Spotify：细进度条 hover 增粗、音量图标分级） ========== */
.volume-icon {
  cursor: pointer;
  font-size: 18px;
  color: var(--text-secondary);
  transition: color var(--transition-fast);
}

.volume-icon:hover {
  color: var(--text-primary);
}

.progress-slider :deep(.el-slider__runway),
.progress-slider :deep(.el-slider__bar) {
  height: 4px;
  transition: height 0.15s ease;
}

.progress-slider :deep(.el-slider__bar) {
  background: var(--gradient-primary);
}

.player-progress:hover .progress-slider :deep(.el-slider__runway),
.player-progress:hover .progress-slider :deep(.el-slider__bar) {
  height: 6px;
}
</style>

