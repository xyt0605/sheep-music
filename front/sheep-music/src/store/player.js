import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { playSong as playSongAPI } from '@/api/song'
import { addPlayHistory } from '@/api/playHistory'
import { useUserStore } from '@/store/user'

export const usePlayerStore = defineStore('player', () => {
  // 状态
  const currentSong = ref(null) // 当前歌曲
  const playlist = ref([]) // 播放列表
  const currentIndex = ref(-1) // 当前播放索引
  const isPlaying = ref(false) // 是否正在播放
  const currentTime = ref(0) // 当前播放时间
  const duration = ref(0) // 歌曲总时长
  const volume = ref(0.7) // 音量 0-1
  const showPlayer = ref(false) // 是否显示播放器
  const showLyric = ref(false) // 是否显示歌词
  const playMode = ref('list') // 播放模式：list-列表循环, random-随机播放, single-单曲循环
  
  // Audio 元素
  const audio = ref(null)
  
  // 计算属性
  const hasNextSong = computed(() => {
    return currentIndex.value < playlist.value.length - 1
  })
  
  const hasPrevSong = computed(() => {
    return currentIndex.value > 0
  })
  
  // 初始化音频元素
  const initAudio = (audioElement) => {
    audio.value = audioElement
    
    // 优化加载策略，减少被中断的请求
    audioElement.preload = 'metadata'
    audioElement.crossOrigin = 'anonymous'

    audioElement.addEventListener('error', () => {
      try {
        console.error('音频资源加载失败', audioElement?.error?.code, audioElement?.src)
      } catch (e) {}
    })

    // 监听时间更新
    audioElement.addEventListener('timeupdate', () => {
      currentTime.value = audioElement.currentTime
    })
    
    // 监听加载完成
    audioElement.addEventListener('loadedmetadata', () => {
      duration.value = audioElement.duration
    })
    
    // 监听播放结束
    audioElement.addEventListener('ended', () => {
      if (playMode.value === 'single') {
        // 单曲循环：重新播放当前歌曲（调用play函数以触发播放次数统计）
        if (currentSong.value) {
          play(currentSong.value, null, true) // 第三个参数表示是单曲循环重播
        }
      } else {
        // 列表循环或随机播放：播放下一曲
        next()
      }
    })
    
    // 设置音量
    audioElement.volume = volume.value
  }
  
  // 播放歌曲
  const play = async (song, list = null, isSingleLoopReplay = false) => {
    try {
      // 如果提供了播放列表，更新播放列表
      if (list && list.length > 0) {
        playlist.value = list
        const index = list.findIndex(s => s.id === song.id)
        currentIndex.value = index >= 0 ? index : 0
      } else if (currentSong.value?.id !== song.id) {
        // 如果是新歌曲且没有提供列表，添加到当前播放列表
        const existIndex = playlist.value.findIndex(s => s.id === song.id)
        if (existIndex >= 0) {
          currentIndex.value = existIndex
        } else {
          playlist.value.push(song)
          currentIndex.value = playlist.value.length - 1
        }
      }
      
      currentSong.value = song
      showPlayer.value = true
      
      // 播放音频
      if (audio.value && song.url) {
        // 单曲循环重播时，不需要重新设置src，只需从头播放
        if (isSingleLoopReplay && audio.value.src.includes(song.url)) {
          audio.value.currentTime = 0
        } else {
          // 切换前暂停，避免并发拉取造成请求被中断
          try { audio.value.pause() } catch (e) {}
          
          // 处理 OSS URL，使用服务器代理避免跨域
          let src = song.url
          const ossHost = 'https://sheepmusic.oss-cn-hangzhou.aliyuncs.com'
          if (src && src.startsWith(ossHost)) {
            // 提取 OSS URL 的路径部分
            const ossPath = src.substring(ossHost.length)
            // 确保路径以 / 开头
            src = '/api/oss' + (ossPath.startsWith('/') ? ossPath : '/' + ossPath)
          }
          
          audio.value.src = src
          // 显式触发加载，降低立即 play 导致的中断
          try { audio.value.load() } catch (e) {}
        }
        
        // 等待可以播放再开始
        try {
          await audio.value.play()
        } catch (e) {
          // 某些浏览器需要用户交互后才能自动播放
          console.warn('音频播放受限或中断，将在用户交互后恢复：', e?.message || e)
        }
        isPlaying.value = true
        
        // 调用后端 API 增加播放次数
        try {
          await playSongAPI(song.id)
        } catch (error) {
          console.error('更新播放次数失败:', error)
        }
        
        // 添加播放历史记录（仅登录用户）
        try {
          const userStore = useUserStore()
          if (userStore.isLogin) {
            await addPlayHistory({ songId: song.id })
          }
        } catch (error) {
          console.error('添加播放历史失败:', error)
        }
      }
    } catch (error) {
      console.error('播放失败:', error)
      isPlaying.value = false
    }
  }
  
  // 暂停
  const pause = () => {
    if (audio.value) {
      audio.value.pause()
      isPlaying.value = false
    }
  }
  
  // 继续播放
  const resume = () => {
    if (audio.value) {
      audio.value.play()
      isPlaying.value = true
    }
  }
  
  // 切换播放/暂停
  const togglePlay = () => {
    if (isPlaying.value) {
      pause()
    } else {
      resume()
    }
  }
  
  // 获取下一首歌曲的索引
  const getNextIndex = () => {
    if (playlist.value.length === 0) return -1
    
    if (playMode.value === 'random') {
      // 随机播放：随机选择一首（避免连续播放同一首）
      if (playlist.value.length === 1) return 0
      
      let nextIndex
      do {
        nextIndex = Math.floor(Math.random() * playlist.value.length)
      } while (nextIndex === currentIndex.value)
      
      return nextIndex
    } else {
      // 列表循环或单曲循环：顺序播放
      if (currentIndex.value < playlist.value.length - 1) {
        return currentIndex.value + 1
      } else {
        return 0 // 循环到第一首
      }
    }
  }
  
  // 下一曲
  const next = () => {
    const nextIndex = getNextIndex()
    if (nextIndex >= 0) {
      currentIndex.value = nextIndex
      play(playlist.value[nextIndex])
    }
  }
  
  // 上一曲
  const prev = () => {
    if (hasPrevSong.value) {
      currentIndex.value--
      play(playlist.value[currentIndex.value])
    }
  }
  
  // 跳转到指定时间
  const seek = (time) => {
    if (audio.value) {
      audio.value.currentTime = time
      currentTime.value = time
    }
  }
  
  // 设置音量
  const setVolume = (vol) => {
    volume.value = vol
    if (audio.value) {
      audio.value.volume = vol
    }
  }
  
  // 添加到播放列表
  const addToPlaylist = (song) => {
    const exists = playlist.value.some(s => s.id === song.id)
    if (!exists) {
      playlist.value.push(song)
    }
  }
  
  // 从播放列表移除
  const removeFromPlaylist = (songId) => {
    const index = playlist.value.findIndex(s => s.id === songId)
    if (index >= 0) {
      playlist.value.splice(index, 1)
      if (index < currentIndex.value) {
        currentIndex.value--
      }
    }
  }
  
  // 切换歌词显示
  const toggleLyric = () => {
    showLyric.value = !showLyric.value
  }
  
  // 设置播放模式
  const setPlayMode = (mode) => {
    playMode.value = mode
  }
  
  // 切换播放模式
  const togglePlayMode = () => {
    const modes = ['list', 'random', 'single']
    const currentModeIndex = modes.indexOf(playMode.value)
    const nextModeIndex = (currentModeIndex + 1) % modes.length
    playMode.value = modes[nextModeIndex]
  }
  
  return {
    // 状态
    currentSong,
    playlist,
    currentIndex,
    isPlaying,
    currentTime,
    duration,
    volume,
    showPlayer,
    showLyric,
    playMode,
    
    // 计算属性
    hasNextSong,
    hasPrevSong,
    
    // 方法
    initAudio,
    play,
    pause,
    resume,
    togglePlay,
    next,
    prev,
    seek,
    setVolume,
    addToPlaylist,
    removeFromPlaylist,
    toggleLyric,
    setPlayMode,
    togglePlayMode
  }
})
