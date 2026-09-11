<template>
  <Teleport to="body">
    <div
      class="star-map-overlay"
      @click.self="close"
    >
      <button
        class="sm-close"
        aria-label="关闭星图"
        @click="close"
      >
        ✕
      </button>

      <div
        v-if="loading"
        class="sm-loading"
      >
        星空正在展开…
      </div>

      <div
        v-else-if="!stars.length"
        class="sm-empty"
      >
        <div class="sm-empty-icon">✦</div>
        <p class="sm-empty-title">
          这片星空还空着
        </p>
        <p class="sm-empty-hint">
          回到小屋，点亮第一颗星星吧
        </p>
      </div>

      <template v-else>
        <canvas
          ref="canvasRef"
          class="sm-canvas"
          @click="onCanvasClick"
        />
        <div class="sm-caption">
          <p class="sm-count">
            你在这里点亮过 <b>{{ stars.length }}</b> 颗星
          </p>
          <p class="sm-sub">
            每一颗，都是你来过的痕迹
          </p>
        </div>
      </template>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch, onBeforeUnmount, nextTick } from 'vue'
import { getStarMap } from '@/api/memory'

const props = defineProps({
  open: { type: Boolean, default: false }
})
const emit = defineEmits(['update:open', 'open-item'])

const loading = ref(false)
const stars = ref([])
const canvasRef = ref(null)

const close = () => emit('update:open', false)

// 黄金角螺旋：同一颗星永远在同一位置（星星只会长，不会乱）
const GOLDEN_ANGLE = 137.508 * Math.PI / 180
const hashOffset = (id) => ((id * 2654435761) % 1000) / 1000

const layout = [] // {x, y, r, star}
let rafId = 0
let reduceMotion = false

const buildLayout = (w, h) => {
  layout.length = 0
  const maxR = Math.min(w, h) * 0.44
  stars.value.forEach((s, i) => {
    const theta = i * GOLDEN_ANGLE + hashOffset(s.starId) * 0.35
    const radius = maxR * Math.sqrt((i + 0.7) / stars.value.length)
    layout.push({
      x: w / 2 + Math.cos(theta) * radius,
      y: h / 2 + Math.sin(theta) * radius * 0.86,
      r: 2.2 + (s.starId % 5) * 0.5 + (i === stars.value.length - 1 ? 1 : 0),
      star: s
    })
  })
}

const draw = (ts) => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const DPR = Math.min(window.devicePixelRatio || 1, 2)
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  const t = ts / 1000
  for (const p of layout) {
    const twinkle = reduceMotion ? 0.85 : 0.65 + 0.35 * Math.sin(t * 1.8 + p.star.starId % 10)
    const r = p.r * DPR
    // 光晕
    const glow = ctx.createRadialGradient(p.x * DPR, p.y * DPR, 0, p.x * DPR, p.y * DPR, r * 4)
    glow.addColorStop(0, `rgba(255, 235, 190, ${0.55 * twinkle})`)
    glow.addColorStop(1, 'rgba(255, 235, 190, 0)')
    ctx.fillStyle = glow
    ctx.beginPath()
    ctx.arc(p.x * DPR, p.y * DPR, r * 4, 0, Math.PI * 2)
    ctx.fill()
    // 星核
    ctx.fillStyle = `rgba(255, 248, 225, ${0.75 + 0.25 * twinkle})`
    ctx.beginPath()
    ctx.arc(p.x * DPR, p.y * DPR, r, 0, Math.PI * 2)
    ctx.fill()
  }
  if (!reduceMotion) rafId = requestAnimationFrame(draw)
}

const setupCanvas = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const DPR = Math.min(window.devicePixelRatio || 1, 2)
  canvas.width = canvas.offsetWidth * DPR
  canvas.height = canvas.offsetHeight * DPR
  buildLayout(canvas.offsetWidth, canvas.offsetHeight)
}

const onCanvasClick = (e) => {
  const rect = e.currentTarget.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  let best = null
  let bestDist = 20
  for (const p of layout) {
    const d = Math.hypot(p.x - x, p.y - y)
    if (d < bestDist) { bestDist = d; best = p }
  }
  if (best) {
    const s = best.star
    emit('open-item', {
      id: s.itemId,
      type: s.type,
      mediaUrl: s.thumb,
      title: s.title,
      caption: s.caption,
      memoryDate: s.memoryDate,
      starCount: 1
    })
  }
}

let onResize
watch(() => props.open, async (v) => {
  if (!v) {
    cancelAnimationFrame(rafId)
    return
  }
  loading.value = true
  try {
    const res = await getStarMap()
    stars.value = res.data || []
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
  if (!stars.value.length) return
  await nextTick()
  reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  setupCanvas()
  onResize = () => setupCanvas()
  window.addEventListener('resize', onResize)
  if (!reduceMotion) rafId = requestAnimationFrame(draw)
  else draw(0)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  if (onResize) window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.star-map-overlay {
  position: fixed;
  inset: 0;
  z-index: 3200;
  background: radial-gradient(ellipse at 50% 30%, #1b1035 0%, #0d0a1f 55%, #070513 100%);
  overflow: hidden;
}

.sm-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.sm-close {
  position: absolute;
  top: 18px;
  right: 22px;
  z-index: 2;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  color: #e8dff5;
  font-size: 18px;
  cursor: pointer;
  transition: transform 0.2s;
}
.sm-close:hover { transform: rotate(90deg); }

.sm-caption {
  position: absolute;
  top: 26px;
  left: 0;
  right: 0;
  text-align: center;
  pointer-events: none;
}
.sm-count {
  font-size: 15px;
  color: #e8dff5;
  letter-spacing: 0.08em;
}
.sm-count b { color: #ffe9b8; }
.sm-sub {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(232, 223, 245, 0.55);
  letter-spacing: 0.22em;
}

.sm-loading, .sm-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: rgba(232, 223, 245, 0.7);
  font-size: 14px;
  gap: 6px;
}
.sm-empty-icon { font-size: 44px; color: #ffe9b8; }
.sm-empty-title { font-size: 16px; color: #e8dff5; }
.sm-empty-hint { font-size: 12.5px; color: rgba(232, 223, 245, 0.5); }
</style>
