# 🎤 桌面歌词同步问题修复说明

## 🔧 已修复的问题

### 1. **监听播放状态**
```javascript
// 之前：只监听时间，不管是否在播放
watch(() => playerStore.currentTime, (time) => {
  updateLyric(time)
})

// 现在：只在播放时更新歌词
const isPlaying = computed(() => playerStore.isPlaying)
watch(() => playerStore.currentTime, (time) => {
  if (mockLyrics.value.length > 0 && isPlaying.value) {
    updateLyric(time)
  }
}, { immediate: true })
```

### 2. **立即执行 Watch**
添加了 `{ immediate: true }` 参数，确保组件挂载时立即同步歌词

### 3. **改进歌词更新逻辑**
```javascript
const updateLyric = (currentTime) => {
  // 添加空值检查
  if (!mockLyrics.value || mockLyrics.value.length === 0) {
    currentLyric.value = '♪ 暂无歌词 ♪'
    return
  }
  
  // 找到对应时间的歌词
  let currentIndex = -1
  for (let i = mockLyrics.value.length - 1; i >= 0; i--) {
    if (currentTime >= mockLyrics.value[i].time) {
      currentIndex = i
      break
    }
  }
  
  // 只在歌词真正改变时更新（避免重复渲染）
  if (currentIndex >= 0) {
    const newLyric = mockLyrics.value[currentIndex].text
    if (currentLyric.value !== newLyric) {
      currentLyric.value = newLyric
      console.log(`[${Math.floor(currentTime)}s] 当前歌词:`, newLyric)
    }
  }
}
```

### 4. **添加调试日志**
控制台会显示：
- 组件挂载信息
- 播放器状态
- 歌词加载状态
- 歌词切换时间点

### 5. **多套模拟歌词**
根据歌曲 ID 生成不同的歌词，方便测试：
- 模板 1：通用励志歌词
- 模板 2：《夜空中最亮的星》
- 模板 3：《追梦人》

---

## 🧪 如何验证歌词同步

### 步骤 1：打开浏览器控制台
按 `F12` 或右键 → 检查，查看 Console 面板

### 步骤 2：播放音乐
1. 播放任意歌曲
2. 点击导航栏的桌面歌词按钮

### 步骤 3：查看日志输出
控制台应该显示：
```
桌面歌词组件已挂载
播放器状态: { playing: true, currentTime: 0, song: "歌曲名" }
加载歌词，歌曲ID: xxx
[0s] 当前歌词: ♪ 前奏 ♪
[5s] 当前歌词: 在这美好的时光里
[10s] 当前歌词: 让音乐带走所有烦恼
...
```

### 步骤 4：验证歌词时间轴
模拟歌词的时间轴如下：

**模板 1（通用）：**
- 0s: ♪ 前奏 ♪
- 5s: 在这美好的时光里
- 10s: 让音乐带走所有烦恼
- 15s: 跟着节奏轻轻摇摆
- 20s: 感受每一个音符的跳动
- 25s: ♪ 间奏 ♪
- 30s: 生活就像一首歌
- 35s: 有高潮也有低谷
- 40s: 但音乐永远陪伴着你
- 45s: ♪ 尾奏 ♪

**模板 2（夜空中最亮的星）：**
- 0s: ♪ 音乐响起 ♪
- 8s: 夜空中最亮的星
- 13s: 能否听清
- 18s: 那仰望的人
- 23s: 心底的孤独和叹息
- 28s: ♪ 继续 ♪

**模板 3（追梦人）：**
- 0s: ♪ 开始 ♪
- 6s: 我们都是追梦人
- 12s: 在这漫长的旅途中
- 18s: 音乐是最好的伙伴
- 24s: 让我们继续前行
- 30s: ♪ 前进 ♪

---

## 🔍 常见问题排查

### 问题 1：歌词不更新
**原因：** 可能是播放器状态未正确获取
**解决：**
```javascript
// 检查控制台输出
console.log('当前播放时间:', playerStore.currentTime)
console.log('是否正在播放:', playerStore.isPlaying)
console.log('歌词数据:', mockLyrics.value)
```

### 问题 2：歌词延迟
**原因：** 模拟歌词的时间可能与实际播放不符
**解决：**
- 调整 `generateMockLyrics` 中的 `time` 值
- 实际项目应从后端获取真实的 LRC 歌词

### 问题 3：歌词跳跃
**原因：** 播放器的 `currentTime` 可能有跳跃
**解决：**
- 检查音频文件是否正常
- 查看网络加载情况

### 问题 4：歌词不切换
**原因：** Watch 可能未触发
**解决：**
```javascript
// 在 updateLyric 函数开头添加日志
console.log('updateLyric 被调用, 当前时间:', currentTime)
```

---

## 📝 集成真实歌词 API

当后端提供歌词 API 后，修改 `loadLyrics` 函数：

```javascript
const loadLyrics = async (songId) => {
  console.log('加载歌词，歌曲ID:', songId)
  
  try {
    // 1. 从后端获取歌词
    const response = await fetch(`/api/songs/${songId}/lyrics`)
    const lrcContent = await response.text()
    
    // 2. 解析 LRC 格式
    mockLyrics.value = parseLrc(lrcContent)
    
    if (mockLyrics.value.length === 0) {
      currentLyric.value = '♪ 纯音乐，请欣赏 ♪'
      nextLyric.value = ''
    } else {
      updateLyric(playerStore.currentTime || 0)
    }
  } catch (error) {
    console.error('加载歌词失败:', error)
    currentLyric.value = '♪ 歌词加载失败 ♪'
    mockLyrics.value = []
  }
}

// 解析 LRC 格式歌词
const parseLrc = (lrcText) => {
  const lines = lrcText.split('\n')
  const lyrics = []
  
  // LRC 格式示例：[00:12.00]歌词内容
  const timeRegex = /\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)/
  
  lines.forEach(line => {
    const match = line.match(timeRegex)
    if (match) {
      const minutes = parseInt(match[1])
      const seconds = parseInt(match[2])
      const milliseconds = parseInt(match[3])
      const text = match[4].trim()
      
      const time = minutes * 60 + seconds + milliseconds / 1000
      
      if (text) {
        lyrics.push({ time, text })
      }
    }
  })
  
  // 按时间排序
  return lyrics.sort((a, b) => a.time - b.time)
}
```

---

## ✅ 验证清单

完成以下检查，确保歌词同步正常：

- [ ] 打开桌面歌词后，控制台有日志输出
- [ ] 播放音乐时，歌词会自动切换
- [ ] 歌词切换时机与时间轴对应
- [ ] 暂停播放时，歌词停止更新
- [ ] 切换歌曲时，歌词自动加载
- [ ] 拖动进度条时，歌词立即跳转
- [ ] 下一句歌词正确显示
- [ ] 无歌曲时显示默认提示

---

## 🎯 性能优化建议

### 1. 减少不必要的更新
```javascript
// 只在歌词真正改变时更新
if (currentLyric.value !== newLyric) {
  currentLyric.value = newLyric
}
```

### 2. 使用防抖
```javascript
import { debounce } from 'lodash-es'

const updateLyric = debounce((currentTime) => {
  // ... 更新逻辑
}, 100)
```

### 3. 缓存歌词数据
```javascript
const lyricCache = new Map()

const loadLyrics = async (songId) => {
  // 先检查缓存
  if (lyricCache.has(songId)) {
    mockLyrics.value = lyricCache.get(songId)
    return
  }
  
  // 从 API 加载
  const lyrics = await fetchLyrics(songId)
  
  // 存入缓存
  lyricCache.set(songId, lyrics)
  mockLyrics.value = lyrics
}
```

---

## 📊 调试技巧

### 在组件中添加调试面板

```vue
<template>
  <div class="debug-panel" v-if="showDebug">
    <div>当前时间: {{ Math.floor(playerStore.currentTime) }}s</div>
    <div>当前歌词: {{ currentLyric }}</div>
    <div>下一句: {{ nextLyric }}</div>
    <div>歌词总数: {{ mockLyrics.length }}</div>
    <div>播放状态: {{ isPlaying ? '播放中' : '已暂停' }}</div>
  </div>
</template>

<script setup>
const showDebug = ref(false) // 按 Ctrl+D 切换

// 添加快捷键
onMounted(() => {
  window.addEventListener('keydown', (e) => {
    if (e.ctrlKey && e.key === 'd') {
      e.preventDefault()
      showDebug.value = !showDebug.value
    }
  })
})
</script>
```

---

希望这份文档能帮助你理解和调试歌词同步功能！🎵

