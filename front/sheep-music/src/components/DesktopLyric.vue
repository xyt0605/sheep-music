<template>
  <teleport to="body" :disabled="!isMounted">
    <transition name="slide-down">
      <div 
        v-if="visible && isMounted" 
        class="desktop-lyric"
        :class="{
          locked: isLocked,
          dragging: isDragging,
          resizing: isResizing
        }"
        :style="{
          top: position.y + 'px',
          left: position.x + 'px',
          width: width + 'px',
          opacity: opacity,
          backgroundColor: backgroundColor
        }"
        @mousedown.stop="handleMouseDown"
        @dblclick.stop="toggleLock"
      >
        <!-- 控制栏（鼠标悬停显示） -->
        <div class="lyric-controls" v-show="!isLocked || showControls">
          <!-- 拖动手柄 -->
          <div class="drag-handle" title="双击锁定/解锁">
            <el-icon><Rank /></el-icon>
            {{ isLocked ? '🔒' : '🔓' }}
          </div>
          
          <!-- 功能按钮 -->
          <div class="control-buttons">
            <!-- 透明度调节 -->
            <el-popover 
              placement="bottom" 
              :width="220" 
              trigger="click"
              popper-class="lyric-control-popover"
            >
              <template #reference>
                <el-button icon="Sunny" circle size="small" title="透明度" />
              </template>
              <div class="opacity-control">
                <div class="control-title">透明度</div>
                <div class="control-value">{{ Math.round(opacity * 100) }}%</div>
                <el-slider 
                  v-model="opacity" 
                  :min="0.3" 
                  :max="1" 
                  :step="0.1"
                  @change="saveSettings"
                />
              </div>
            </el-popover>
            
            <!-- 背景色调节 -->
            <el-popover 
              placement="bottom" 
              :width="240" 
              trigger="click"
              popper-class="lyric-color-popover"
            >
              <template #reference>
                <el-button icon="Brush" circle size="small" title="背景色" />
              </template>
              <div class="color-control">
                <div class="color-control-title">背景颜色</div>
                <div class="color-presets">
                  <div 
                    v-for="color in colorPresets" 
                    :key="color"
                    class="color-item"
                    :class="{ active: backgroundColor === color }"
                    :style="{ backgroundColor: color }"
                    @click="changeBackgroundColor(color)"
                    :title="getColorName(color)"
                  >
                    <el-icon v-if="backgroundColor === color" class="check-icon">
                      <Check />
                    </el-icon>
                  </div>
                </div>
              </div>
            </el-popover>
            
            <!-- 字体大小 -->
            <el-popover 
              placement="bottom" 
              :width="220" 
              trigger="click"
              popper-class="lyric-control-popover"
            >
              <template #reference>
                <el-button icon="FontSize" circle size="small" title="字体大小" />
              </template>
              <div class="font-control">
                <div class="control-title">字体大小</div>
                <div class="control-value">{{ fontSize }}px</div>
                <el-slider 
                  v-model="fontSize" 
                  :min="16" 
                  :max="48" 
                  :step="2"
                  @change="saveSettings"
                />
              </div>
            </el-popover>
            
            <!-- 重置位置 -->
            <el-button 
              icon="RefreshRight" 
              circle 
              size="small" 
              title="重置位置"
              @click="resetPosition"
            />
            
            <!-- 关闭 -->
            <el-button 
              icon="Close" 
              circle 
              size="small" 
              title="关闭桌面歌词"
              @click="close"
            />
          </div>
        </div>
        
        <!-- 歌词显示区域 -->
        <div class="lyric-content" :style="{ fontSize: fontSize + 'px' }">
          <transition name="lyric-fade" mode="out-in">
            <div :key="currentLyric" class="lyric-line">
              {{ currentLyric || '暂无歌词' }}
            </div>
          </transition>
          
          <!-- 下一句歌词（可选） -->
          <transition name="fade">
            <div v-if="nextLyric && showNextLine" class="lyric-next">
              {{ nextLyric }}
            </div>
          </transition>
        </div>
        
        <!-- 歌曲信息（可选显示） -->
        <transition name="fade">
          <div v-if="showSongInfo" class="song-info">
            <span class="song-title">{{ currentSong?.title || '未播放' }}</span>
            <span class="song-artist">
              {{ currentSong?.artists?.map(a => a.name).join(' / ') || '' }}
            </span>
          </div>
        </transition>
        
        <!-- 调整大小手柄（仅在未锁定时显示） -->
        <div 
          v-if="!isLocked"
          class="resize-handle resize-left"
          @mousedown.stop="startResize($event, 'left')"
          title="拖拽调整宽度"
        />
        <div 
          v-if="!isLocked"
          class="resize-handle resize-right"
          @mousedown.stop="startResize($event, 'right')"
          title="拖拽调整宽度"
        />
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, onActivated, onDeactivated } from 'vue'
import { usePlayerStore } from '@/store/player'
import { Rank, Check } from '@element-plus/icons-vue'
import { getSongLyric } from '@/api/lyric'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const playerStore = usePlayerStore()
const router = useRouter()

// 状态
const visible = ref(false)
const isMounted = ref(false)
const isLocked = ref(false)
const isDragging = ref(false)
const isResizing = ref(false)
const showControls = ref(false)
const showNextLine = ref(true)
const showSongInfo = ref(true)

// 位置和样式
const position = ref({ x: 0, y: 0 })
const opacity = ref(0.95)
const backgroundColor = ref('rgba(0, 0, 0, 0.75)')
const fontSize = ref(28)
const width = ref(600) // 默认宽度

// 颜色预设
const colorPresets = [
  'rgba(0, 0, 0, 0.75)',
  'rgba(102, 126, 234, 0.85)',
  'rgba(118, 75, 162, 0.85)',
  'rgba(245, 87, 108, 0.85)',
  'rgba(52, 152, 219, 0.85)',
  'rgba(46, 204, 113, 0.85)',
  'rgba(155, 89, 182, 0.85)',
  'rgba(52, 73, 94, 0.85)'
]

// 颜色名称映射
const getColorName = (color) => {
  const colorNames = {
    'rgba(0, 0, 0, 0.75)': '经典黑',
    'rgba(102, 126, 234, 0.85)': '紫罗兰',
    'rgba(118, 75, 162, 0.85)': '深紫',
    'rgba(245, 87, 108, 0.85)': '粉红',
    'rgba(52, 152, 219, 0.85)': '天蓝',
    'rgba(46, 204, 113, 0.85)': '翠绿',
    'rgba(155, 89, 182, 0.85)': '紫水晶',
    'rgba(52, 73, 94, 0.85)': '深灰'
  }
  return colorNames[color] || '自定义'
}

// 拖动相关
const dragOffset = ref({ x: 0, y: 0 })
let controlsTimeout = null

// 调整大小相关
let resizeDirection = ''
let resizeStartX = 0
let resizeStartWidth = 0
let resizeStartLeft = 0
const MIN_WIDTH = 300
const MAX_WIDTH = 1200

// 清理所有事件监听器
const cleanupEventListeners = () => {
  try {
    // 移除事件监听器
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
    
    // 重置拖拽和调整大小状态
    isDragging.value = false
    isResizing.value = false
    resizeDirection = ''
    
    console.log('事件监听器已清理')
  } catch (error) {
    console.error('清理事件监听器时出错:', error)
  }
}

// 当前歌曲和歌词
const currentSong = computed(() => playerStore.currentSong)
const currentLyric = ref('♪ 暂无歌词 ♪')
const nextLyric = ref('')

// 歌词数据
const mockLyrics = ref([])

// 暴露方法供外部调用
const show = () => {
  visible.value = true
  loadSettings()
}

const hide = () => {
  visible.value = false
}

const toggle = () => {
  visible.value = !visible.value
  if (visible.value) {
    loadSettings()
  }
}

const close = () => {
  visible.value = false
}

// 切换锁定状态
const toggleLock = () => {
  isLocked.value = !isLocked.value
  saveSettings()
}

// 拖动处理
const handleMouseDown = (e) => {
  if (!isMounted.value) return
  if (isLocked.value || isResizing.value) return
  if (e.target.closest('.control-buttons')) return
  if (e.target.closest('.resize-handle')) return
  
  // 阻止事件冒泡，避免影响页面其他元素
  e.stopPropagation()
  
  isDragging.value = true
  dragOffset.value = {
    x: e.clientX - position.value.x,
    y: e.clientY - position.value.y
  }
  
  document.addEventListener('mousemove', handleMouseMove, { passive: false })
  document.addEventListener('mouseup', handleMouseUp, { passive: false })
}

const handleMouseMove = (e) => {
  if (!isMounted.value) {
    cleanupEventListeners()
    return
  }
  
  // 阻止默认行为，避免文本选择等
  e.preventDefault()
  
  if (isDragging.value) {
    const newX = e.clientX - dragOffset.value.x
    const newY = e.clientY - dragOffset.value.y
    
    // 动态计算窗口大小限制
    const maxX = window.innerWidth - width.value
    const maxY = window.innerHeight - 150
    
    position.value = {
      x: Math.max(0, Math.min(newX, maxX)),
      y: Math.max(0, Math.min(newY, maxY))
    }
  } else if (isResizing.value) {
    handleResize(e)
  }
}

const handleMouseUp = () => {
  try {
    if (isDragging.value) {
      isDragging.value = false
      saveSettings()
    }
    if (isResizing.value) {
      isResizing.value = false
      resizeDirection = ''
      saveSettings()
    }
  } catch (error) {
    console.error('handleMouseUp error:', error)
  } finally {
    // 确保总是清理事件监听器
    cleanupEventListeners()
  }
}

// 调整大小处理
const startResize = (e, direction) => {
  if (!isMounted.value) return
  if (isLocked.value) return
  
  // 阻止事件冒泡
  e.stopPropagation()
  
  isResizing.value = true
  resizeDirection = direction
  resizeStartX = e.clientX
  resizeStartWidth = width.value
  resizeStartLeft = position.value.x
  
  document.addEventListener('mousemove', handleMouseMove, { passive: false })
  document.addEventListener('mouseup', handleMouseUp, { passive: false })
}

const handleResize = (e) => {
  if (!isResizing.value) return
  
  const deltaX = e.clientX - resizeStartX
  let newWidth = resizeStartWidth
  let newLeft = resizeStartLeft
  
  if (resizeDirection === 'right') {
    newWidth = resizeStartWidth + deltaX
  } else if (resizeDirection === 'left') {
    newWidth = resizeStartWidth - deltaX
    newLeft = resizeStartLeft + deltaX
  }
  
  // 限制宽度范围
  newWidth = Math.max(MIN_WIDTH, Math.min(newWidth, MAX_WIDTH))
  
  // 确保不超出屏幕
  if (resizeDirection === 'right') {
    const maxWidth = window.innerWidth - position.value.x - 20
    newWidth = Math.min(newWidth, maxWidth)
  } else if (resizeDirection === 'left') {
    const minLeft = 0
    const maxLeft = resizeStartLeft + resizeStartWidth - MIN_WIDTH
    newLeft = Math.max(minLeft, Math.min(newLeft, maxLeft))
    newWidth = resizeStartLeft + resizeStartWidth - newLeft
  }
  
  width.value = newWidth
  if (resizeDirection === 'left') {
    position.value.x = newLeft
  }
}

// 鼠标悬停显示控制栏
const handleMouseEnter = () => {
  showControls.value = true
  clearTimeout(controlsTimeout)
}

const handleMouseLeave = () => {
  if (!isLocked.value) {
    controlsTimeout = setTimeout(() => {
      showControls.value = false
    }, 2000)
  }
}

// 重置位置
const resetPosition = () => {
  width.value = 600
  position.value = {
    x: (window.innerWidth - width.value) / 2,
    y: 50
  }
  saveSettings()
}

// 改变背景色
const changeBackgroundColor = (color) => {
  backgroundColor.value = color
  saveSettings()
}

// 保存设置到 localStorage
const saveSettings = () => {
  const settings = {
    position: position.value,
    opacity: opacity.value,
    backgroundColor: backgroundColor.value,
    fontSize: fontSize.value,
    width: width.value,
    isLocked: isLocked.value,
    showNextLine: showNextLine.value,
    showSongInfo: showSongInfo.value
  }
  localStorage.setItem('desktopLyricSettings', JSON.stringify(settings))
}

// 加载设置
const loadSettings = () => {
  const saved = localStorage.getItem('desktopLyricSettings')
  if (saved) {
    try {
      const settings = JSON.parse(saved)
      width.value = settings.width || 600
      position.value = settings.position || { x: (window.innerWidth - width.value) / 2, y: 50 }
      opacity.value = settings.opacity || 0.95
      backgroundColor.value = settings.backgroundColor || 'rgba(0, 0, 0, 0.75)'
      fontSize.value = settings.fontSize || 28
      isLocked.value = settings.isLocked || false
      showNextLine.value = settings.showNextLine ?? true
      showSongInfo.value = settings.showSongInfo ?? true
    } catch (e) {
      resetPosition()
    }
  } else {
    resetPosition()
  }
}

// 监听播放状态
const isPlaying = computed(() => playerStore.isPlaying)

// 监听当前播放时间，更新歌词
watch(() => playerStore.currentTime, (time) => {
  if (mockLyrics.value.length > 0 && isPlaying.value) {
    updateLyric(time)
  }
}, { immediate: true })

// 监听歌曲变化，加载新歌词
watch(() => currentSong.value?.id, (newId) => {
  if (newId) {
    loadLyrics(newId)
  } else {
    // 没有歌曲时显示默认提示
    currentLyric.value = '♪ 暂无播放 ♪'
    nextLyric.value = ''
    mockLyrics.value = []
  }
}, { immediate: true })

// 加载歌词（函数声明，避免初始化前访问）
async function loadLyrics(songId) {
  console.log('加载歌词，歌曲ID:', songId)
  
  try {
    // 从后端API获取歌词
    const response = await getSongLyric(songId)
    
    if (response.code === 200) {
      const lrcContent = response.data
      
      if (!lrcContent || lrcContent.trim() === '') {
        // 没有歌词，显示纯音乐
        console.log('该歌曲暂无歌词')
        currentLyric.value = '♪ 纯音乐，请欣赏 ♪'
        nextLyric.value = ''
        mockLyrics.value = []
        return
      }
      
      // 解析 LRC 格式歌词
      mockLyrics.value = parseLrc(lrcContent)
      console.log('歌词解析完成，共', mockLyrics.value.length, '行')
      
      if (mockLyrics.value.length === 0) {
        currentLyric.value = '♪ 歌词解析失败 ♪'
        nextLyric.value = ''
      } else {
        // 立即显示第一句歌词
        updateLyric(playerStore.currentTime || 0)
      }
    } else {
      console.error('获取歌词失败:', response.message)
      // 如果 API 获取失败，使用模拟数据作为后备
      useFallbackLyrics(songId)
    }
  } catch (error) {
    console.error('加载歌词出错:', error)
    // 出错时使用模拟数据
    useFallbackLyrics(songId)
  }
}

// 使用后备模拟歌词
const useFallbackLyrics = (songId) => {
  console.log('使用模拟歌词数据')
  const lyrics = generateMockLyrics(songId)
  mockLyrics.value = lyrics
  
  if (mockLyrics.value.length > 0) {
    updateLyric(playerStore.currentTime || 0)
  } else {
    currentLyric.value = '♪ 暂无歌词 ♪'
    nextLyric.value = ''
  }
}

// 解析 LRC 格式歌词（函数声明，避免初始化前访问）
function parseLrc(lrcText) {
  const lines = lrcText.split('\n')
  const lyrics = []
  
  // LRC 格式示例：[00:12.00]歌词内容 或 [00:12.000]歌词内容
  const timeRegex = /\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)/
  
  lines.forEach(line => {
    const match = line.match(timeRegex)
    if (match) {
      const minutes = parseInt(match[1])
      const seconds = parseInt(match[2])
      const milliseconds = parseInt(match[3].padEnd(3, '0')) // 确保是3位数
      const text = match[4].trim()
      
      // 计算总秒数
      const time = minutes * 60 + seconds + milliseconds / 1000
      
      // 只添加有内容的歌词
      if (text && text.length > 0) {
        lyrics.push({ time, text })
      }
    }
  })
  
  // 按时间排序
  lyrics.sort((a, b) => a.time - b.time)
  
  console.log('解析到', lyrics.length, '行歌词')
  if (lyrics.length > 0) {
    console.log('第一句:', lyrics[0])
    console.log('最后一句:', lyrics[lyrics.length - 1])
  }
  
  return lyrics
}

// 生成模拟歌词数据
const generateMockLyrics = (songId) => {
  // 为不同歌曲生成不同的模拟歌词
  const lyricTemplates = [
    [
      { time: 0, text: '♪ 前奏 ♪' },
      { time: 5, text: '在这美好的时光里' },
      { time: 10, text: '让音乐带走所有烦恼' },
      { time: 15, text: '跟着节奏轻轻摇摆' },
      { time: 20, text: '感受每一个音符的跳动' },
      { time: 25, text: '♪ 间奏 ♪' },
      { time: 30, text: '生活就像一首歌' },
      { time: 35, text: '有高潮也有低谷' },
      { time: 40, text: '但音乐永远陪伴着你' },
      { time: 45, text: '♪ 尾奏 ♪' }
    ],
    [
      { time: 0, text: '♪ 音乐响起 ♪' },
      { time: 8, text: '夜空中最亮的星' },
      { time: 13, text: '能否听清' },
      { time: 18, text: '那仰望的人' },
      { time: 23, text: '心底的孤独和叹息' },
      { time: 28, text: '♪ 继续 ♪' }
    ],
    [
      { time: 0, text: '♪ 开始 ♪' },
      { time: 6, text: '我们都是追梦人' },
      { time: 12, text: '在这漫长的旅途中' },
      { time: 18, text: '音乐是最好的伙伴' },
      { time: 24, text: '让我们继续前行' },
      { time: 30, text: '♪ 前进 ♪' }
    ]
  ]
  
  // 根据歌曲ID选择不同的歌词模板
  const index = (songId || 0) % lyricTemplates.length
  return lyricTemplates[index]
}

// 更新当前歌词（函数声明，避免初始化前访问）
function updateLyric(currentTime) {
  if (!mockLyrics.value || mockLyrics.value.length === 0) {
    currentLyric.value = '♪ 暂无歌词 ♪'
    nextLyric.value = ''
    return
  }
  
  let currentIndex = -1
  
  // 找到当前时间对应的歌词索引
  for (let i = mockLyrics.value.length - 1; i >= 0; i--) {
    if (currentTime >= mockLyrics.value[i].time) {
      currentIndex = i
      break
    }
  }
  
  if (currentIndex >= 0) {
    const newLyric = mockLyrics.value[currentIndex].text
    
    // 只在歌词改变时更新，避免不必要的重渲染
    if (currentLyric.value !== newLyric) {
      currentLyric.value = newLyric
      console.log(`[${Math.floor(currentTime)}s] 当前歌词:`, newLyric)
    }
    
    // 更新下一句歌词
    if (currentIndex + 1 < mockLyrics.value.length) {
      nextLyric.value = mockLyrics.value[currentIndex + 1].text
    } else {
      nextLyric.value = ''
    }
  } else {
    // 在第一句歌词之前
    currentLyric.value = '♪ 前奏 ♪'
    if (mockLyrics.value.length > 0) {
      nextLyric.value = mockLyrics.value[0].text
    }
  }
}

// 窗口大小改变时调整位置
const handleWindowResize = () => {
  const maxX = window.innerWidth - width.value
  const maxY = window.innerHeight - 150
  
  if (position.value.x > maxX) position.value.x = Math.max(0, maxX)
  if (position.value.y > maxY) position.value.y = Math.max(0, maxY)
  
  // 确保宽度不超出屏幕
  if (width.value > window.innerWidth - 40) {
    width.value = Math.min(window.innerWidth - 40, MAX_WIDTH)
  }
  
  // 确保窗口在可见范围内
  if (position.value.x < 0) position.value.x = 0
  if (position.value.y < 0) position.value.y = 0
}

onMounted(() => {
  console.log('桌面歌词组件开始挂载')
  
  // 延迟设置 isMounted 以确保 DOM 已准备好
  setTimeout(() => {
    isMounted.value = true
    console.log('桌面歌词组件已挂载，isMounted设置为true')
  }, 0)
  
  window.addEventListener('resize', handleWindowResize)
  loadSettings()
  
  // 如果正在播放，立即加载歌词
  if (currentSong.value?.id) {
    console.log('组件挂载，当前歌曲:', currentSong.value.title)
    loadLyrics(currentSong.value.id)
  } else {
    console.log('组件挂载，暂无播放歌曲')
  }
  
  // 添加调试信息
  console.log('播放器状态:', {
    playing: playerStore.isPlaying,
    currentTime: playerStore.currentTime,
    song: currentSong.value?.title
  })
})

onBeforeUnmount(() => {
  console.log('桌面歌词组件开始卸载')
  isMounted.value = false
  visible.value = false
  
  window.removeEventListener('resize', handleWindowResize)
  cleanupEventListeners()
  
  // 清理超时
  if (controlsTimeout) {
    clearTimeout(controlsTimeout)
    controlsTimeout = null
  }
  
  console.log('桌面歌词组件卸载完成')
})

// 监听路由变化，清理事件监听器
watch(() => router.currentRoute.value.path, (newPath, oldPath) => {
  console.log('路由切换:', oldPath, '->', newPath)
  console.log('清理桌面歌词事件监听器')
  
  // 立即清理所有事件监听器
  cleanupEventListeners()
  
  // 重置控制显示状态
  showControls.value = false
}, { immediate: false })

// 导出方法
defineExpose({
  show,
  hide,
  toggle,
  visible
})
</script>

<style scoped>
.desktop-lyric {
  position: fixed;
  z-index: 9998;
  padding: 16px 24px;
  border-radius: 12px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  transition: opacity 0.3s ease, background-color 0.3s ease;
  cursor: move;
  user-select: none;
  overflow: visible;
  box-sizing: border-box;
}

.desktop-lyric.locked {
  cursor: default;
}

.desktop-lyric.dragging {
  cursor: grabbing;
  transition: none;
}

.desktop-lyric.resizing {
  cursor: ew-resize !important;
  transition: none !important;
}

/* 调整大小手柄 */
.resize-handle {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 10px;
  cursor: ew-resize;
  z-index: 10;
  transition: background-color 0.2s;
}

.resize-handle:hover {
  background-color: rgba(102, 126, 234, 0.3);
}

.resize-handle:active {
  background-color: rgba(102, 126, 234, 0.5);
}

.resize-left {
  left: 0;
  border-top-left-radius: 12px;
  border-bottom-left-radius: 12px;
}

.resize-right {
  right: 0;
  border-top-right-radius: 12px;
  border-bottom-right-radius: 12px;
}

/* 控制栏 */
.lyric-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.drag-handle {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  cursor: move;
}

.desktop-lyric.locked .drag-handle {
  cursor: default;
}

.control-buttons {
  display: flex;
  gap: 8px;
}

.control-buttons :deep(.el-button) {
  background-color: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  color: white;
}

.control-buttons :deep(.el-button:hover) {
  background-color: rgba(255, 255, 255, 0.3);
}

/* 歌词内容 */
.lyric-content {
  text-align: center;
  padding: 12px 0;
  width: 100%;
}

.lyric-line {
  color: white;
  font-weight: 600;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
  line-height: 1.6;
  margin-bottom: 8px;
  word-wrap: break-word;
  word-break: break-word;
  white-space: pre-wrap;
  overflow-wrap: break-word;
  max-width: 100%;
  padding: 0 20px;
  hyphens: auto;
}

.lyric-next {
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.75em;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.5);
  line-height: 1.6;
  margin-top: 8px;
  word-wrap: break-word;
  word-break: break-word;
  white-space: pre-wrap;
  overflow-wrap: break-word;
  max-width: 100%;
  padding: 0 20px;
  hyphens: auto;
}

/* 歌曲信息 */
.song-info {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.song-title {
  font-weight: 500;
}

.song-artist {
  opacity: 0.7;
}

/* 弹出框控制 */
.opacity-control,
.font-control {
  padding: 16px;
}

.color-control {
  padding: 16px;
}

.control-title,
.color-control-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.control-value {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  text-align: right;
}

.color-presets {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.color-item {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
  border: 3px solid transparent;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.color-item:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
}

.color-item.active {
  border-color: white;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.color-item .check-icon {
  color: white;
  font-size: 24px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.5));
}

/* 动画 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from {
  opacity: 0;
  transform: translateY(-20px);
}

.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

.lyric-fade-enter-active,
.lyric-fade-leave-active {
  transition: all 0.4s ease;
}

.lyric-fade-enter-from {
  opacity: 0;
  transform: translateY(10px) scale(0.95);
}

.lyric-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.95);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .desktop-lyric {
    min-width: 90vw;
    max-width: 95vw;
    left: 2.5vw !important;
    padding: 12px 16px;
  }
  
  .lyric-line {
    font-size: 18px !important;
    padding: 0 12px;
  }
  
  .lyric-next {
    padding: 0 12px;
  }
  
  .lyric-controls {
    flex-wrap: wrap;
  }
  
  .color-item {
    width: 40px;
    height: 40px;
  }
  
  .color-presets {
    gap: 8px;
  }
}

@media (max-width: 480px) {
  .desktop-lyric {
    min-width: 95vw;
    padding: 10px 12px;
  }
  
  .lyric-line {
    font-size: 16px !important;
    padding: 0 8px;
  }
  
  .lyric-next {
    padding: 0 8px;
  }
}
</style>

<style>
/* 全局样式 - 优化弹出框外观 */
.lyric-color-popover.el-popper,
.lyric-control-popover.el-popper {
  background: var(--card-bg) !important;
  border: 1px solid var(--border-color) !important;
  box-shadow: var(--shadow-lg) !important;
  border-radius: 12px !important;
  z-index: 10000 !important; /* 确保弹出框在桌面歌词之上 */
}

.lyric-color-popover .el-popper__arrow::before,
.lyric-control-popover .el-popper__arrow::before {
  background: var(--card-bg) !important;
  border: 1px solid var(--border-color) !important;
}

/* 优化滑块样式 */
.lyric-control-popover .el-slider__runway {
  background-color: var(--bg-tertiary) !important;
}

.lyric-control-popover .el-slider__bar {
  background-color: var(--color-primary) !important;
}

.lyric-control-popover .el-slider__button {
  border-color: var(--color-primary) !important;
  background-color: white !important;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.3);
}

.lyric-control-popover .el-slider__button:hover {
  transform: scale(1.2);
}
</style>

