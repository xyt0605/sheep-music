<template>
  <Teleport to="body">
    <div
      class="hug-overlay"
      @click="close"
    >
      <div class="hug-glow" />

      <!-- 奶包本包：举着猫爪求抱抱的白色长毛猫（按本猫照片绘制） -->
      <div class="hug-alpaca">
        <svg
          viewBox="0 0 200 200"
          class="alpaca-svg"
        >
          <!-- 耳朵（长毛猫小三角耳） -->
          <path
            d="M58 62 L66 24 Q67 18 73 23 L98 45 Z"
            fill="#fffdfa"
            stroke="#ece0d8"
            stroke-width="2.5"
            stroke-linejoin="round"
          />
          <path
            d="M142 62 L134 24 Q133 18 127 23 L102 45 Z"
            fill="#fffdfa"
            stroke="#ece0d8"
            stroke-width="2.5"
            stroke-linejoin="round"
          />
          <path
            d="M66 54 L70 36 L84 47 Z"
            fill="#ffd9e2"
          />
          <path
            d="M134 54 L130 36 L116 47 Z"
            fill="#ffd9e2"
          />

          <!-- 举起来的猫爪 -->
          <g class="alpaca-arms">
            <circle
              cx="44"
              cy="148"
              r="12"
              fill="#fffdfa"
              stroke="#ece0d8"
              stroke-width="2.5"
            />
            <path
              d="M40 142 L40 152 M48 142 L48 152"
              stroke="#f3d8d0"
              stroke-width="2"
              stroke-linecap="round"
            />
            <circle
              cx="156"
              cy="148"
              r="12"
              fill="#fffdfa"
              stroke="#ece0d8"
              stroke-width="2.5"
            />
            <path
              d="M152 142 L152 152 M160 142 L160 152"
              stroke="#f3d8d0"
              stroke-width="2"
              stroke-linecap="round"
            />
          </g>

          <!-- 圆滚滚的毛脸（长毛蓬松感：头顶毛毛球） -->
          <ellipse
            cx="100"
            cy="118"
            rx="60"
            ry="54"
            fill="#fffdfa"
            stroke="#ece0d8"
            stroke-width="2.5"
          />
          <circle
            cx="76"
            cy="72"
            r="16"
            fill="#fffdfa"
          />
          <circle
            cx="100"
            cy="66"
            r="18"
            fill="#fffdfa"
          />
          <circle
            cx="124"
            cy="72"
            r="16"
            fill="#fffdfa"
          />

          <!-- 大眼睛（照片同款绿褐色） -->
          <g>
            <circle
              cx="70"
              cy="106"
              r="12.5"
              fill="#8fae5e"
              stroke="#4a3f35"
              stroke-width="2.5"
            />
            <circle
              cx="70"
              cy="106"
              r="6"
              fill="#241d1a"
            />
            <circle
              cx="66.5"
              cy="101.5"
              r="3.2"
              fill="#ffffff"
            />
            <circle
              cx="73.5"
              cy="109"
              r="1.5"
              fill="#ffffff"
              opacity="0.9"
            />
          </g>
          <g>
            <circle
              cx="130"
              cy="106"
              r="12.5"
              fill="#8fae5e"
              stroke="#4a3f35"
              stroke-width="2.5"
            />
            <circle
              cx="130"
              cy="106"
              r="6"
              fill="#241d1a"
            />
            <circle
              cx="126.5"
              cy="101.5"
              r="3.2"
              fill="#ffffff"
            />
            <circle
              cx="133.5"
              cy="109"
              r="1.5"
              fill="#ffffff"
              opacity="0.9"
            />
          </g>

          <!-- 腮红 -->
          <ellipse
            cx="50"
            cy="126"
            rx="9"
            ry="5.5"
            fill="#ffc9d9"
            opacity="0.8"
          />
          <ellipse
            cx="150"
            cy="126"
            rx="9"
            ry="5.5"
            fill="#ffc9d9"
            opacity="0.8"
          />

          <!-- 粉鼻子 + ω 嘴 -->
          <path
            d="M93 123 L107 123 Q109 125 107 128 L101 132 Q100 133 99 132 L93 128 Q91 125 93 123 Z"
            fill="#e8a2a2"
          />
          <path
            d="M100 132 L100 137 M100 137 Q95 142 91 137 M100 137 Q105 142 109 137"
            stroke="#4a3f35"
            stroke-width="2.5"
            stroke-linecap="round"
            fill="none"
          />

          <!-- 长胡须（本猫招牌） -->
          <g
            stroke="#d9c9c0"
            stroke-width="2"
            stroke-linecap="round"
          >
            <path d="M42 110 L12 104" />
            <path d="M42 120 L10 122" />
            <path d="M42 130 L14 140" />
            <path d="M158 110 L188 104" />
            <path d="M158 120 L190 122" />
            <path d="M158 130 L186 140" />
          </g>

          <!-- 头顶小爱心（心跳） -->
          <path
            class="alpaca-heart"
            d="M152 40 c-3-4-9-1-9 3 c0 4 9 9 9 9 s9-5 9-9 c0-4-6-7-9-3z"
            fill="#ff5d8f"
          />
        </svg>
      </div>

      <div class="hug-core">
        <p class="hug-line1">
          呼—— 抱紧啦
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

      <!-- 出场爆开的小心心 -->
      <span
        v-for="h in burstHearts"
        :key="'b' + h.id"
        class="hug-burst-heart"
        :style="h.style"
      >♥</span>
      <!-- 持续上浮的爱心 -->
      <span
        v-for="h in floatHearts"
        :key="'f' + h.id"
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

// 出场从奶包怀里爆开的爱心（随机方向飞散）
const burstHearts = Array.from({ length: 9 }, (_, i) => {
  const angle = (i / 9) * Math.PI * 2 + Math.random() * 0.5
  const dist = 90 + Math.random() * 90
  return {
    id: i,
    style: {
      '--tx': `${Math.cos(angle) * dist}px`,
      '--ty': `${Math.sin(angle) * dist - 40}px`,
      left: '50%',
      top: '42%',
      fontSize: `${13 + Math.random() * 10}px`,
      animationDelay: `${0.25 + Math.random() * 0.2}s`,
      color: Math.random() < 0.5 ? '#ff5d8f' : '#ffb3c6'
    }
  }
})

// 持续上浮的爱心
const floatHearts = Array.from({ length: 10 }, (_, i) => ({
  id: i,
  style: {
    left: `${6 + Math.random() * 88}%`,
    animationDuration: `${5 + Math.random() * 4}s`,
    animationDelay: `${Math.random() * 4}s`,
    fontSize: `${12 + Math.random() * 14}px`,
    opacity: 0.25 + Math.random() * 0.35
  }
}))

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

.hug-alpaca {
  position: absolute;
  width: min(46vmin, 300px);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -62%);
  animation: alpaca-squish 2.4s ease-in-out infinite;
  filter: drop-shadow(0 14px 30px rgba(201, 24, 74, 0.18));
}
@keyframes alpaca-squish {
  0%, 100% { transform: translate(-50%, -62%) scale(1, 1); }
  50% { transform: translate(-50%, -60%) scale(1.04, 0.96); }
}
.alpaca-svg { width: 100%; height: auto; display: block; }

.alpaca-arms {
  transform-origin: 100px 130px;
  animation: arms-wave 2.4s ease-in-out infinite;
}
@keyframes arms-wave {
  0%, 100% { transform: rotate(0deg); }
  50% { transform: rotate(-6deg); }
}
.alpaca-heart {
  animation: heart-beat 1.6s ease-in-out infinite;
  transform-origin: 150px 50px;
}
@keyframes heart-beat {
  0%, 100% { transform: scale(1); }
  18% { transform: scale(1.25); }
  36% { transform: scale(1); }
}

.hug-core {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, 18%);
  text-align: center;
  z-index: 1;
  width: 100%;
}
.hug-line1 {
  font-family: 'STKaiti', 'KaiTi', 'Noto Serif SC', serif;
  font-size: 32px;
  color: #c9184a;
  letter-spacing: 0.12em;
  animation: line-pop 0.55s cubic-bezier(0.34, 1.56, 0.64, 1) 0.15s backwards;
}
.hug-line2 {
  margin-top: 12px;
  font-size: 15px;
  color: #6d3b47;
  animation: line-pop 0.55s cubic-bezier(0.34, 1.56, 0.64, 1) 0.4s backwards;
}
.hug-song {
  margin-top: 16px;
  font-size: 13.5px;
  color: #a2637a;
  max-width: 80vw;
  margin-left: auto;
  margin-right: auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  animation: line-pop 0.55s cubic-bezier(0.34, 1.56, 0.64, 1) 0.65s backwards;
}
.hug-song-none { color: #b78a97; }
.hug-tap {
  margin-top: 30px;
  font-size: 11px;
  color: rgba(162, 99, 122, 0.55);
  letter-spacing: 0.3em;
  animation: line-pop 0.55s ease 0.95s backwards;
}
@keyframes line-pop {
  from { opacity: 0; transform: translateY(14px) scale(0.92); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.hug-burst-heart {
  position: absolute;
  opacity: 0;
  animation: heart-burst 1.3s cubic-bezier(0.22, 1, 0.36, 1) forwards;
  pointer-events: none;
}
@keyframes heart-burst {
  0% { transform: translate(-50%, -50%) scale(0.3); opacity: 0; }
  25% { opacity: 1; }
  100% { transform: translate(calc(-50% + var(--tx)), calc(-50% + var(--ty))) scale(1.15) rotate(18deg); opacity: 0; }
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
  .hug-alpaca, .alpaca-arms, .alpaca-heart, .hug-glow,
  .hug-burst-heart, .hug-heart, .hug-line1, .hug-line2, .hug-song, .hug-tap { animation: none; }
}
</style>
