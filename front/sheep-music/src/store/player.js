import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { playSong as playSongAPI } from '@/api/song'
import { addPlayHistory } from '@/api/playHistory'
import { getExternalLyric, getExternalCover } from '@/api/externalMusic'
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
  const showDesktopLyric = ref(false) // 桌面歌词显隐（Layout 按钮 / 全屏歌词 / DesktopLyric 组件共享）
  const playMode = ref('list') // 播放模式：list-列表循环, random-随机播放, single-单曲循环
  
  // Audio 元素
  const audio = ref(null)

  // 播放世代号：每次进入 play() 递增，用于丢弃被后续切歌抢占的过期异步结果，
  // 避免重叠的 play() 互相覆盖 isPlaying 状态（快速连续切歌 / 自动续播竞态）
  let playToken = 0
  // 自动续播被浏览器 autoplay 策略拒绝（无用户手势）时暂存的待恢复歌曲；
  // 注册一次性全局手势监听，用户下次任意交互即自动续上，无需去找播放键
  let pendingResumeSong = null
  let gestureListenerBound = false

  // 在用户下一次任意交互时，自动恢复被 autoplay 策略拦下的播放
  const bindResumeOnGesture = () => {
    if (gestureListenerBound) return
    gestureListenerBound = true
    const handler = () => {
      gestureListenerBound = false
      window.removeEventListener('pointerdown', handler)
      window.removeEventListener('keydown', handler)
      window.removeEventListener('touchstart', handler)
      const song = pendingResumeSong
      pendingResumeSong = null
      // 仅当待恢复歌曲仍是当前歌、且确实处于暂停态时才自动续播，
      // 避免用户在此期间已手动切歌/播放造成错乱
      if (song && currentSong.value?.id === song.id && !isPlaying.value) {
        resume()
      }
    }
    window.addEventListener('pointerdown', handler, { once: true })
    window.addEventListener('keydown', handler, { once: true })
    window.addEventListener('touchstart', handler, { once: true })
  }

  // 计算属性
  // 手动切歌与自动切歌（getNextIndex）保持一致：列表/单曲模式下可回绕，
  // 随机模式只要多于一首即可切
  const canSwitchSong = computed(() => {
    if (playlist.value.length === 0) return false
    if (playMode.value === 'random') return playlist.value.length > 1
    return true
  })

  const hasNextSong = canSwitchSong

  const hasPrevSong = canSwitchSong
  
  // 初始化音频元素
  const initAudio = (audioElement) => {
    // 幂等：同一元素重复初始化时直接跳过，避免重复挂 'ended' 等监听
    // 导致自然播放结束触发多次 next()（dev HMR / 组件重挂场景）
    if (audio.value === audioElement) return
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
  
  // 自动跳到队列中下一首「有 url、可播放」的歌见 autoSkipToPlayable（定义在 next 之后）

  // 播放歌曲
  const play = async (song, list = null, isSingleLoopReplay = false) => {
    try {
      // 如果提供了播放列表，更新播放列表
      if (list && list.length > 0) {
        const index = list.findIndex(s => s.id === song.id)
        if (index >= 0) {
          playlist.value = list
          currentIndex.value = index
        } else {
          // 歌曲不在列表中：插入到队首，保证 currentIndex 与 currentSong 指向一致
          playlist.value = [song, ...list]
          currentIndex.value = 0
        }
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

      // 本次播放的世代号；被后续切歌抢占（token 过期）时，丢弃本次的异步收尾，
      // 避免过期的 play() 结果覆盖新歌状态
      const token = ++playToken

      // 无有效 url（如 DJ 混合队列里 url 丢失的本地卡）：不静默停在旧歌，
      // 显式置暂停并尝试自动跳到下一首可播的歌
      if (audio.value && !song.url) {
        console.warn('歌曲缺少可播放 url，跳过：', song.title)
        isPlaying.value = false
        autoSkipToPlayable(token)
        return
      }

      // 播放音频
      if (audio.value && song.url) {
        // 处理 OSS URL，使用服务器代理避免跨域
        let src = song.url
        const ossHost = 'https://sheepmusic.oss-cn-hangzhou.aliyuncs.com'
        if (src && src.startsWith(ossHost)) {
          // 提取 OSS URL 的路径部分
          const ossPath = src.substring(ossHost.length)
          // 确保路径以 / 开头
          src = '/api/oss' + (ossPath.startsWith('/') ? ossPath : '/' + ossPath)
        }

        // 已加载着同一首歌时（单曲循环重播、或重复点当前歌），无需重设 src，只从头播放。
        // 注意：src 已被改写为 /api/oss 代理路径，故与改写后的 src 比较
        //（旧实现用 song.url 原始 oss 域名比较，对 OSS 歌恒不相等 → 单曲循环也全量重载，已修）
        const alreadyLoaded = !!audio.value.src && audio.value.src.endsWith(src)
        if (alreadyLoaded) {
          audio.value.currentTime = 0
        } else {
          // 切换前暂停，避免并发拉取造成请求被中断
          try { audio.value.pause() } catch (e) {}
          // 设置 src 本身即触发浏览器的媒体加载算法；不要再显式调用 load()，
          // 否则会以 AbortError("interrupted by a new load request") 打断随后的 play()
          audio.value.src = src
        }

        // 等待可以播放再开始；只有真正开始播放才置为播放中状态，
        // 避免资源加载失败时 UI 卡在"播放中"且进度条冻结
        try {
          await audio.value.play()
          if (token !== playToken) return // 已被后续切歌抢占，丢弃过期结果
          isPlaying.value = true
          pendingResumeSong = null
        } catch (e) {
          if (token !== playToken) return // 已被后续切歌抢占，丢弃过期结果
          const name = e?.name || ''
          console.warn('音频播放受限或中断：', name, e?.message || e)

          if (name === 'NotAllowedError') {
            // 自动播放被浏览器策略拦截（自动续播 / agent 点歌等无用户手势场景）：
            // 挂起待恢复，用户下次任意交互时自动续播，无需去找播放键
            isPlaying.value = false
            pendingResumeSong = song
            bindResumeOnGesture()
            try {
              const { ElMessage } = await import('element-plus')
              ElMessage.info(`点一下页面任意处就能继续听《${song.title}》`)
            } catch (_) {}
            return
          }

          if (name === 'AbortError') {
            // 加载被新的请求打断（快速连续切歌）：自动重试一次，不重设 src
            try {
              await audio.value.play()
              if (token !== playToken) return
              isPlaying.value = true
              pendingResumeSong = null
            } catch (e2) {
              if (token !== playToken) return
              isPlaying.value = false
              if (e2?.name === 'NotAllowedError') {
                pendingResumeSong = song
                bindResumeOnGesture()
              }
            }
            return
          }

          // 其它（资源加载失败 / 网络错误等）：重设 src 后重试一次
          try {
            audio.value.src = src
            await audio.value.play()
            if (token !== playToken) return
            isPlaying.value = true
            pendingResumeSong = null
          } catch (e2) {
            if (token !== playToken) return
            console.error('音频重试仍失败：', e2?.name, e2?.message || e2)
            isPlaying.value = false
            try {
              const { ElMessage } = await import('element-plus')
              ElMessage.error(`《${song.title}》暂时无法播放，可能是音源失效`)
            } catch (_) {}
          }
          return
        }

        // 外源歌曲（开放曲库试听）无本地 songId，不产生任何后端写操作（曲库供应链 v1）
        if (!song.isExternal) {
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
        } else if (song.lyric === '' && song.source && song.sourceTrackId) {
          // 外源歌词异步加载（gequhai 等来源播放页带 LRC）：加载完成写入歌曲对象，歌词面板响应式刷新
          getExternalLyric({ source: song.source, trackId: song.sourceTrackId })
            .then(res => { song.lyric = res.code === 200 ? (res.data || '') : '' })
            .catch(() => { song.lyric = '' })
          // 搜索列表可能尚未回填封面：播放时兜底加载（加载完成播放器/歌词封面同步刷新）
          if (!song.cover) {
            getExternalCover({ source: song.source, trackId: song.sourceTrackId })
              .then(res => { if (res.code === 200 && res.data) song.cover = res.data })
              .catch(() => {})
          }
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
    if (!audio.value) return
    Promise.resolve(audio.value.play())
      .then(() => { isPlaying.value = true; pendingResumeSong = null })
      .catch((e) => {
        console.warn('恢复播放失败：', e?.message || e)
        isPlaying.value = false
      })
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

  // 当前歌缺少可播放 url 时，自动向后寻找下一首可播的歌（最多遍历整个列表一圈，避免死循环）
  const autoSkipToPlayable = (token) => {
    if (token !== playToken) return // 已被后续操作抢占
    const list = playlist.value
    if (list.length <= 1) return
    for (let step = 0; step < list.length; step++) {
      const idx = getNextIndex()
      if (idx < 0 || idx === currentIndex.value) break
      currentIndex.value = idx
      const candidate = list[idx]
      if (candidate && candidate.url) {
        play(candidate)
        return
      }
    }
  }
  
  // 上一曲（与自动切歌一致：列表/单曲模式回绕，随机模式随机换一首）
  const prev = () => {
    if (!hasPrevSong.value) return
    if (playMode.value === 'random') {
      currentIndex.value = getNextIndex()
    } else {
      currentIndex.value = currentIndex.value > 0
        ? currentIndex.value - 1
        : playlist.value.length - 1
    }
    play(playlist.value[currentIndex.value])
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
    if (index < 0) return
    const isCurrent = index === currentIndex.value
    playlist.value.splice(index, 1)

    if (index < currentIndex.value) {
      // 移除的是当前歌曲之前的歌：索引前移一位
      currentIndex.value--
    } else if (isCurrent) {
      // 移除的正是当前播放的歌：同步切换/清理，避免索引越界或高亮错位
      if (playlist.value.length === 0) {
        pause()
        currentSong.value = null
        currentIndex.value = -1
        return
      }
      if (currentIndex.value >= playlist.value.length) {
        currentIndex.value = playlist.value.length - 1
      }
      play(playlist.value[currentIndex.value])
    }
  }
  
  // 切换歌词显示
  const toggleLyric = () => {
    showLyric.value = !showLyric.value
  }

  // 切换桌面歌词显示
  const toggleDesktopLyric = () => {
    showDesktopLyric.value = !showDesktopLyric.value
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
  
  // 清空播放器（退出登录时使用）
  const clearPlayer = () => {
    // 停止播放
    if (audio.value) {
      audio.value.pause()
      audio.value.src = ''
    }
    
    // 重置所有状态
    currentSong.value = null
    playlist.value = []
    currentIndex.value = -1
    isPlaying.value = false
    currentTime.value = 0
    duration.value = 0
    showPlayer.value = false
    showLyric.value = false
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
    showDesktopLyric,
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
    toggleDesktopLyric,
    setPlayMode,
    togglePlayMode,
    clearPlayer
  }
})
