<template>
  <div class="music-player" v-show="playerStore.showPlayer">
    <!-- 隐藏的 audio 元素 -->
    <audio ref="audioRef" preload="metadata" crossorigin="anonymous"></audio>
    
    <!-- 播放器主体 -->
    <div class="player-main">
      <!-- 左侧：歌曲信息 -->
      <div class="player-left">
        <div class="album-cover-wrapper" @click="showFullscreenLyric">
          <img 
            :src="processImageUrl(playerStore.currentSong?.cover) || defaultCover" 
            alt="封面" 
            :class="['song-cover', { 'playing': playerStore.isPlaying }]"
          >
          <div class="cover-overlay">
            <el-icon class="expand-icon"><FullScreen /></el-icon>
          </div>
        </div>
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
            @click="playerStore.togglePlay"
            :disabled="!playerStore.currentSong"
          >
            <el-icon v-if="playerStore.isPlaying"><VideoPause /></el-icon>
            <el-icon v-else><VideoPlay /></el-icon>
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
            @change="handleSeek"
            class="progress-slider"
          />
          <span class="time-text">{{ formatTime(playerStore.duration) }}</span>
        </div>
      </div>
      
      <!-- 右侧：其他控制 -->
      <div class="player-right">
        <!-- 音量控制 -->
        <div class="volume-control">
          <el-icon><Headset /></el-icon>
          <el-slider 
            v-model="volumeValue" 
            :show-tooltip="false"
            @input="handleVolumeChange"
            class="volume-slider"
          />
        </div>

        <div class="divider"></div>

        <!-- 播放模式切换 -->
        <el-tooltip :content="playModeText" placement="top">
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
        <el-tooltip content="歌词" placement="top">
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
        <el-tooltip content="播放列表" placement="top">
          <el-button 
            class="action-btn" 
            circle
            @click="showPlaylistDialog = true"
          >
            <el-icon><List /></el-icon>
          </el-button>
        </el-tooltip>

        <!-- 歌曲评论入口 -->
        <el-tooltip content="评论" placement="top">
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
        <el-tooltip content="分享" placement="top">
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
      width="480px"
      append-to-body
      class="playlist-dialog"
      :show-close="false"
    >
      <template #header="{ close, titleId, titleClass }">
        <div class="playlist-header">
          <div class="playlist-title">
            <span>播放列表</span>
            <span class="playlist-count">({{ playerStore.playlist.length }})</span>
          </div>
          <div class="header-actions">
            <el-button link type="info" @click="clearPlaylist" :disabled="!playerStore.playlist.length">
              <el-icon><Delete /></el-icon> 清空
            </el-button>
            <el-button link class="close-btn" @click="close">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>
      </template>
      
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
            <div v-if="index === playerStore.currentIndex" class="playing-indicator">
              <span></span><span></span><span></span>
            </div>
            <span v-else class="index-num">{{ index + 1 }}</span>
          </div>
          
          <!-- 信息 -->
          <div class="song-info-col">
            <div class="song-title" :title="song.title">{{ song.title }}</div>
            <div class="song-artist" :title="getArtistsName(song)">{{ getArtistsName(song) }}</div>
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
        
        <div v-if="playerStore.playlist.length === 0" class="empty-playlist">
          <el-empty description="播放列表为空" :image-size="100" />
        </div>
      </div>
    </el-dialog>

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
        <el-button @click="showCommentsDialog = false">关闭</el-button>
      </template>
    </el-dialog>
    
    <!-- 分享弹窗 -->
    <ShareDialog
      v-model="showShareDialog"
      type="song"
      :share-data="shareData"
      @success="handleShareSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick, inject } from 'vue'
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
  Delete
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import CommentList from '@/components/Social/CommentList.vue'
import ShareDialog from '@/components/ShareDialog.vue'

const playerStore = usePlayerStore()
const userStore = useUserStore()
const router = useRouter()
const audioRef = ref(null)
const lyricContentRef = ref(null)
const showPlaylistDialog = ref(false)
const showCommentsDialog = ref(false)
const showShareDialog = ref(false)
const shareData = ref({})
const defaultCover = '/default-cover.svg'

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

// 处理 OSS URL 为本地代理路径
const processImageUrl = (url) => {
  if (!url) return url
  
  const ossHost = 'https://sheepmusic.oss-cn-hangzhou.aliyuncs.com'
  
  if (url.startsWith(ossHost)) {
    try {
      const u = new URL(url)
      // 开发环境使用 /oss 代理
      if (import.meta.env.DEV) {
        return `/oss${u.pathname}`
      }
    } catch (e) {
      console.warn('URL 处理失败:', e)
    }
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
  border-bottom: 1px solid rgba(0,0,0,0.06);
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
  color: #303133;
  display: flex;
  align-items: center;
}

.playlist-count {
  font-size: 12px;
  color: #909399;
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
  transition: all 0.2s;
  position: relative;
}

.playlist-item:hover {
  background-color: var(--bg-secondary, #f5f7fa);
}

.playlist-item.active {
  background-color: rgba(64, 158, 255, 0.06);
}

/* 左侧序号/波形 */
.song-index-col {
  width: 30px;
  margin-right: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #c0c4cc;
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
  background-color: var(--primary-color, #409eff);
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
  color: #303133;
  font-weight: 400;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-artist {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playlist-item.active .song-title {
  color: var(--primary-color, #409eff);
  font-weight: 500;
}

.playlist-item.active .song-artist {
  color: var(--primary-color, #409eff);
  opacity: 0.8;
}

/* 操作区 */
.song-actions-col {
  opacity: 0;
  transition: opacity 0.2s;
  margin-left: 10px;
}

.playlist-item:hover .song-actions-col {
  opacity: 1;
}

.delete-btn {
  font-size: 16px;
  padding: 4px;
  color: #909399;
}

.delete-btn:hover {
  color: #f56c6c;
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
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08), 0 4px 12px rgba(0, 0, 0, 0.04);
  z-index: 1000;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.music-player:hover {
  transform: translateX(-50%) translateY(-4px);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.12), 0 8px 24px rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.95);
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
  border-radius: 12px; /* 改为圆角矩形，更现代 */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.album-cover-wrapper:hover {
  transform: scale(1.05);
}

.song-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 12px;
  /* 移除旋转动画，因为改成圆角矩形了，旋转会很奇怪，改用缩放交互 */
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
  transition: opacity 0.3s ease;
  pointer-events: none;
  border-radius: 12px;
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
  color: var(--text-primary, #303133);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-artist {
  font-size: 12px;
  color: var(--text-secondary, #909399);
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
  color: var(--text-primary, #303133);
  font-size: 22px;
  width: 36px;
  height: 36px;
  transition: all 0.2s;
}

.control-btn:hover {
  color: var(--primary-color, #409eff);
  background: rgba(0,0,0,0.05);
  transform: scale(1.1);
}

/* 播放/暂停按钮 */
.play-btn {
  width: 48px;
  height: 48px;
  font-size: 24px;
  background: var(--primary-color, #409eff);
  color: white;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transition: all 0.2s;
  border: none;
}

.play-btn:hover {
  background: #66b1ff;
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
}

.player-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  max-width: 420px;
  height: 16px;
}

.time-text {
  font-size: 11px;
  color: #999;
  min-width: 40px;
  text-align: center;
  font-variant-numeric: tabular-nums;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, monospace;
}

.progress-slider {
  flex: 1;
}

/* 进度条深度定制 */
.player-progress :deep(.el-slider__runway) {
  height: 4px;
  margin: 6px 0;
  background-color: rgba(0,0,0,0.06);
  border-radius: 2px;
}

.player-progress :deep(.el-slider__bar) {
  height: 4px;
  background-color: var(--primary-color);
  border-radius: 2px;
}

.player-progress :deep(.el-slider__button) {
  width: 10px;
  height: 10px;
  border: 2px solid var(--primary-color);
  background-color: #fff;
  transition: transform 0.2s;
}

.player-progress :deep(.el-slider__button-wrapper) {
  width: 24px;
  height: 24px;
  top: -10px;
}

.player-progress :deep(.el-slider__button-wrapper:hover .el-slider__button) {
  transform: scale(1.3);
}

/* 右侧 */
.player-right {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 260px;
  justify-content: flex-end;
}

.divider {
  width: 1px;
  height: 20px;
  background: rgba(0,0,0,0.08);
  margin: 0 4px;
}

/* 功能按钮样式 */
.action-btn {
  width: 34px;
  height: 34px;
  font-size: 18px;
  color: #606266;
  border: none !important;
  background: transparent !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}

.action-btn:hover {
  background: rgba(0,0,0,0.04) !important;
  color: var(--primary-color, #409eff);
  transform: translateY(-2px);
}

.action-btn.is-active {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%) !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transform: translateY(-1px);
}

.action-btn.is-disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn.is-disabled:hover {
  transform: none;
  background: transparent !important;
  color: #909399;
}

/* 音量控制优化 */
.volume-control {
  display: flex;
  align-items: center;
  background: rgba(0,0,0,0.03);
  padding: 0 12px;
  height: 32px;
  border-radius: 16px;
  transition: all 0.3s;
  width: 110px;
}

.volume-control:hover {
  background: rgba(0,0,0,0.06);
  width: 120px;
}

.volume-control .el-icon {
  font-size: 16px;
  color: #909399;
  margin-right: 8px;
}

.volume-slider {
  flex: 1;
  --el-slider-main-bg-color: #909399;
  --el-slider-runway-bg-color: rgba(0,0,0,0.06);
  --el-slider-button-size: 12px;
}

.volume-slider :deep(.el-slider__bar) {
  background-color: #909399;
}

.volume-control:hover .volume-slider :deep(.el-slider__bar) {
  background-color: var(--primary-color, #409eff);
}

.volume-slider :deep(.el-slider__button) {
  width: 10px;
  height: 10px;
  border: none;
  background-color: white;
  box-shadow: 0 1px 3px rgba(0,0,0,0.3);
  transition: all 0.2s;
}

.volume-slider :deep(.el-slider__button:hover) {
  transform: scale(1.2);
}

.volume-slider :deep(.el-slider__runway) {
  margin: 0;
  height: 4px;
}

/* 歌词面板 - 悬浮在上方 */
.lyric-panel {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.5);
  overflow: hidden;
  transform-origin: bottom center;
}

.lyric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  border-bottom: 1px solid rgba(0,0,0,0.05);
}

.lyric-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.lyric-content {
  height: 320px;
  overflow-y: auto;
  padding: 24px;
  scroll-behavior: smooth;
  mask-image: linear-gradient(to bottom, transparent, black 10%, black 90%, transparent);
  -webkit-mask-image: linear-gradient(to bottom, transparent, black 10%, black 90%, transparent);
}

.lyric-line {
  text-align: center;
  padding: 10px 0;
  color: #999;
  font-size: 15px;
  line-height: 1.6;
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  opacity: 0.6;
  filter: blur(0.5px);
}

.lyric-line.active {
  color: var(--primary-color, #409eff);
  font-size: 18px;
  font-weight: 600;
  transform: scale(1.05);
  opacity: 1;
  filter: blur(0);
  text-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.no-lyric {
  text-align: center;
  color: #999;
  padding: 80px 0;
  font-size: 14px;
}

/* 播放列表弹窗样式优化 */
.playlist-content {
  max-height: 400px;
  overflow-y: auto;
  padding: 8px;
}

.playlist-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.playlist-item:hover {
  background: var(--bg-secondary, #f5f7fa);
}

.playlist-item.active {
  background: var(--primary-light, #ecf5ff);
}

.playlist-item.active .playlist-item-name {
  color: var(--primary-color, #409eff);
  font-weight: 600;
}

/* 动画 */
.lyric-slide-enter-active,
.lyric-slide-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.lyric-slide-enter-from,
.lyric-slide-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

/* 响应式 */
@media (max-width: 768px) {
  .music-player {
    width: calc(100% - 32px);
    bottom: 16px;
    padding: 0;
  }
  
  .player-main {
    padding: 10px 16px;
    height: 72px;
    gap: 12px;
  }

  .player-left {
    min-width: auto;
    flex: 1;
  }

  .album-cover-wrapper {
    width: 48px;
    height: 48px;
  }
  
  .player-center {
    position: absolute;
    top: -4px;
    left: 0;
    right: 0;
    height: 2px;
    flex: none;
    gap: 0;
  }
  
  .player-controls {
    display: none; /* 移动端简化，根据需要可调整 */
  }
  
  .player-progress {
    padding: 0;
    max-width: none;
  }
  
  .player-progress .el-slider {
    margin: 0;
    height: 2px;
  }
  
  .player-progress .el-slider__button-wrapper {
    display: none;
  }
  
  .time-text {
    display: none;
  }
  
  .player-right {
    min-width: auto;
    gap: 4px;
  }
  
  .volume-control {
    display: none;
  }
}
</style>

