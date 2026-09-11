<template>
  <Teleport to="body">
    <div
      class="hug-overlay"
      @click="close"
    >
      <div class="hug-glow" />
      <div class="hug-core">
        <div class="hug-emoji">
          🫂
        </div>
        <p class="hug-line1">
          抱紧啦
        </p>
        <p class="hug-line2">
          奶包一直都在，他也是
        </p>
        <p
          v-if="song"
          class="hug-song"
        >
          ♪ {{ song.songTitle }}{{ song.songArtist ? ' · ' + song.songArtist : '' }} 已为你响起
        </p>
        <p
          v-else
          class="hug-song hug-song-none"
        >
          已经告诉他了，他很快就会来找你
        </p>
        <p class="hug-tap">
          轻触任意处继续
        </p>
      </div>
      <span
        v-for="h in hearts"
        :key="h.id"
        class="hug-heart"
        :style="h.style"
      >♥</span>
    </div>
  </Teleport>
</template>

<script setup>
import { onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  song: { type: Object, default: null } // {songSource, songId, songExternalId, songTitle, songArtist}
})
const emit = defineEmits(['close'])

const close = () => emit('close')

const hearts = []
for (let i = 0; i < 10; i++) {
  hearts.push({
    id: i,
    style: {
      left: `${6 + Math.random() * 88}%`,
      animationDuration: `${5 + Math.random() * 4}s`,
      animationDelay: `${Math.random() * 4}s`,
      fontSize: `${12 + Math.random() * 16}px`,
      opacity: 0.25 + Math.random() * 0.35
    }
  })
}

// 自动放安全歌（外部传入已含歌曲信息；播放管道与灯箱 ♪ 一致）
const playSong = async () => {
  if (!props.song) return
  try {
    const { usePlayerStore } = await import('@/store/player')
    const playerStore = usePlayerStore()
    const apiBase = (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE_URL) || '/api'
    if (props.song.songSource === 'gequhai' && props.song.songExternalId) {
      playerStore.play({
        id: `ext:gequhai:${props.song.songExternalId}`,
        title: props.song.songTitle,
        artists: [{ name: props.song.songArtist || '' }],
        cover: props.song.songCover || '',
        url: `${apiBase}/music/external/stream?source=gequhai&trackId=${props.song.songExternalId}`,
        lyric: '',
        isExternal: true,
        source: 'gequhai',
        sourceTrackId: props.song.songExternalId
      }, null)
    } else if (props.song.songId) {
      const { getMusicDetail } = await import('@/api/song')
      const res = await getMusicDetail(props.song.songId)
      const song = res.data
      if (!song || !song.url) return
      playerStore.play({
        id: song.id,
        title: song.title,
        artists: (song.artists || []).map(a => ({ name: a.name })),
        cover: song.cover || '',
        url: song.url,
        lyric: song.lyric || ''
      }, null)
    }
  } catch (e) {
    // 放歌失败不阻断拥抱动画
  }
}

let escHandler
onMounted(() => {
  playSong()
  escHandler = (e) => { if (e.key === 'Escape') close() }
  window.addEventListener('keydown', escHandler)
})
onBeforeUnmount(() => window.removeEventListener('keydown', escHandler))
</script>

<style scoped>
.hug-overlay {
  position: fixed;
  inset: 0;
  z-index: 3400;
  background: linear-gradient(160deg, #fff0f3 0%, #ffe0e8 55%, #ffd3de 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  animation: hug-in 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}
@keyframes hug-in {
  from { opacity: 0; transform: scale(1.04); }
  to { opacity: 1; transform: scale(1); }
}

.hug-glow {
  position: absolute;
  width: 70vmin;
  height: 70vmin;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 143, 171, 0.35) 0%, rgba(255, 143, 171, 0) 65%);
  animation: glow-breathe 3s ease-in-out infinite;
}
@keyframes glow-breathe {
  0%, 100% { transform: scale(1); opacity: 0.9; }
  50% { transform: scale(1.12); opacity: 1; }
}

.hug-core {
  position: relative;
  text-align: center;
  z-index: 1;
}
.hug-emoji {
  font-size: 96px;
  line-height: 1;
  animation: hug-bounce 2.2s ease-in-out infinite;
  filter: drop-shadow(0 12px 30px rgba(201, 24, 74, 0.25));
}
@keyframes hug-bounce {
  0%, 100% { transform: scale(1) rotate(-2deg); }
  50% { transform: scale(1.08) rotate(2deg); }
}
.hug-line1 {
  margin-top: 26px;
  font-family: 'STKaiti', 'KaiTi', 'Noto Serif SC', serif;
  font-size: 34px;
  color: #c9184a;
  letter-spacing: 0.14em;
}
.hug-line2 {
  margin-top: 12px;
  font-size: 15px;
  color: #6d3b47;
}
.hug-song {
  margin-top: 18px;
  font-size: 13.5px;
  color: #a2637a;
  max-width: 80vw;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hug-song-none { color: #b78a97; }
.hug-tap {
  margin-top: 34px;
  font-size: 11px;
  color: rgba(162, 99, 122, 0.55);
  letter-spacing: 0.3em;
}

.hug-heart {
  position: absolute;
  bottom: -30px;
  color: #ff8fab;
  animation: heart-rise linear infinite;
  pointer-events: none;
}
@keyframes heart-rise {
  0% { transform: translateY(0) rotate(0deg); opacity: 0; }
  12% { opacity: var(--heart-opacity, 0.4); }
  100% { transform: translateY(-108vh) rotate(24deg); opacity: 0; }
}

@media (prefers-reduced-motion: reduce) {
  .hug-emoji, .hug-glow, .hug-heart { animation: none; }
}
</style>
