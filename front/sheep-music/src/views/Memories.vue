<template>
  <div class="memories-scope">
    <!-- 漂浮花瓣/爱心画布（reduced-motion 时静态隐藏） -->
    <canvas
      ref="petalCanvas"
      class="petal-canvas"
    />

    <div class="mem-page">
      <!-- 顶部：问候 + 统计 -->
      <header class="mem-header">
        <div class="mem-title-row">
          <h2 class="mem-title">
            婉婉小屋
            <span class="mem-title-heart">♥</span>
          </h2>
        </div>
        <p class="mem-subtitle">
          {{ greeting }}，{{ userStore.userInfo?.nickname || '欢迎回来' }}
        </p>
        <p
          v-if="total > 0"
          class="mem-count"
        >
          小屋里珍藏着 <b>{{ total }}</b> 个瞬间<template v-if="myStarCount > 0">
            · 你点亮过 <b>{{ myStarCount }}</b> 颗星</template>
        </p>
        <div class="mem-actions">
          <button
            v-if="myStarCount > 0"
            class="mem-star-map-btn"
            @click="starMapOpen = true"
          >
            ✦ 星图
          </button>
        </div>
      </header>

      <!-- 加载中 -->
      <div
        v-if="loading"
        class="mem-loading"
      >
        <span class="mem-loading-heart">♡</span>
        <span>小屋的门正在打开…</span>
      </div>

      <!-- 空状态：小屋正在布置中 -->
      <div
        v-else-if="!total"
        class="mem-empty"
      >
        <div class="mem-empty-icon">🏡</div>
        <p class="mem-empty-title">
          小屋正在布置中
        </p>
        <p class="mem-empty-hint">
          等第一张照片贴上墙，这里就会亮起来
        </p>
      </div>

      <template v-else>
        <!-- 每日瞬间：拍立得卡 -->
        <section
          v-if="todayPick"
          class="mem-today"
        >
          <div class="mem-today-label">
            <span class="mem-today-dot" />今天的瞬间
          </div>
          <div class="polaroid">
            <img
              v-if="todayPick.type === 'photo'"
              :src="todayPick.mediaUrl"
              class="polaroid-img"
              @click="openLightbox(todayPick)"
            >
            <video
              v-else
              :src="todayPick.mediaUrl"
              :poster="todayPick.coverUrl || undefined"
              class="polaroid-img polaroid-video"
              muted
              playsinline
              preload="metadata"
              @click="openLightbox(todayPick)"
            />
            <div class="polaroid-caption">
              <p class="polaroid-text">
                {{ todayPick.caption || todayPick.title || '' }}
              </p>
              <p class="polaroid-date">
                {{ fmtDate(todayPick) }}
              </p>
            </div>
          </div>
        </section>

        <!-- 筛选 -->
        <div class="mem-filter">
          <button
            v-for="f in filters"
            :key="f.key"
            class="mem-filter-btn"
            :class="{ active: activeFilter === f.key }"
            @click="activeFilter = f.key"
          >
            {{ f.label }}
            <span
              v-if="f.count"
              class="mem-filter-count"
            >{{ f.count }}</span>
          </button>
        </div>

        <!-- 按月分组瀑布流 -->
        <section
          v-for="group in visibleGroups"
          :key="group.month"
          class="mem-group"
        >
          <h3 class="mem-month">
            <span class="mem-month-bar" />{{ group.month }}
          </h3>
          <div class="mem-masonry">
            <div
              v-for="(item, idx) in group.items"
              :key="item.id"
              class="mem-card"
              :style="{ animationDelay: `${Math.min(idx, 8) * 60}ms` }"
              @click="openLightbox(item)"
            >
              <img
                v-if="item.type === 'photo'"
                :src="ossThumb(item.mediaUrl, 480)"
                class="mem-card-img"
                loading="lazy"
                alt=""
              >
              <video
                v-else
                :src="item.mediaUrl"
                :poster="item.coverUrl || undefined"
                class="mem-card-img mem-card-video"
                muted
                playsinline
                preload="metadata"
              />
              <span
                v-if="item.type === 'video'"
                class="mem-play-badge"
              >▶</span>
              <span
                v-if="item.songTitle"
                class="mem-song-badge"
                :title="`那天的歌：${item.songTitle}`"
              >♪</span>
              <div class="mem-card-overlay">
                <p class="mem-card-caption">
                  {{ item.caption || item.title || '' }}
                </p>
              </div>
            </div>
          </div>
        </section>
      </template>
    </div>

    <!-- 抱抱悬浮按钮 -->
    <button
      class="hug-fab"
      :disabled="hugging"
      title="需要抱抱"
      @click="onHug"
    >
      <span class="hug-fab-emoji">🫂</span>
      <span class="hug-fab-label">需要抱抱</span>
    </button>

    <!-- 星图 -->
    <StarMapOverlay
      v-model="starMapOpen"
      @open-item="openStarMapItem"
    />

    <!-- 拥抱动画 -->
    <HugOverlay
      v-if="hugVisible"
      :song="hugSong"
      @close="hugVisible = false"
    />

    <!-- 灯箱 -->
    <MemoryLightbox
      v-if="lightboxItem"
      :item="lightboxItem"
      :starred="!!starMap[lightboxItem.id]"
      :star-count="lightboxItem.starCount || 0"
      :has-prev="lightboxIndex > 0"
      :has-next="lightboxIndex < flatItems.length - 1"
      @close="lightboxItem = null"
      @prev="stepLightbox(-1)"
      @next="stepLightbox(1)"
      @star="onStarToggled"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useUserStore } from '@/store/user'
import { ossThumb } from '@/utils/image'
import { getMemoryList, sendHug } from '@/api/memory'
import MemoryLightbox from '@/components/MemoryLightbox.vue'
import StarMapOverlay from '@/components/StarMapOverlay.vue'
import HugOverlay from '@/components/HugOverlay.vue'

const userStore = useUserStore()
const loading = ref(true)
const groups = ref([])
const total = ref(0)
const myStarCount = ref(0)
const todayPick = ref(null)
const starMap = ref({}) // itemId -> starred

const activeFilter = ref('all')
const filters = computed(() => [
  { key: 'all', label: '全部', count: total.value },
  { key: 'photo', label: '照片', count: countType('photo') },
  { key: 'video', label: '视频', count: countType('video') }
])
const countType = (t) => groups.value.reduce((n, g) => n + g.items.filter(i => i.type === t).length, 0)

const visibleGroups = computed(() => {
  if (activeFilter.value === 'all') return groups.value
  return groups.value
    .map(g => ({ month: g.month, items: g.items.filter(i => i.type === activeFilter.value) }))
    .filter(g => g.items.length)
})

const flatItems = computed(() => visibleGroups.value.flatMap(g => g.items))
const lightboxItem = ref(null)
const lightboxIndex = computed(() => flatItems.value.findIndex(i => i.id === lightboxItem.value?.id))

// ===== v1.2：星图 + 抱抱 =====
const starMapOpen = ref(false)
const hugVisible = ref(false)
const hugSong = ref(null)
const hugging = ref(false)

// 星图上的星星 → 直接打开该瞬间的灯箱（星图里的星必然是当前用户点亮的）
const openStarMapItem = (item) => {
  starMapOpen.value = false
  starMap.value[item.id] = true
  lightboxItem.value = item
}

const onHug = async () => {
  if (hugging.value) return
  hugging.value = true
  try {
    const res = await sendHug()
    hugSong.value = res.data || null
    hugVisible.value = true
  } catch (e) {
    // 冷却等错误由拦截器提示
  } finally {
    hugging.value = false
  }
}

const openLightbox = (item) => { lightboxItem.value = item }
const stepLightbox = (dir) => {
  const idx = lightboxIndex.value + dir
  if (idx >= 0 && idx < flatItems.value.length) {
    lightboxItem.value = flatItems.value[idx]
  }
}
const onStarToggled = ({ id, starred, starCount, myStarCount: mine }) => {
  starMap.value[id] = starred
  if (typeof mine === 'number') myStarCount.value = mine
  if (lightboxItem.value?.id === id) {
    lightboxItem.value = { ...lightboxItem.value, starCount }
  }
  const g = groups.value.flatMap(x => x.items).find(i => i.id === id)
  if (g) g.starCount = starCount
}

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 5) return '夜深了'
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const fmtDate = (item) => {
  const d = item.memoryDate || (item.createTime || '').slice(0, 10) || ''
  return d ? d.replace(/-/g, '.') : ''
}

const load = async () => {
  loading.value = true
  try {
    const res = await getMemoryList()
    const data = res.data || {}
    groups.value = data.groups || []
    total.value = data.total || 0
    myStarCount.value = data.myStarCount || 0
    todayPick.value = data.todayPick || null
  } catch (e) {
    // request 拦截器已提示
  } finally {
    loading.value = false
  }
}

// ===== 漂浮花瓣/爱心粒子（≤30 个；reduced-motion 跳过） =====
const petalCanvas = ref(null)
let rafId = 0
const startPetals = () => {
  const canvas = petalCanvas.value
  if (!canvas) return
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const ctx = canvas.getContext('2d')
  const DPR = Math.min(window.devicePixelRatio || 1, 2)
  const resize = () => {
    canvas.width = canvas.offsetWidth * DPR
    canvas.height = canvas.offsetHeight * DPR
  }
  resize()
  window.addEventListener('resize', resize)

  const COLORS = ['#ffb3c6', '#ff8fab', '#ffc9d9', '#ffe5ec']
  const petals = Array.from({ length: 30 }, () => ({
    x: Math.random(),
    y: Math.random(),
    size: 4 + Math.random() * 7,
    speed: 0.15 + Math.random() * 0.35,
    sway: Math.random() * Math.PI * 2,
    swaySpeed: 0.005 + Math.random() * 0.01,
    color: COLORS[(Math.random() * COLORS.length) | 0],
    heart: Math.random() < 0.25
  }))

  const drawHeart = (x, y, s, color) => {
    ctx.fillStyle = color
    ctx.beginPath()
    ctx.moveTo(x, y + s * 0.3)
    ctx.bezierCurveTo(x, y, x - s, y - s * 0.4, x - s, y + s * 0.3)
    ctx.bezierCurveTo(x - s, y + s, x, y + s * 1.2, x, y + s * 1.7)
    ctx.bezierCurveTo(x, y + s * 1.2, x + s, y + s, x + s, y + s * 0.3)
    ctx.bezierCurveTo(x + s, y - s * 0.4, x, y, x, y + s * 0.3)
    ctx.fill()
  }

  const tick = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    for (const p of petals) {
      p.y += p.speed / canvas.height * DPR
      p.sway += p.swaySpeed
      if (p.y > 1.05) { p.y = -0.05; p.x = Math.random() }
      const x = (p.x + Math.sin(p.sway) * 0.02) * canvas.width
      const y = p.y * canvas.height
      if (p.heart) {
        drawHeart(x, y, p.size * DPR, p.color)
      } else {
        // 花瓣：椭圆 + 轻微旋转
        ctx.save()
        ctx.translate(x, y)
        ctx.rotate(p.sway)
        ctx.fillStyle = p.color
        ctx.beginPath()
        ctx.ellipse(0, 0, p.size * DPR, p.size * DPR * 0.55, 0, 0, Math.PI * 2)
        ctx.fill()
        ctx.restore()
      }
    }
    rafId = requestAnimationFrame(tick)
  }
  tick()
  petalCanvas.value._cleanup = () => {
    cancelAnimationFrame(rafId)
    window.removeEventListener('resize', resize)
  }
}

onMounted(async () => {
  await load()
  await nextTick()
  startPetals()
})
onBeforeUnmount(() => {
  if (petalCanvas.value?._cleanup) petalCanvas.value._cleanup()
  cancelAnimationFrame(rafId)
})
</script>

<style scoped>
/* ===== 婉婉小屋：粉嫩风（作用域隔离，不污染全局主题） ===== */
.memories-scope {
  --mem-bg: #fff5f7;
  --mem-bg-2: #ffe5ec;
  --mem-primary: #ff8fab;
  --mem-deep: #c9184a;
  --mem-ink: #6d3b47;
  --mem-ink-light: #a2637a;
  --mem-card: #ffffff;
  --mem-radius: 18px;

  position: relative;
  min-height: 100%;
  background: linear-gradient(160deg, var(--mem-bg) 0%, #fff9fa 45%, var(--mem-bg-2) 100%);
  border-radius: var(--mem-radius);
  overflow: hidden;
}

.petal-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.mem-page {
  position: relative;
  z-index: 1;
  max-width: 1060px;
  margin: 0 auto;
  padding: 34px 22px 80px;
}

/* ===== 顶部 ===== */
.mem-header { text-align: center; margin-bottom: 30px; }
.mem-title {
  font-size: 34px;
  font-weight: 700;
  color: var(--mem-deep);
  letter-spacing: 0.06em;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}
.mem-title-heart {
  color: var(--mem-primary);
  animation: heartbeat 2.4s ease-in-out infinite;
  display: inline-block;
}
@keyframes heartbeat {
  0%, 100% { transform: scale(1); }
  12% { transform: scale(1.22); }
  24% { transform: scale(1); }
}
.mem-subtitle {
  margin-top: 10px;
  font-size: 15px;
  color: var(--mem-ink);
}
.mem-count {
  margin-top: 6px;
  font-size: 13px;
  color: var(--mem-ink-light);
}
.mem-count b { color: var(--mem-deep); font-weight: 600; }

/* ===== 加载/空状态 ===== */
.mem-loading, .mem-empty {
  text-align: center;
  padding: 90px 20px;
  color: var(--mem-ink-light);
  font-size: 14px;
}
.mem-loading-heart {
  display: block;
  font-size: 40px;
  color: var(--mem-primary);
  margin-bottom: 14px;
  animation: heartbeat 1.6s ease-in-out infinite;
}
.mem-empty-icon { font-size: 56px; margin-bottom: 16px; }
.mem-empty-title { font-size: 17px; color: var(--mem-ink); font-weight: 600; }
.mem-empty-hint { margin-top: 8px; }

/* ===== 每日瞬间：拍立得 ===== */
.mem-today { display: flex; flex-direction: column; align-items: center; margin-bottom: 40px; }
.mem-today-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  letter-spacing: 0.28em;
  color: var(--mem-deep);
  margin-bottom: 16px;
}
.mem-today-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: var(--mem-primary);
  box-shadow: 0 0 0 4px rgba(255, 143, 171, 0.25);
  animation: pulse 2.2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 4px rgba(255, 143, 171, 0.25); }
  50% { box-shadow: 0 0 0 8px rgba(255, 143, 171, 0.12); }
}

.polaroid {
  background: #fff;
  padding: 14px 14px 0;
  border-radius: 8px;
  box-shadow: 0 18px 46px rgba(201, 24, 74, 0.18);
  transform: rotate(-1.2deg);
  transition: transform 0.3s ease;
  max-width: 420px;
  width: 100%;
  cursor: zoom-in;
}
.polaroid:hover { transform: rotate(0deg) scale(1.02); }
.polaroid-img {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  border-radius: 4px;
  background: var(--mem-bg-2);
  display: block;
}
.polaroid-caption { padding: 12px 6px 14px; text-align: center; }
.polaroid-text {
  font-family: 'STKaiti', 'KaiTi', 'Noto Serif SC', serif;
  font-size: 15px;
  color: var(--mem-ink);
  line-height: 1.7;
  white-space: pre-wrap;
}
.polaroid-date {
  margin-top: 6px;
  font-size: 12px;
  color: var(--mem-primary);
  letter-spacing: 0.18em;
}

/* ===== 筛选 ===== */
.mem-filter {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 28px;
}
.mem-filter-btn {
  border: 1.5px solid transparent;
  background: rgba(255, 255, 255, 0.7);
  color: var(--mem-ink);
  border-radius: 999px;
  padding: 7px 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.mem-filter-btn:hover { border-color: var(--mem-primary); }
.mem-filter-btn.active {
  background: linear-gradient(135deg, var(--mem-primary), #ff5d8f);
  color: #fff;
  box-shadow: 0 6px 18px rgba(255, 93, 143, 0.35);
}
.mem-filter-count {
  background: rgba(201, 24, 74, 0.1);
  border-radius: 999px;
  padding: 1px 8px;
  font-size: 11px;
  color: var(--mem-deep);
}
.mem-filter-btn.active .mem-filter-count {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

/* ===== 按月分组 + 瀑布流 ===== */
.mem-group { margin-bottom: 36px; }
.mem-month {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  color: var(--mem-deep);
  font-weight: 600;
  letter-spacing: 0.14em;
  margin-bottom: 16px;
}
.mem-month-bar {
  width: 22px; height: 4px;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--mem-primary), var(--mem-bg-2));
}

.mem-masonry {
  column-count: 3;
  column-gap: 16px;
}
.mem-card {
  position: relative;
  break-inside: avoid;
  margin-bottom: 16px;
  border-radius: 14px;
  overflow: hidden;
  cursor: zoom-in;
  background: var(--mem-card);
  box-shadow: 0 8px 24px rgba(201, 24, 74, 0.1);
  animation: card-in 0.5s cubic-bezier(0.22, 1, 0.36, 1) backwards;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.mem-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 36px rgba(201, 24, 74, 0.2);
}
@keyframes card-in {
  from { opacity: 0; transform: translateY(18px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.mem-card-img {
  width: 100%;
  display: block;
  background: var(--mem-bg-2);
}
.mem-card-video { pointer-events: none; }
.mem-play-badge {
  position: absolute;
  right: 10px; bottom: 10px;
  width: 30px; height: 30px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  color: var(--mem-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  box-shadow: 0 4px 12px rgba(201, 24, 74, 0.25);
}
.mem-song-badge {
  position: absolute;
  left: 10px; bottom: 10px;
  width: 30px; height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--mem-primary), #ff5d8f);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  box-shadow: 0 4px 12px rgba(201, 24, 74, 0.3);
}
.mem-card-overlay {
  position: absolute;
  left: 0; right: 0; bottom: 0;
  padding: 26px 12px 10px;
  background: linear-gradient(transparent, rgba(105, 39, 58, 0.62));
  opacity: 0;
  transition: opacity 0.25s;
}
.mem-card:hover .mem-card-overlay { opacity: 1; }
.mem-card-caption {
  font-size: 12.5px;
  color: #fff;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .mem-masonry { column-count: 2; }
}
@media (max-width: 560px) {
  .mem-page { padding: 22px 14px 60px; }
  .mem-title { font-size: 27px; }
  .mem-masonry { column-count: 1; }
  .polaroid { max-width: 100%; }
}

/* ===== 星图入口 + 抱抱 FAB ===== */
.mem-actions {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
.mem-star-map-btn {
  border: 1.5px solid rgba(201, 24, 74, 0.25);
  background: rgba(255, 255, 255, 0.7);
  color: var(--mem-deep);
  border-radius: 999px;
  padding: 7px 22px;
  font-size: 13px;
  letter-spacing: 0.14em;
  cursor: pointer;
  transition: all 0.2s;
}
.mem-star-map-btn:hover {
  border-color: var(--mem-primary);
  box-shadow: 0 6px 18px rgba(255, 143, 171, 0.3);
  transform: translateY(-1px);
}

.hug-fab {
  position: absolute;
  right: 22px;
  bottom: 26px;
  z-index: 5;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #ff8fab, #ff5d8f);
  color: #fff;
  padding: 10px 18px;
  display: flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  box-shadow: 0 10px 26px rgba(255, 93, 143, 0.45);
  transition: transform 0.2s, box-shadow 0.2s;
}
.hug-fab:hover {
  transform: translateY(-2px) scale(1.03);
  box-shadow: 0 14px 32px rgba(255, 93, 143, 0.55);
}
.hug-fab:disabled { opacity: 0.7; cursor: wait; }
.hug-fab-emoji { font-size: 18px; }
.hug-fab-label { font-size: 13px; letter-spacing: 0.06em; }

@media (max-width: 560px) {
  .hug-fab { right: 14px; bottom: 18px; padding: 9px 14px; }
}

/* ===== 动效降级 ===== */
@media (prefers-reduced-motion: reduce) {
  .petal-canvas { display: none; }
  .mem-card { animation: none; }
  .mem-title-heart, .mem-loading-heart, .mem-today-dot { animation: none; }
}
</style>
