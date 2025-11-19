<template>
  <div class="music-visualizer" :class="{ active: isPlaying }">
    <canvas ref="canvasRef" :width="width" :height="height"></canvas>
    
    <!-- 简单的条形可视化（作为备用） -->
    <div v-if="!supportsCanvas" class="simple-bars">
      <div v-for="i in 20" :key="i" class="bar" :style="{ animationDelay: `${i * 0.05}s` }"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'

const props = defineProps({
  audioElement: {
    type: HTMLAudioElement,
    default: null
  },
  isPlaying: {
    type: Boolean,
    default: false
  },
  width: {
    type: Number,
    default: 300
  },
  height: {
    type: Number,
    default: 100
  },
  barColor: {
    type: String,
    default: '#667eea'
  },
  type: {
    type: String,
    default: 'bars' // bars, wave, circle
  }
})

const canvasRef = ref(null)
const supportsCanvas = ref(true)
let audioContext = null
let analyser = null
let dataArray = null
let bufferLength = 0
let animationId = null

onMounted(() => {
  if (!canvasRef.value) {
    supportsCanvas.value = false
    return
  }

  initVisualizer()
})

onBeforeUnmount(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  if (audioContext) {
    audioContext.close()
  }
})

watch(() => props.audioElement, (newAudio) => {
  if (newAudio && !audioContext) {
    initVisualizer()
  }
})

watch(() => props.isPlaying, (playing) => {
  if (playing) {
    draw()
  } else {
    if (animationId) {
      cancelAnimationFrame(animationId)
    }
  }
})

const initVisualizer = () => {
  if (!props.audioElement || !canvasRef.value) return

  try {
    // 创建音频上下文
    const AudioContext = window.AudioContext || window.webkitAudioContext
    audioContext = new AudioContext()
    
    // 创建分析器
    analyser = audioContext.createAnalyser()
    analyser.fftSize = 256
    bufferLength = analyser.frequencyBinCount
    dataArray = new Uint8Array(bufferLength)
    
    // 连接音频源
    const source = audioContext.createMediaElementSource(props.audioElement)
    source.connect(analyser)
    analyser.connect(audioContext.destination)
    
    if (props.isPlaying) {
      draw()
    }
  } catch (error) {
    console.error('音频可视化初始化失败:', error)
    supportsCanvas.value = false
  }
}

const draw = () => {
  if (!canvasRef.value || !analyser) return

  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')
  const WIDTH = canvas.width
  const HEIGHT = canvas.height

  animationId = requestAnimationFrame(draw)

  analyser.getByteFrequencyData(dataArray)

  // 清空画布
  ctx.fillStyle = 'transparent'
  ctx.fillRect(0, 0, WIDTH, HEIGHT)

  if (props.type === 'bars') {
    drawBars(ctx, WIDTH, HEIGHT)
  } else if (props.type === 'wave') {
    drawWave(ctx, WIDTH, HEIGHT)
  } else if (props.type === 'circle') {
    drawCircle(ctx, WIDTH, HEIGHT)
  }
}

const drawBars = (ctx, WIDTH, HEIGHT) => {
  const barWidth = (WIDTH / bufferLength) * 2.5
  let barHeight
  let x = 0

  for (let i = 0; i < bufferLength; i++) {
    barHeight = (dataArray[i] / 255) * HEIGHT

    // 创建渐变
    const gradient = ctx.createLinearGradient(0, HEIGHT, 0, HEIGHT - barHeight)
    gradient.addColorStop(0, props.barColor)
    gradient.addColorStop(1, props.barColor + '80')

    ctx.fillStyle = gradient
    ctx.fillRect(x, HEIGHT - barHeight, barWidth, barHeight)

    x += barWidth + 1
  }
}

const drawWave = (ctx, WIDTH, HEIGHT) => {
  ctx.lineWidth = 2
  ctx.strokeStyle = props.barColor
  ctx.beginPath()

  const sliceWidth = WIDTH / bufferLength
  let x = 0

  for (let i = 0; i < bufferLength; i++) {
    const v = dataArray[i] / 128.0
    const y = v * HEIGHT / 2

    if (i === 0) {
      ctx.moveTo(x, y)
    } else {
      ctx.lineTo(x, y)
    }

    x += sliceWidth
  }

  ctx.lineTo(WIDTH, HEIGHT / 2)
  ctx.stroke()
}

const drawCircle = (ctx, WIDTH, HEIGHT) => {
  const centerX = WIDTH / 2
  const centerY = HEIGHT / 2
  const radius = Math.min(WIDTH, HEIGHT) / 3

  ctx.beginPath()
  ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI)
  ctx.strokeStyle = props.barColor
  ctx.lineWidth = 2
  ctx.stroke()

  const angleStep = (Math.PI * 2) / bufferLength

  for (let i = 0; i < bufferLength; i++) {
    const angle = i * angleStep
    const barHeight = (dataArray[i] / 255) * radius

    const x1 = centerX + Math.cos(angle) * radius
    const y1 = centerY + Math.sin(angle) * radius
    const x2 = centerX + Math.cos(angle) * (radius + barHeight)
    const y2 = centerY + Math.sin(angle) * (radius + barHeight)

    ctx.beginPath()
    ctx.moveTo(x1, y1)
    ctx.lineTo(x2, y2)
    ctx.strokeStyle = props.barColor
    ctx.lineWidth = 2
    ctx.stroke()
  }
}
</script>

<style scoped>
.music-visualizer {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

canvas {
  width: 100%;
  height: 100%;
}

/* 简单条形可视化（备用） */
.simple-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 100%;
  width: 100%;
  padding: 10px;
  gap: 4px;
}

.bar {
  flex: 1;
  background: linear-gradient(to top, var(--color-primary), var(--color-secondary));
  border-radius: 2px 2px 0 0;
  animation: pulse-bar 0.8s ease-in-out infinite;
}

@keyframes pulse-bar {
  0%, 100% {
    height: 20%;
  }
  50% {
    height: 80%;
  }
}

.music-visualizer:not(.active) canvas,
.music-visualizer:not(.active) .simple-bars {
  opacity: 0.3;
}
</style>

