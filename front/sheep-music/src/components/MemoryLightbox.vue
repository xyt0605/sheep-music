<template>
  <Teleport to="body">
    <div
      class="mem-lightbox"
      @click.self="close"
    >
      <button
        class="lb-close"
        aria-label="关闭"
        @click="close"
      >
        ✕
      </button>

      <button
        v-if="hasPrev"
        class="lb-nav lb-prev"
        aria-label="上一张"
        @click="prev"
      >
        ‹
      </button>
      <button
        v-if="hasNext"
        class="lb-nav lb-next"
        aria-label="下一张"
        @click="next"
      >
        ›
      </button>

      <div
        class="lb-stage"
        :style="{ transform: `translateY(${dragY}px)` }"
      >
        <!-- 照片 -->
        <img
          v-if="item.type === 'photo'"
          :key="item.id"
          :src="item.mediaUrl"
          class="lb-media lb-photo"
          draggable="false"
          @touchstart.passive="onTouchStart"
          @touchmove.passive="onTouchMove"
          @touchend="onTouchEnd"
        >
        <!-- 视频：绝不自动播放，点击才出声 -->
        <video
          v-else
          :key="item.id"
          :src="item.mediaUrl"
          :poster="item.coverUrl || undefined"
          class="lb-media lb-video"
          controls
          playsinline
          preload="metadata"
        />

        <!-- 拍立得信息区：配文 + 日期 + 那天的歌 -->
        <div class="lb-card">
          <div class="lb-caption">{{ item.caption || item.title || '' }}</div>
          <div class="lb-meta">
            <span class="lb-date">{{ displayDate }}</span>
            <div class="lb-actions">
              <button
                v-if="hasSong"
                class="lb-btn lb-song"
                :title="`播放那天的歌：${item.songTitle}`"
                @click="playSong"
              >
                <span class="lb-song-note">♪</span>
                <span class="lb-song-text">{{ item.songTitle }} · {{ item.songArtist }}</span>
              </button>
              <button
                class="lb-btn lb-star"
                :class="{ on: starred }"
                :title="starred ? '取消星星' : '点亮星星'"
                @click="toggleStar"
              >
                {{ starred ? '★' : '☆' }}
                <span v-if="starCount">{{ starCount }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { getMusicDetail } from '@/api/song'
import { toggleMemoryStar } from '@/api/memory'
import { usePlayerStore } from '@/store/player'

const props = defineProps({
  item: { type: Object, required: true },
  starred: { type: Boolean, default: false },
  starCount: { type: Number, default: 0 },
  hasPrev: { type: Boolean, default: false },
  hasNext: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'prev', 'next', 'star'])

const playerStore = usePlayerStore()
const localStarred = ref(props.starred)
const localCount = ref(props.starCount)
const songLoading = ref(false)

const hasSong = computed(() => !!(props.item && props.item.songTitle))

const displayDate = computed(() => {
  const d = props.item?.memoryDate || props.item?.createTime?.slice(0, 10) || ''
  return d ? d.replace(/-/g, '.') : ''
})

const onKey = (e) => {
  if (e.key === 'Escape') close()
  else if (e.key === 'ArrowLeft' && props.hasPrev) prev()
  else if (e.key === 'ArrowRight' && props.hasNext) next()
}
onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))

const close = () => emit('close')
const prev = () => emit('prev')
const next = () => emit('next')

// 移动端下滑关闭
let touchStartY = 0
const dragY = ref(0)
const onTouchStart = (e) => { touchStartY = e.touches[0].clientY }
const onTouchMove = (e) => {
  const dy = e.touches[0].clientY - touchStartY
  if (dy > 0) dragY.value = dy
}
const onTouchEnd = () => {
  if (dragY.value > 90) close()
  dragY.value = 0
}

// 那天的歌：本地拉详情、歌曲海拼免登录流式代理地址，都走全局播放器
const playSong = async () => {
  if (songLoading.value) return
  songLoading.value = true
  try {
    if (props.item.songSource === 'gequhai' && props.item.songExternalId) {
      const apiBase = (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE_URL) || '/api'
      playerStore.play({
        id: `ext:gequhai:${props.item.songExternalId}`,
        title: props.item.songTitle || '那天的歌',
        artists: [{ name: props.item.songArtist || '' }],
        cover: props.item.songCover || '',
        url: `${apiBase}/music/external/stream?source=gequhai&trackId=${props.item.songExternalId}`,
        lyric: '',
        isExternal: true,
        source: 'gequhai',
        sourceTrackId: props.item.songExternalId
      }, null)
      ElMessage.success(`♪ 正在播放：${props.item.songTitle}`)
      return
    }
    const res = await getMusicDetail(props.item.songId)
    const song = res.data
    if (!song || !song.url) {
      ElMessage.warning('这首歌暂时播放不了')
      return
    }
    playerStore.play({
      id: song.id,
      title: song.title,
      artists: (song.artists || []).map(a => ({ name: a.name })),
      cover: song.cover || '',
      url: song.url,
      lyric: song.lyric || ''
    }, null)
    ElMessage.success(`♪ 正在播放：${song.title}`)
  } catch (e) {
    // request 拦截器已提示
  } finally {
    songLoading.value = false
  }
}

const toggleStar = async () => {
  try {
    const res = await toggleMemoryStar(props.item.id)
    localStarred.value = res.data.starred
    localCount.value = res.data.starCount
    emit('star', { id: props.item.id, ...res.data })
  } catch (e) {
    // request 拦截器已提示
  }
}
</script>

<style scoped>
.mem-lightbox {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: rgba(255, 240, 245, 0.92);
  backdrop-filter: blur(14px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.lb-stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  max-width: min(92vw, 900px);
  max-height: 92vh;
  transition: transform 0.18s ease;
  animation: lb-in 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}
@keyframes lb-in {
  from { opacity: 0; transform: scale(0.94) translateY(14px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.lb-media {
  max-width: 100%;
  max-height: 62vh;
  border-radius: 18px;
  box-shadow: 0 24px 60px rgba(201, 24, 74, 0.25);
  background: #fff;
}
.lb-photo { object-fit: contain; }
.lb-video { width: min(92vw, 860px); background: #1a0d12; }

.lb-card {
  background: #fff;
  padding: 16px 20px 14px;
  border-radius: 14px;
  box-shadow: 0 10px 30px rgba(201, 24, 74, 0.14);
  transform: rotate(-0.6deg);
  max-width: 100%;
}
.lb-caption {
  font-family: 'STKaiti', 'KaiTi', 'Noto Serif SC', serif;
  font-size: 16px;
  color: #6d3b47;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.lb-meta {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.lb-date {
  font-size: 12px;
  color: #c9184a;
  letter-spacing: 0.12em;
}
.lb-actions { display: flex; align-items: center; gap: 10px; }
.lb-btn {
  border: none;
  background: #fff0f5;
  color: #c9184a;
  border-radius: 999px;
  padding: 6px 14px;
  font-size: 13px;
  cursor: pointer;
  transition: transform 0.15s, background 0.15s, box-shadow 0.15s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.lb-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 16px rgba(201, 24, 74, 0.18); }
.lb-star.on {
  background: linear-gradient(135deg, #ff8fab, #ff5d8f);
  color: #fff;
}
.lb-song-note { font-size: 15px; }
.lb-song-text { max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.lb-close {
  position: absolute;
  top: 18px; right: 22px;
  width: 40px; height: 40px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  color: #c9184a;
  font-size: 18px;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(201, 24, 74, 0.18);
}
.lb-close:hover { transform: rotate(90deg); }
.lb-close, .lb-nav { transition: transform 0.2s; }

.lb-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 46px; height: 46px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  color: #c9184a;
  font-size: 26px;
  line-height: 1;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(201, 24, 74, 0.18);
}
.lb-nav:hover { background: #fff; }
.lb-prev { left: 20px; }
.lb-next { right: 20px; }

@media (max-width: 680px) {
  .mem-lightbox { padding: 10px; }
  .lb-media { max-height: 54vh; }
  .lb-nav { display: none; }
  .lb-song-text { max-width: 160px; }
}

@media (prefers-reduced-motion: reduce) {
  .lb-stage { animation: none; }
  .lb-close:hover, .lb-nav:hover { transform: none; }
}
</style>
