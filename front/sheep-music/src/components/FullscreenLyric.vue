<template>
  <transition name="lyric-fade">
    <div
      v-if="visible"
      class="fullscreen-lyric"
      @click="handleClose"
    >
      <!-- 背景模糊层 -->
      <div class="lyric-backdrop" />
      
      <!-- 主内容区 -->
      <div
        class="lyric-container"
        @click.stop
      >
        <!-- 顶部操作栏 -->
        <div class="lyric-header">
          <el-button
            circle
            size="small"
            @click="handleClose"
          >
            <el-icon><ArrowDown /></el-icon>
          </el-button>
          <span class="song-title">{{ currentSong?.title }}</span>
          <el-button
            circle
            size="small"
            @click="toggleDesktopLyric"
          >
            <el-icon><Monitor /></el-icon>
          </el-button>
        </div>
        
        <!-- 中间内容区 -->
        <div class="lyric-main">
          <!-- 左侧专辑封面 -->
          <div class="album-section">
            <div class="album-wrapper">
              <img 
                :src="processImageUrl(currentSong?.cover) || defaultCover" 
                :class="['album-cover', { 'playing': isPlaying }]"
                alt="专辑封面"
              >
              <div class="album-shadow" />
            </div>
            
            <!-- 歌曲信息 -->
            <div class="song-meta">
              <h2 class="song-name">
                {{ currentSong?.title || '暂无播放' }}
              </h2>
              <p class="artist-name">
                {{ getArtistsName(currentSong) }}
              </p>
              <p class="album-name">
                {{ currentSong?.albumName || '未知专辑' }}
              </p>
            </div>
          </div>
          
          <!-- 右侧歌词 -->
          <div class="lyric-section">
            <div 
              ref="lyricScrollRef"
              class="lyric-scroll"
              @wheel="handleWheel"
              @touchstart="handleTouchStart"
              @touchmove="handleTouchMove"
              @touchend="handleTouchEnd"
            >
              <div
                ref="lyricContentRef"
                class="lyric-content"
                :style="{ transform: `translateY(${lyricOffset}px)` }"
              >
                <!-- 顶部占位 -->
                <div class="lyric-placeholder" />
                
                <!-- 歌词行 -->
                <div
                  v-for="(line, index) in lyrics"
                  :key="index"
                  :data-lyric-index="index"
                  :class="[
                    'lyric-line',
                    { 
                      'active': index === activeLyricIndex,
                      'passed': index < activeLyricIndex
                    }
                  ]"
                  @click="seekToLine(index)"
                >
                  <p class="lyric-text">
                    {{ line.text }}
                  </p>
                  <p
                    v-if="line.translation"
                    class="lyric-translation"
                  >
                    {{ line.translation }}
                  </p>
                </div>
                
                <!-- 底部占位 -->
                <div class="lyric-placeholder" />
              </div>
            </div>
            
            <!-- 时间轴指示器（拖动时显示） -->
            <transition name="fade">
              <div
                v-if="isDragging"
                class="time-indicator"
              >
                <div class="time-line" />
                <span class="time-text">{{ formatTime(dragTime) }}</span>
              </div>
            </transition>
          </div>
        </div>
        
        <!-- 底部播放控制 -->
        <div class="lyric-footer">
          <!-- 进度条 -->
          <div class="progress-wrapper">
            <span class="time">{{ formatTime(currentTime) }}</span>
            <el-slider 
              v-model="progressValue"
              :show-tooltip="false"
              class="progress-bar"
              @change="handleSeek"
            />
            <span class="time">{{ formatTime(duration) }}</span>
          </div>
          
          <!-- 控制按钮 -->
          <div class="control-buttons">
            <el-button
              circle
              class="control-btn"
              @click="togglePlayMode"
            >
              <el-icon>
                <Refresh v-if="playMode === 'list'" />
                <Promotion v-if="playMode === 'random'" />
                <RefreshLeft v-if="playMode === 'single'" />
              </el-icon>
            </el-button>
            
            <el-button
              circle
              class="control-btn"
              :disabled="!hasPrev"
              @click="playPrev"
            >
              <el-icon><CaretLeft /></el-icon>
            </el-button>
            
            <el-button
              circle
              class="play-btn"
              @click="togglePlay"
            >
              <el-icon v-if="isPlaying">
                <VideoPause />
              </el-icon>
              <el-icon v-else>
                <VideoPlay />
              </el-icon>
            </el-button>
            
            <el-button
              circle
              class="control-btn"
              :disabled="!hasNext"
              @click="playNext"
            >
              <el-icon><CaretRight /></el-icon>
            </el-button>
            
            <el-button
              circle
              class="control-btn"
              @click="toggleFavorite"
            >
              <el-icon :style="{ color: isFavorite ? '#f56c6c' : '' }">
                <Star v-if="!isFavorite" />
                <StarFilled v-else />
              </el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { usePlayerStore } from '@/store/player'
import { getSongLyric } from '@/api/lyric'
import { 
  ArrowDown, Monitor, CaretLeft, CaretRight, 
  VideoPause, VideoPlay, Refresh, Promotion, 
  RefreshLeft, Star, StarFilled 
} from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

const playerStore = usePlayerStore()
const lyricScrollRef = ref(null)
const lyricContentRef = ref(null)
const lyricOffset = ref(0)
const activeLyricIndex = ref(0)
const isDragging = ref(false)
const dragTime = ref(0)
const touchStartY = ref(0)
const lastTouchY = ref(0)
const autoScroll = ref(true)
let centerFrameId = null
let resumeAutoScrollTimer = null
let lyricResizeObserver = null

// 歌词数据
const lyrics = ref([])
const isLoadingLyric = ref(false)

// 解析 LRC 格式歌词
const parseLyric = (lrcText) => {
  if (!lrcText) return []
  
  const lines = lrcText.split('\n')
  const result = []
  
  for (const line of lines) {
    const match = line.match(/\[(\d{2}):(\d{2})\.?(\d{2,3})?\](.*)/)
    if (match) {
      const minutes = parseInt(match[1])
      const seconds = parseInt(match[2])
      const milliseconds = match[3] ? parseInt(match[3].padEnd(3, '0')) : 0
      const time = minutes * 60 + seconds + milliseconds / 1000
      const text = match[4].trim()
      
      if (text) {
        result.push({ time, text, translation: '' })
      }
    }
  }
  
  return result.sort((a, b) => a.time - b.time)
}

// 加载歌词
const loadLyric = async (songId) => {
  if (!songId) {
    lyrics.value = [{ time: 0, text: '暂无歌词', translation: '' }]
    return
  }
  
  isLoadingLyric.value = true
  
  try {
    const res = await getSongLyric(songId)
    console.log('歌词响应:', res)
    
    // 处理不同的响应格式
    let lyricText = ''
    if (res.code === 200 && res.data) {
      // 后端返回的是 Result 格式
      lyricText = res.data
    } else if (res.data && typeof res.data === 'string') {
      lyricText = res.data
    } else if (res.lyric) {
      lyricText = res.lyric
    }
    
    if (lyricText && lyricText.trim()) {
      lyrics.value = parseLyric(lyricText)
      if (lyrics.value.length === 0) {
        lyrics.value = [{ time: 0, text: '纯音乐，请欣赏', translation: '' }]
      }
    } else {
      lyrics.value = [{ time: 0, text: '暂无歌词', translation: '' }]
    }
  } catch (error) {
    console.error('获取歌词失败:', error)
    lyrics.value = [{ time: 0, text: '歌词加载失败', translation: '' }]
  } finally {
    isLoadingLyric.value = false
  }
}

const defaultCover = '/default-cover.svg'

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

// 计算属性
const currentSong = computed(() => playerStore.currentSong)
const isPlaying = computed(() => playerStore.isPlaying)
const currentTime = computed(() => playerStore.currentTime)
const duration = computed(() => playerStore.duration)
const playMode = computed(() => playerStore.playMode)
const hasPrev = computed(() => playerStore.hasPrevSong)
const hasNext = computed(() => playerStore.hasNextSong)
const isFavorite = ref(false)

const progressValue = computed({
  get: () => duration.value ? (currentTime.value / duration.value) * 100 : 0,
  set: (val) => {
    const time = (val / 100) * duration.value
    playerStore.seek(time)
  }
})

// 获取艺术家名称
const getArtistsName = (song) => {
  if (!song) return '未知艺术家'
  if (song.artists && song.artists.length > 0) {
    return song.artists.map(a => a.name).join(' / ')
  }
  return song.artistName || '未知艺术家'
}

// 格式化时间
const formatTime = (seconds) => {
  if (!seconds || seconds < 0) return '00:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 使用真实 DOM 尺寸居中当前歌词。歌词字号、换行和翻译都会改变行高，不能使用固定值估算。
const scrollToActiveLyric = async () => {
  if (!autoScroll.value) return

  await nextTick()

  if (centerFrameId) cancelAnimationFrame(centerFrameId)
  centerFrameId = requestAnimationFrame(() => {
    centerFrameId = requestAnimationFrame(() => {
      const container = lyricScrollRef.value
      const content = lyricContentRef.value
      const activeLine = content?.querySelector(`[data-lyric-index="${activeLyricIndex.value}"]`)
      if (!container || !activeLine) return

      const lineCenter = activeLine.offsetTop + activeLine.offsetHeight / 2
      const targetOffset = container.clientHeight / 2 - lineCenter
      if (Number.isFinite(targetOffset)) {
        lyricOffset.value = Math.round(targetOffset)
      }
    })
  })
}

const syncActiveLyric = (time) => {
  if (!lyrics.value.length) return

  let targetIndex = 0
  for (let i = lyrics.value.length - 1; i >= 0; i--) {
    if (time >= lyrics.value[i].time) {
      targetIndex = i
      break
    }
  }
  activeLyricIndex.value = targetIndex
}

// 根据屏幕中央最近的真实歌词行计算拖动目标，不再按固定行高反推。
const getCenteredLyricIndex = () => {
  const container = lyricScrollRef.value
  const content = lyricContentRef.value
  if (!container || !content) return activeLyricIndex.value

  const containerRect = container.getBoundingClientRect()
  const centerY = containerRect.top + containerRect.height / 2
  const lines = Array.from(content.querySelectorAll('.lyric-line'))
  let nearestIndex = activeLyricIndex.value
  let nearestDistance = Number.POSITIVE_INFINITY

  lines.forEach((line, index) => {
    const rect = line.getBoundingClientRect()
    const distance = Math.abs(rect.top + rect.height / 2 - centerY)
    if (distance < nearestDistance) {
      nearestDistance = distance
      nearestIndex = index
    }
  })

  return nearestIndex
}

const observeLyricLayout = () => {
  lyricResizeObserver?.disconnect()
  if (!lyricScrollRef.value || !lyricContentRef.value || typeof ResizeObserver === 'undefined') return

  lyricResizeObserver = new ResizeObserver(() => {
    if (props.visible && autoScroll.value) scrollToActiveLyric()
  })
  lyricResizeObserver.observe(lyricScrollRef.value)
  lyricResizeObserver.observe(lyricContentRef.value)
}

// 监听当前时间变化，更新活动歌词
watch(currentTime, (time) => {
  if (!lyrics.value.length) return

  const previousIndex = activeLyricIndex.value
  syncActiveLyric(time)
  if (activeLyricIndex.value !== previousIndex) {
    scrollToActiveLyric()
  }
})

// 处理滚轮事件
const handleWheel = (e) => {
  e.preventDefault()
  autoScroll.value = false
  lyricOffset.value += e.deltaY * -0.5
  
  // 3秒后恢复自动滚动
  clearTimeout(resumeAutoScrollTimer)
  resumeAutoScrollTimer = setTimeout(() => {
    autoScroll.value = true
    scrollToActiveLyric()
  }, 3000)
}

// 触摸事件处理
const handleTouchStart = (e) => {
  touchStartY.value = e.touches[0].clientY
  lastTouchY.value = e.touches[0].clientY
  isDragging.value = true
  autoScroll.value = false
}

const handleTouchMove = (e) => {
  if (!isDragging.value) return
  
  const deltaY = e.touches[0].clientY - lastTouchY.value
  lyricOffset.value += deltaY
  lastTouchY.value = e.touches[0].clientY
  
  const targetIndex = getCenteredLyricIndex()
  dragTime.value = lyrics.value[targetIndex]?.time || 0
}

const handleTouchEnd = () => {
  isDragging.value = false
  
  // 如果拖动距离较大，跳转到对应时间
  const deltaY = Math.abs(touchStartY.value - lastTouchY.value)
  if (deltaY > 50) {
    playerStore.seek(dragTime.value)
  }
  
  // 3秒后恢复自动滚动
  clearTimeout(resumeAutoScrollTimer)
  resumeAutoScrollTimer = setTimeout(() => {
    autoScroll.value = true
    scrollToActiveLyric()
  }, 3000)
}

// 点击歌词跳转
const seekToLine = (index) => {
  const time = lyrics.value[index]?.time
  if (time !== undefined) {
    autoScroll.value = true
    playerStore.seek(time)
    activeLyricIndex.value = index
    scrollToActiveLyric()
  }
}

// 控制方法
const handleClose = () => {
  emit('close')
}

const toggleDesktopLyric = () => {
  playerStore.toggleDesktopLyric()
}

const togglePlay = () => {
  playerStore.togglePlay()
}

const playPrev = () => {
  playerStore.prev()
}

const playNext = () => {
  playerStore.next()
}

const togglePlayMode = () => {
  playerStore.togglePlayMode()
}

const handleSeek = (val) => {
  const time = (val / 100) * duration.value
  playerStore.seek(time)
}

const toggleFavorite = () => {
  isFavorite.value = !isFavorite.value
  // TODO: 调用收藏 API
}

// 监听可见性变化
watch(() => props.visible, async (val) => {
  if (val) {
    console.log('全屏歌词打开')
    
    // 强制启用自动滚动
    autoScroll.value = true
    console.log('自动滚动已启用:', autoScroll.value)
    
    // 加载当前歌曲的歌词
    if (currentSong.value?.id) {
      await loadLyric(currentSong.value.id)
    }
    
    // 等待 DOM 更新后滚动到当前位置
    await nextTick()
    
    syncActiveLyric(currentTime.value)
    observeLyricLayout()
    scrollToActiveLyric()
  } else {
    lyricResizeObserver?.disconnect()
    console.log('全屏歌词关闭')
  }
})

// 监听歌曲变化
watch(() => currentSong.value?.id, async (newId) => {
  if (newId && props.visible) {
    autoScroll.value = true
    await loadLyric(newId)
    syncActiveLyric(currentTime.value)
    await nextTick()
    observeLyricLayout()
    scrollToActiveLyric()
  }
})

onBeforeUnmount(() => {
  if (centerFrameId) cancelAnimationFrame(centerFrameId)
  clearTimeout(resumeAutoScrollTimer)
  lyricResizeObserver?.disconnect()
})
</script>

<style scoped>
.fullscreen-lyric {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lyric-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(20px);
}

.lyric-container {
  position: relative;
  width: 90%;
  height: 90%;
  max-width: 1400px;
  display: flex;
  flex-direction: column;
  color: #fff;
}

/* 顶部栏 */
.lyric-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  opacity: 0.8;
}

.song-title {
  font-size: 14px;
  opacity: 0.7;
}

/* 主内容区 */
.lyric-main {
  flex: 1;
  display: flex;
  gap: 30px;
  padding: 0 40px;
  overflow: hidden;
  align-items: center;
}

/* 左侧专辑 */
.album-section {
  flex: 0 0 350px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.album-wrapper {
  position: relative;
  margin-bottom: 30px;
}

.album-cover {
  width: 260px;
  height: 260px;
  border-radius: 50%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  animation: rotate 20s linear infinite paused;
}

.album-cover.playing {
  animation-play-state: running;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.album-shadow {
  position: absolute;
  bottom: -20px;
  left: 50%;
  transform: translateX(-50%);
  width: 220px;
  height: 30px;
  background: radial-gradient(ellipse at center, rgba(0, 0, 0, 0.4), transparent);
  filter: blur(20px);
}

.song-meta {
  text-align: center;
}

.song-name {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 8px;
}

.artist-name {
  font-size: 16px;
  opacity: 0.8;
  margin-bottom: 4px;
}

.album-name {
  font-size: 13px;
  opacity: 0.6;
}

/* 右侧歌词 */
.lyric-section {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.lyric-scroll {
  width: 100%;
  height: 100%;
  max-height: 50vh;
  overflow: hidden;
  position: relative;
  mask-image: linear-gradient(
    to bottom,
    transparent 0%,
    black 15%,
    black 85%,
    transparent 100%
  );
}

.lyric-content {
  transition: transform 0.5s ease-out;
}

.lyric-placeholder {
  height: calc(50vh - 60px);
}

.lyric-line {
  padding: 20px;
  cursor: pointer;
  transition: opacity 0.3s ease, transform 0.3s ease;
  opacity: 0.4;
  transform: scale(0.95);
}

.lyric-line:hover {
  opacity: 0.7;
}

.lyric-line.active {
  opacity: 1;
  transform: scale(1.05);
}

.lyric-line.active .lyric-text {
  font-size: 32px;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.95);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.lyric-line.passed {
  opacity: 0.3;
}

.lyric-text {
  margin: 0;
  font-size: 24px;
  line-height: 1.5;
  transition: color 0.3s ease, text-shadow 0.3s ease, opacity 0.3s ease;
}

.lyric-translation {
  margin: 5px 0 0;
  font-size: 16px;
  opacity: 0.7;
}

/* 时间指示器 */
.time-indicator {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  pointer-events: none;
  z-index: 2;
}

.time-line {
  flex: 1;
  height: 2px;
  background: rgba(102, 126, 234, 0.8);
  box-shadow: 0 0 10px rgba(102, 126, 234, 0.5);
}

.time-text {
  padding: 4px 12px;
  margin: 0 20px;
  font-size: 14px;
  font-weight: bold;
  background: rgba(102, 126, 234, 0.9);
  color: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

/* 底部控制栏 */
.lyric-footer {
  padding: 40px 60px 60px;
  max-width: 1000px;
  margin: 0 auto;
  width: 100%;
}

.progress-wrapper {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 40px;
}

.time {
  font-size: 13px;
  opacity: 0.8;
  min-width: 45px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.5px;
}

/* 进度条优化 */
.progress-bar {
  flex: 1;
}

.progress-bar :deep(.el-slider__runway) {
  height: 4px;
  background-color: rgba(255, 255, 255, 0.15);
  border-radius: 2px;
}

.progress-bar :deep(.el-slider__bar) {
  height: 4px;
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 2px;
}

.progress-bar :deep(.el-slider__button) {
  width: 10px;
  height: 10px;
  background-color: #fff;
  border: none;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
  transition: transform 0.2s;
}

.progress-bar :deep(.el-slider__button-wrapper:hover .el-slider__button) {
  transform: scale(1.2);
}

.control-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
}

/* 按钮样式 */
.control-btn {
  width: 42px;
  height: 42px;
  font-size: 20px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
  transition: all 0.3s;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  transform: scale(1.05);
  border-color: rgba(255, 255, 255, 0.3);
}

.control-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
  transform: none;
}

/* 播放按钮 */
.play-btn {
  width: 60px;
  height: 60px;
  font-size: 28px;
  background: rgba(255, 255, 255, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
  backdrop-filter: blur(10px);
  transition: all 0.3s;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}

.play-btn:hover {
  background: rgba(255, 255, 255, 0.35);
  transform: scale(1.05);
  box-shadow: 0 12px 48px rgba(0,0,0,0.3);
}
.lyric-fade-enter-active,
.lyric-fade-leave-active {
  transition: all 0.3s ease;
}

.lyric-fade-enter-from,
.lyric-fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .lyric-container {
    width: 100%;
    height: 100%;
  }

  .lyric-header {
    flex: 0 0 auto;
    padding: 14px 18px 8px;
  }

  .lyric-main {
    display: grid;
    grid-template-rows: auto minmax(180px, 1fr);
    min-height: 0;
    gap: 12px;
    padding: 0 18px;
    align-items: stretch;
  }
  
  .album-section {
    flex: none;
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
    gap: 16px;
    min-height: 108px;
  }

  .album-wrapper {
    flex: 0 0 auto;
    margin-bottom: 0;
  }
  
  .album-cover {
    width: 96px;
    height: 96px;
  }

  .album-shadow {
    display: none;
  }

  .song-meta {
    min-width: 0;
    text-align: left;
  }

  .song-name {
    overflow: hidden;
    margin: 0 0 6px;
    font-size: 20px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .artist-name {
    overflow: hidden;
    margin: 0;
    font-size: 14px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .album-name {
    display: none;
  }

  .lyric-section {
    min-height: 0;
    height: auto;
  }
  
  .lyric-scroll {
    height: 100%;
    max-height: none;
    min-height: 180px;
  }
  
  .lyric-text {
    font-size: 18px;
  }
  
  .lyric-line.active .lyric-text {
    font-size: 24px;
  }

  .lyric-line {
    padding: 14px 10px;
    text-align: center;
  }

  .lyric-footer {
    flex: 0 0 auto;
    width: 100%;
    padding: 12px 18px 18px;
  }

  .progress-wrapper {
    gap: 10px;
    margin-bottom: 14px;
  }

  .control-buttons {
    gap: 12px;
  }

  .control-btn {
    width: 38px;
    height: 38px;
    font-size: 17px;
  }

  .play-btn {
    width: 50px;
    height: 50px;
    font-size: 22px;
  }
}
</style>
