<template>
  <div class="dynamic-background" :style="backgroundStyle">
    <canvas ref="canvasRef" class="gradient-canvas"></canvas>
    <div class="blur-overlay"></div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { usePlayerStore } from '@/store/player'

const playerStore = usePlayerStore()
const canvasRef = ref(null)
const dominantColors = ref(['#1e1e2e', '#2a2a3e'])

// 动画相关
let animationId = null
let gradientAngle = 0

// 预设的颜色主题（根据不同音乐风格）
const colorThemes = [
  ['#667eea', '#764ba2'], // 紫色渐变
  ['#f093fb', '#f5576c'], // 粉色渐变
  ['#4facfe', '#00f2fe'], // 蓝色渐变
  ['#43e97b', '#38f9d7'], // 绿色渐变
  ['#fa709a', '#fee140'], // 暖色渐变
  ['#30cfd0', '#330867'], // 深蓝渐变
  ['#a8edea', '#fed6e3'], // 柔和渐变
  ['#ff9a9e', '#fecfef'], // 玫瑰渐变
]

// 计算背景样式
const backgroundStyle = computed(() => ({
  background: `linear-gradient(${gradientAngle}deg, ${dominantColors.value[0]}, ${dominantColors.value[1]})`,
}))

// 处理 OSS URL，使用服务器代理避免跨域
const processImageUrl = (url) => {
  if (!url) return ''
  
  const ossHost = 'https://sheepmusic.oss-cn-hangzhou.aliyuncs.com'
  if (url.startsWith(ossHost)) {
    return url.replace(ossHost, '/api/oss')
  }
  
  return url
}

// 简单的颜色提取方法（使用 Canvas API）
const extractColors = async (imageUrl) => {
  if (!imageUrl) return
  
  try {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    
    // 处理 URL
    const processedUrl = processImageUrl(imageUrl)
    
    await new Promise((resolve, reject) => {
      img.onload = resolve
      img.onerror = reject
      img.src = processedUrl
    })
    
    // 创建临时 canvas
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    canvas.width = 50
    canvas.height = 50
    
    // 绘制缩小的图片
    ctx.drawImage(img, 0, 0, 50, 50)
    
    // 获取图片数据
    const imageData = ctx.getImageData(0, 0, 50, 50)
    const data = imageData.data
    
    // 简单的颜色采样
    const colors = []
    const step = 5 // 每5个像素采样一次
    
    for (let i = 0; i < data.length; i += 4 * step) {
      const r = data[i]
      const g = data[i + 1]
      const b = data[i + 2]
      
      // 过滤掉太暗或太亮的颜色
      const brightness = (r + g + b) / 3
      if (brightness > 30 && brightness < 225) {
        colors.push({ r, g, b })
      }
    }
    
    if (colors.length > 0) {
      // 简单的颜色聚类，取两个主要颜色
      const color1 = colors[Math.floor(colors.length * 0.25)]
      const color2 = colors[Math.floor(colors.length * 0.75)]
      
      dominantColors.value = [
        `rgb(${color1.r}, ${color1.g}, ${color1.b})`,
        `rgb(${color2.r}, ${color2.g}, ${color2.b})`
      ]
    } else {
      // 使用随机主题
      const randomTheme = colorThemes[Math.floor(Math.random() * colorThemes.length)]
      dominantColors.value = randomTheme
    }
    
    // 添加渐变动画
    animateGradient()
  } catch (error) {
    console.warn('无法提取颜色，使用默认主题:', error)
    // 使用随机主题
    const randomTheme = colorThemes[Math.floor(Math.random() * colorThemes.length)]
    dominantColors.value = randomTheme
    animateGradient()
  }
}

// 渐变动画
const animateGradient = () => {
  const animate = () => {
    gradientAngle = (gradientAngle + 0.5) % 360
    
    if (canvasRef.value) {
      const ctx = canvasRef.value.getContext('2d')
      const width = window.innerWidth
      const height = window.innerHeight
      
      canvasRef.value.width = width
      canvasRef.value.height = height
      
      // 创建流动的渐变效果
      const gradient = ctx.createLinearGradient(
        width * Math.cos(gradientAngle * Math.PI / 180),
        height * Math.sin(gradientAngle * Math.PI / 180),
        width * Math.cos((gradientAngle + 180) * Math.PI / 180),
        height * Math.sin((gradientAngle + 180) * Math.PI / 180)
      )
      
      // 将 rgb 颜色转换为 rgba 格式
      const color1 = dominantColors.value[0].replace('rgb', 'rgba').replace(')', ', 0.25)')
      const color2 = dominantColors.value[1].replace('rgb', 'rgba').replace(')', ', 0.4)')
      
      gradient.addColorStop(0, color1)
      gradient.addColorStop(0.5, color2)
      gradient.addColorStop(1, color1)
      
      ctx.fillStyle = gradient
      ctx.fillRect(0, 0, width, height)
      
      // 添加噪点效果
      addNoise(ctx, width, height)
    }
    
    animationId = requestAnimationFrame(animate)
  }
  
  animate()
}

// 添加噪点纹理（优化版本，减少性能消耗）
let noiseFrameCount = 0
const addNoise = (ctx, width, height) => {
  // 每 5 帧才更新一次噪点，减少性能消耗
  noiseFrameCount++
  if (noiseFrameCount % 5 !== 0) return
  
  try {
    const imageData = ctx.getImageData(0, 0, width, height)
    const data = imageData.data
    
    // 只处理部分像素，进一步减少计算量
    for (let i = 0; i < data.length; i += 16) { // 每隔 4 个像素处理一次
      const noise = (Math.random() - 0.5) * 10
      data[i] += noise     // R
      data[i + 1] += noise // G
      data[i + 2] += noise // B
    }
    
    ctx.putImageData(imageData, 0, 0)
  } catch (error) {
    // 忽略 getImageData 错误
    console.warn('Canvas getImageData 调用失败:', error)
  }
}

// 监听当前歌曲变化
watch(() => playerStore.currentSong?.cover, (newCover) => {
  if (newCover) {
    extractColors(newCover)
  }
}, { immediate: true })

// 监听播放状态（仅控制动画，不影响音频）
watch(() => playerStore.isPlaying, (isPlaying) => {
  // 注意：这里只控制背景动画，不应该影响音频播放
  if (isPlaying && !animationId) {
    animateGradient()
  } else if (!isPlaying && animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
})

// 组件挂载时初始化
onMounted(() => {
  // 如果正在播放，启动动画
  if (playerStore.isPlaying) {
    animateGradient()
  }
  
  // 监听页面可见性变化，确保音频不受影响
  const handleVisibilityChange = () => {
    if (document.hidden) {
      // 页面不可见时，停止背景动画以节省性能，但不影响音频
      if (animationId) {
        cancelAnimationFrame(animationId)
        animationId = null
      }
    } else {
      // 页面可见时，如果正在播放则恢复动画
      if (playerStore.isPlaying && !animationId) {
        animateGradient()
      }
    }
  }
  
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  // 清理动画帧
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  // 移除事件监听器会自动清理
})
</script>

<style scoped>
.dynamic-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
  transition: background 2s ease;
  overflow: hidden;
}

.gradient-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0.6;
  mix-blend-mode: multiply;
}

.blur-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  backdrop-filter: blur(100px);
  background: radial-gradient(
    circle at center,
    transparent 0%,
    rgba(0, 0, 0, 0.3) 100%
  );
}

/* 暗色主题适配 */
:root[data-theme="dark"] .dynamic-background {
  opacity: 0.3;
}

:root[data-theme="dark"] .blur-overlay {
  background: radial-gradient(
    circle at center,
    transparent 0%,
    rgba(0, 0, 0, 0.6) 100%
  );
}
</style>
