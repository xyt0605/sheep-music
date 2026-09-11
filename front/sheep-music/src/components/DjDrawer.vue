<template>
  <el-drawer
    v-model="visible"
    direction="rtl"
    size="430px"
    class="dj-drawer"
    :append-to-body="true"
  >
    <template #header>
      <div class="dj-header">
        <span class="dj-header-title">奶包</span>
        <el-tooltip
          :content="isAdmin ? 'AI 连接在 管理后台 → 系统设置 中配置' : 'AI 连接由管理员统一配置'"
          placement="bottom"
        >
          <span class="dj-cfg-badge">{{ cfgLoaded ? (cfg.configured ? 'AI 已就绪' : 'AI 未配置') : '' }}</span>
        </el-tooltip>
      </div>
    </template>
    <div class="dj-body">
      <!-- 消息流 -->
      <div
        ref="listRef"
        class="dj-messages"
      >
        <!-- 待播卡片：DJ 已找到歌，点击开始播放（浏览器需要用户手势） -->
        <div
          v-if="djPending"
          class="dj-pending-card"
        >
          <img
            :src="djPending.cover || defaultCover"
            class="dj-pending-cover"
          >
          <div class="dj-pending-info">
            <div class="dj-pending-title">{{ djPending.title }}</div>
            <div class="dj-pending-artist">{{ djPending.artist }}</div>
          </div>
          <el-button
            type="primary"
            round
            @click="playPending"
          >▶ 播放</el-button>
          <el-button
            text
            circle
            @click="djPending = null"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div
          v-if="messages.length === 0"
          class="dj-empty"
        >
          <div class="dj-empty-icon">🎧</div>
          <p>跟奶包说一句你想听什么</p>
          <p class="dj-empty-hint">比如："来点适合下雨天听的歌"</p>
          <div
            v-if="cfgLoaded && !cfg.configured"
            class="dj-config-tip"
          >
            管理员还没有配置 AI 连接，奶包暂时不能营业
          </div>
        </div>

        <template
          v-for="(msg, mi) in messages"
          :key="mi"
        >
          <!-- 用户消息 -->
          <div
            v-if="msg.role === 'user'"
            class="dj-msg dj-msg-user"
          >
            <div
              v-if="msg.images && msg.images.length"
              class="dj-msg-images"
            >
              <img
                v-for="(img, ii) in msg.images"
                :key="ii"
                :src="img"
                class="dj-msg-thumb"
              >
            </div>
            <span
              v-if="msg.text"
              class="dj-msg-text"
            >{{ msg.text }}</span>
          </div>

          <!-- DJ 消息 -->
          <div
            v-else
            class="dj-msg dj-msg-dj"
          >
            <!-- 过程时间线（可视化小羊驼的工作步骤） -->
            <div
              v-if="msg.steps && msg.steps.length && !msg.done && !msg.error"
              class="dj-timeline"
            >
              <div
                v-for="(st, i) in msg.steps"
                :key="st.key"
                class="dj-step"
                :class="{ active: i === msg.activeStep, done: st.done }"
              >
                <span class="dj-step-dot">
                  <el-icon
                    v-if="i === msg.activeStep"
                    class="is-loading"
                  ><Loading /></el-icon>
                  <el-icon
                    v-else-if="st.done"
                  ><Check />
                  </el-icon>
                  <span
                    v-else
                    class="dj-step-num"
                  >{{ i + 1 }}</span>
                </span>
                <span class="dj-step-label">{{ st.label }}</span>
                <span
                  v-if="st.ms"
                  class="dj-step-ms"
                >{{ st.ms >= 1000 ? (st.ms / 1000).toFixed(1) + 's' : st.ms + 'ms' }}</span>
              </div>
              <div
                v-if="msg.thought"
                class="dj-thought"
              >💭 {{ msg.thought }}</div>
            </div>
            <!-- 兜底：无步骤数据时保留单行状态 -->
            <div
              v-else-if="msg.statusText"
              class="dj-stage"
            >
              <el-icon
                class="is-loading"
                v-if="!msg.done && !msg.error"
              ><Loading /></el-icon>
              {{ msg.statusText }}
            </div>
            <!-- 思考链：推理模型的 reasoning_content 实时流（可折叠，完成后自动收起） -->
            <div
              v-if="msg.reasoning"
              class="dj-reasoning"
            >
              <div
                class="dj-reasoning-head"
                @click="msg.reasoningOpen = !msg.reasoningOpen"
              >
                <el-icon><MagicStick /></el-icon>
                <span>思考过程</span>
                <el-icon
                  class="dj-reasoning-caret"
                  :class="{ open: msg.reasoningOpen }"
                ><ArrowRight /></el-icon>
              </div>
              <pre
                v-show="msg.reasoningOpen"
                class="dj-reasoning-body"
              >{{ msg.reasoning }}</pre>
            </div>
            <div
              v-if="msg.text"
              class="dj-text"
            >{{ msg.text }}</div>
            <div
              v-if="msg.cards.length"
              class="dj-cards"
            >
              <div
                v-for="card in msg.cards"
                :key="card.key"
                class="dj-card"
                @click="playCard(card)"
              >
                <img
                  :src="card.cover || defaultCover"
                  class="dj-card-cover"
                  @error="card.cover = ''"
                >
                <div class="dj-card-info">
                  <div class="dj-card-title">{{ card.title }}</div>
                  <div class="dj-card-artist">{{ card.artist }}</div>
                  <div
                    v-if="card.reason"
                    class="dj-card-reason"
                  >{{ card.reason }}</div>
                  <el-tag
                    size="small"
                    :type="card.source === 'local' ? 'primary' : 'warning'"
                    effect="plain"
                    class="dj-card-tag"
                  >
                    {{ card.source === 'local' ? '本地' : '歌曲海' }}
                  </el-tag>
                </div>
                <el-button
                  icon="Plus"
                  circle
                  size="small"
                  title="加入队列"
                  @click.stop="addCard(card)"
                />
              </div>
            </div>
            <div
              v-if="msg.playlistInfo"
              class="dj-playlist-tip"
            >
              <el-icon><FolderAdd /></el-icon>
              歌单「{{ msg.playlistInfo.name }}」已创建（{{ msg.playlistInfo.count }} 首）
              <el-button
                size="small"
                text
                type="primary"
                @click="goPlaylist(msg.playlistInfo.playlistId)"
              >查看</el-button>
            </div>
            <div
              v-if="msg.cards.length && msg.done"
              class="dj-msg-actions"
            >
              <div class="dj-actions-main">
                <el-button
                  size="small"
                  type="primary"
                  round
                  @click="playAll(msg)"
                >播放整组</el-button>
                <el-button
                  size="small"
                  round
                  @click="queueAll(msg)"
                >全部入队</el-button>
              </div>
              <div
                v-if="!loading"
                class="dj-actions-feedback"
              >
                <el-button
                  size="small"
                  text
                  type="primary"
                  @click="ask('多来点这样的')"
                >多来点这样的</el-button>
                <el-divider direction="vertical" />
                <el-button
                  size="small"
                  text
                  @click="ask('换个口味，来点不一样的')"
                >换个口味</el-button>
              </div>
            </div>
            <div
              v-if="msg.error"
              class="dj-error"
            >
              <span>{{ msg.error }}</span>
              <el-button
                size="small"
                text
                type="primary"
                @click="retry(msg)"
              >重试</el-button>
            </div>
          </div>
        </template>
      </div>


      <!-- 输入区（多模态：文字 + 图片） -->
      <div class="dj-input-wrap">
        <div
          v-if="pendingImages.length"
          class="dj-image-preview"
        >
          <div
            v-for="(img, i) in pendingImages"
            :key="i"
            class="dj-preview-item"
          >
            <img :src="img">
            <button
              class="dj-preview-remove"
              type="button"
              @click="removeImage(i)"
            >✕</button>
          </div>
        </div>
        <div class="dj-input">
          <el-upload
            :show-file-list="false"
            :auto-upload="false"
            accept="image/*"
            :on-change="onPickImage"
          >
            <el-button
              circle
              :disabled="loading"
              title="附一张图片（最多 3 张）"
            >
              <el-icon><Picture /></el-icon>
            </el-button>
          </el-upload>
          <el-input
            v-model="draft"
            placeholder="跟奶包说点想听什么，也可以附图…"
            :disabled="loading"
            @keyup.enter="ask"
          />
          <el-button
            type="primary"
            :loading="loading"
            @click="ask"
          >发送</el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, Check, FolderAdd, Close, MagicStick, ArrowRight, Picture } from '@element-plus/icons-vue'
import { streamDj, getAiConfig } from '@/api/agent'
import { getExternalCover } from '@/api/externalMusic'
import { usePlayerStore } from '@/store/player'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'

const visible = ref(false)
const draft = ref('')
const loading = ref(false)
const sessionId = ref(genSessionId())
const cfgLoaded = ref(false)
const cfg = ref({ configured: false })
// 多模态附图：data URL（前端压缩后 ≤3 张），随下一轮对话发给 DJ
const pendingImages = ref([])
// 待播卡片（play_ref 命令的歌，等用户点击播放——绕开 autoplay 拦截）
const djPending = ref(null)

// ===== 附图：压缩到 1024px JPEG（≈两三百 KB），控制 SSE 请求体体积 =====
const compressImage = (file) => new Promise((resolve, reject) => {
  const reader = new FileReader()
  reader.onload = () => {
    const img = new Image()
    img.onload = () => {
      const MAX = 1024
      const scale = Math.min(1, MAX / Math.max(img.width, img.height))
      const canvas = document.createElement('canvas')
      canvas.width = Math.round(img.width * scale)
      canvas.height = Math.round(img.height * scale)
      canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height)
      resolve(canvas.toDataURL('image/jpeg', 0.82))
    }
    img.onerror = () => reject(new Error('图片解析失败'))
    img.src = reader.result
  }
  reader.onerror = () => reject(new Error('图片读取失败'))
  reader.readAsDataURL(file)
})

const onPickImage = async (file) => {
  if (!file?.raw) return
  if (!file.raw.type || !file.raw.type.startsWith('image/')) {
    ElMessage.error('只能附图片')
    return
  }
  if (pendingImages.value.length >= 3) {
    ElMessage.warning('最多附 3 张图片')
    return
  }
  try {
    pendingImages.value.push(await compressImage(file.raw))
  } catch (e) {
    ElMessage.error('图片处理失败')
  }
}

const removeImage = (i) => {
  pendingImages.value.splice(i, 1)
}

function genSessionId() {
  return (crypto.randomUUID && crypto.randomUUID()) || `dj-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}
const messages = ref([]) // {role:'user'|'dj', text, statusText, cards[], done, error}
const listRef = ref(null)
const defaultCover = '/default-cover.svg'

import { computed, watch } from 'vue'
watch(visible, v => { if (v) loadConfig() })
const router = useRouter()
const playerStore = usePlayerStore()
const userStore = useUserStore()
// P4：AI 连接由管理员统一配置——⚙ 入口与配置对话框仅管理员可见
const isAdmin = computed(() => userStore.isAdmin)

// 过程时间线的步骤定义：key 唯一，label 展示
const STEP_DEFS = [
  { key: 'dispatcher', label: '理解需求' },
  { key: 'search_local', label: '翻本地曲库' },
  { key: 'search_web', label: '逛歌曲海' },
  { key: 'recommend', label: '翻你的口味' },
  { key: 'critic', label: '自我质检' },
  { key: 'dj', label: '组织推荐语' }
]

const ensureStep = (msg, key) => {
  const def = STEP_DEFS.find(d => d.key === key)
  if (!def) return null
  let st = msg.steps.find(x => x.key === key)
  if (!st) {
    st = { key, label: def.label, done: false, ms: 0 }
    msg.steps.push(st)
  }
  return st
}

const markPrevDone = (msg, elapsedMs) => {
  // 新步骤开始时：把上一个未完成步骤标记完成并记录耗时
  msg.steps.forEach((st, i) => {
    if (i === msg.activeStep && !st.done) {
      st.done = true
      st.ms = Math.max(0, (elapsedMs || 0) - (st.startMs || 0))
    }
  })
}

const STAGE_TEXT = {
  dispatcher: '正在理解你的需求…',
  librarian: '正在翻找曲库和歌曲海…',
  critic: '奶包在自我质检…',
  dj: '正在组织语言…'
}

// 思考链分段标签（reasoning_delta 事件按阶段切换时插入小标题）
const REASON_STAGE_LABEL = {
  dispatcher: '理解需求',
  librarian: '翻找曲库',
  critic: '自我质检',
  dj: '组织推荐语',
  player: '解析操作',
  info: '查阅资料',
  playlist: '整理歌单'
}

// 后端 song_card → player 歌曲对象（写操作只认 songId：外源卡只有播放/入队）
// 本地卡用 url、外源卡用 streamUrl（此前统一取 streamUrl 导致本地卡 url 为空、自动切歌卡住）
const toPlayerSong = (card) => ({
  id: card.source === 'local' ? card.songId : `ext:gequhai:${card.sourceTrackId}`,
  title: card.title,
  artists: [{ name: card.artist }],
  cover: card.cover,
  url: card.source === 'local' ? (card.url || '') : (card.streamUrl || ''),
  lyric: '',
  isExternal: !!card.isExternal,
  source: card.source,
  sourceTrackId: card.sourceTrackId
})

const scrollBottom = () => nextTick(() => {
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
})

// 点击待播卡片（用户手势 → 不会被 autoplay 拦截）
const playPending = () => {
  const card = djPending.value
  if (!card) return
  const song = {
    id: card.source === 'local' ? card.songId : `ext:gequhai:${card.sourceTrackId}`,
    title: card.title,
    artists: [{ name: card.artist }],
    cover: card.cover,
    url: card.source === 'local' ? (card.url || '') : (card.streamUrl || ''),
    lyric: '',
    isExternal: !!card.isExternal,
    source: card.source,
    sourceTrackId: card.sourceTrackId
  }
  djPending.value = null
  playerStore.play(song, [song])
}

// ===== 播放控制（能力扩展第一批） =====
const execPlayerCommand = (data) => {
  const cmd = data.command
  try {
    switch (cmd) {
      case 'play': playerStore.resume(); break
      case 'pause': playerStore.pause(); break
      case 'next': playerStore.next(); break
      case 'prev': playerStore.prev(); break
      case 'volume_up': playerStore.setVolume(Math.min(1, playerStore.volume + 0.15)); break
      case 'volume_down': playerStore.setVolume(Math.max(0, playerStore.volume - 0.15)); break
      case 'mode_list': playerStore.setPlayMode('list'); break
      case 'mode_random': playerStore.setPlayMode('random'); break
      case 'mode_single': playerStore.setPlayMode('single'); break
      case 'queue_clear': playerStore.playlist.splice(0, playerStore.playlist.length); playerStore.pause(); break
      case 'play_ref': {
        // 不直接播放：SSE 事件无用户手势，audio.play() 会被浏览器 autoplay 策略拒绝
        // （表现为切歌成功但停在 00:00）。改为把歌挂到当前消息的“待播卡片”，用户点击即播。
        const card = data.song
        if (card) {
          djPending.value = card
        }
        break
      }
      default: console.warn('未知播放命令', cmd)
    }
  } catch (e) {
    console.error('播放命令执行失败', e)
  }
}

// 外源卡片封面异步回填（与搜索页同款逻辑）
const fillCardCover = (card) => {
  if (card.isExternal && !card.cover && card.sourceTrackId) {
    getExternalCover({ source: 'gequhai', trackId: card.sourceTrackId })
      .then(res => { if (res.code === 200 && res.data) card.cover = res.data })
      .catch(() => {})
  }
}

// 重试：找到该 DJ 消息对应的用户请求重发
const retry = (djMsg) => {
  const idx = messages.value.indexOf(djMsg)
  for (let i = idx - 1; i >= 0; i--) {
    if (messages.value[i].role === 'user') {
      draft.value = messages.value[i].text
      pendingImages.value = (messages.value[i].images || []).slice()
      ask()
      return
    }
  }
}

const ask = async () => {
  const query = draft.value.trim()
  if ((!query && !pendingImages.value.length) || loading.value) return
  const images = pendingImages.value.slice()
  draft.value = ''
  pendingImages.value = []
  loading.value = true
  messages.value.push({ role: 'user', text: query, images })

  const dj = { role: 'dj', text: '', statusText: STAGE_TEXT.dispatcher, cards: [], done: false, error: '', steps: [], activeStep: -1, thought: '', playlistInfo: null, reasoning: '', reasoningOpen: true, _reasonStage: '' }
  // 请求一发出就显示第一步（不等后端事件——LLM 排队也能看到时间线）
  ensureStep(dj, 'dispatcher')
  dj.activeStep = 0
  messages.value.push(dj)
  scrollBottom()

  try {
    await streamDj(query, sessionId.value, {
      images,
      onEvent: (event, data) => {
        if (event === 'session' && data.sessionId) {
          sessionId.value = data.sessionId  // 服务端归一化后的会话 ID（P2 会话记忆）
        }
        if (event === 'stage') {
          dj.statusText = STAGE_TEXT[data.stage] || dj.statusText
          if (data.stage === 'librarian' && data.round > 1) {
            dj.statusText = `奶包在自我改进（第 ${data.round} 轮检索）…`
          }
          // 时间线：新阶段开始 → 结算上一步、点亮当前步
          if (data.status === 'start') {
            markPrevDone(dj, data.elapsedMs)
            const st = ensureStep(dj, data.stage === 'librarian' ? 'search_local' : data.stage)
            if (st) {
              st.startMs = data.elapsedMs || 0
              dj.activeStep = dj.steps.indexOf(st)
            }
          } else if (data.status === 'done') {
            dj.steps.forEach(st => {
              if (dj.steps.indexOf(st) <= dj.activeStep && !st.done) {
                st.done = true
                st.ms = Math.max(0, (data.elapsedMs || 0) - (st.startMs || 0))
              }
            })
          }
        } else if (event === 'thought') {
          dj.thought = data.text || ''
        } else if (event === 'reasoning_delta') {
          // 模型思考链实时展示（推理模型的 reasoning_content）；按阶段切换插入小标题
          const stage = data.stage || ''
          if (stage && stage !== dj._reasonStage) {
            const label = REASON_STAGE_LABEL[stage] || stage
            dj.reasoning += (dj.reasoning ? '\n\n' : '') + `【${label}】\n`
            dj._reasonStage = stage
          }
          dj.reasoning += data.delta || ''
        } else if (event === 'action') {
          const argText = data.args?.keyword ? `“${data.args.keyword}”` : ''
          const st = ensureStep(dj, data.action === 'finish' ? 'critic' : data.action)
          if (st) {
            // 先结算之前的步骤（elapsed 用 stage 事件锚点，这里没有就跳过）
            dj.steps.forEach(x => {
              if (dj.steps.indexOf(x) < dj.steps.indexOf(st) && !x.done) x.done = true
            })
            if (!st.startMs) st.startMs = dj.steps.filter(x => x.done).reduce((a, b) => a + (b.ms || 0), 0)
            dj.activeStep = dj.steps.indexOf(st)
          }
          const actionText = { search_local: '翻本地曲库', search_web: '逛歌曲海', recommend: '翻你的口味', finish: '整理候选' }[data.action] || data.action
          dj.statusText = `${actionText}${argText}…`
        } else if (event === 'text_delta') {
          dj.statusText = ''
          dj.steps.forEach(st => { st.done = true })
          dj.activeStep = -1
          dj.text += data.delta || ''
        } else if (event === 'player_command') {
          execPlayerCommand(data)
        } else if (event === 'playlist_created') {
          dj.playlistInfo = data
          ElMessage.success(`歌单「${data.name}」已创建（${data.count} 首）`)
        } else if (event === 'song_card') {
          const card = {
            source: data.source,
            songId: data.songId,
            sourceTrackId: data.sourceTrackId,
            title: data.title,
            artist: data.artist,
            cover: data.cover || '',
            url: data.url,
            streamUrl: data.streamUrl,
            reason: data.reason || '',
            isExternal: !!data.isExternal,
            key: `${data.source}-${data.songId || data.sourceTrackId}`
          }
          dj.cards.push(card)
          fillCardCover(card)
        } else if (event === 'error') {
          dj.error = data.message || 'DJ 开小差了，稍后再试'
          if (data.message && /还没有配置/.test(data.message)) {
            loadConfig()
          }
        } else if (event === 'done') {
          dj.done = true
          dj.statusText = ''
          dj.steps.forEach(st => { st.done = true })
          dj.activeStep = -1
          dj.reasoningOpen = false // 完成后自动收起思考链，突出正文/卡片（可点开回看）
          if (data.elapsedMs) {
            dj.textDoneMs = data.elapsedMs
          }
        }
        scrollBottom()
      }
    })
  } catch (e) {
    dj.error = e?.message || '连接失败，请稍后再试'
  } finally {
    dj.done = true
    dj.statusText = ''
    loading.value = false
    scrollBottom()
    loadConfig() // 每轮结束刷新 AI 状态徽标（管理员可能在系统设置里刚改配置）
  }
}

// ===== AI 连接状态徽标（配置唯一入口：管理后台 → 系统设置） =====
const loadConfig = async () => {
  try {
    const res = await getAiConfig()
    if (res.code === 200) {
      cfg.value = res.data || {}
      cfgLoaded.value = true
    }
  } catch (e) {
    console.error('加载 AI 配置失败:', e)
  }
}

const goPlaylist = (id) => {
  if (id) {
    visible.value = false
    router.push(`/playlist/${id}`)
  }
}

const playCard = (card) => {
  const song = toPlayerSong(card)
  playerStore.play(song, [song])
}

const addCard = (card) => {
  playerStore.addToPlaylist(toPlayerSong(card))
  ElMessage.success(`已加入队列: ${card.title}`)
}

const playAll = (msg) => {
  const songs = msg.cards.map(toPlayerSong)
  if (songs.length) playerStore.play(songs[0], songs)
}

const queueAll = (msg) => {
  msg.cards.map(toPlayerSong).forEach(s => playerStore.addToPlaylist(s))
  ElMessage.success(`已把 ${msg.cards.length} 首加入队列`)
}

defineExpose({ open: () => { visible.value = true } })
</script>

<style scoped>
.dj-body {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.dj-messages {
  flex: 1;
  overflow-y: auto;
  padding: 4px 4px 12px;
}

.dj-empty {
  text-align: center;
  color: var(--text-secondary);
  padding: 60px 10px;
}

.dj-empty-icon {
  font-size: 40px;
  margin-bottom: 10px;
}

.dj-empty-hint {
  font-size: 13px;
  color: var(--text-tertiary);
}

.dj-msg {
  margin-bottom: 16px;
  border-radius: var(--radius-lg);
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
}

.dj-msg-user {
  background: var(--color-primary);
  color: var(--text-inverse);
  margin-left: 48px;
  white-space: pre-wrap;
}

.dj-msg-dj {
  background: var(--surface-raised);
  border: 1px solid var(--border-color-light);
  margin-right: 24px;
}

.dj-stage {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-tertiary);
  font-size: 13px;
  margin-bottom: 6px;
}

.dj-text {
  color: var(--text-primary);
  white-space: pre-wrap;
}

/* 思考链（reasoning_content 实时流） */
.dj-reasoning {
  margin: 6px 0 10px;
  border: 1px dashed var(--border-color, #d9d9d9);
  border-radius: 8px;
  background: var(--bg-tertiary, rgba(127, 127, 127, 0.06));
  overflow: hidden;
}

.dj-reasoning-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  cursor: pointer;
  font-size: 12px;
  color: var(--text-secondary, #888);
  user-select: none;
}

.dj-reasoning-head:hover {
  color: var(--color-primary, #667eea);
}

.dj-reasoning-caret {
  margin-left: auto;
  transition: transform 0.2s ease;
}

.dj-reasoning-caret.open {
  transform: rotate(90deg);
}

.dj-reasoning-body {
  margin: 0;
  padding: 8px 12px;
  max-height: 220px;
  overflow-y: auto;
  font-family: inherit;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-secondary, #999);
  white-space: pre-wrap;
  word-break: break-word;
  border-top: 1px dashed var(--border-color, #eee);
}

.dj-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}

.dj-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border: 1px solid var(--border-color-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.dj-card:hover {
  border-color: var(--border-strong);
  background: var(--card-hover-bg);
}

.dj-card-cover {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  object-fit: cover;
  flex-shrink: 0;
}

.dj-card-info {
  flex: 1;
  min-width: 0;
}

.dj-card-title {
  font-size: 14px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dj-card-artist {
  font-size: 12px;
  color: var(--text-tertiary);
  margin: 2px 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dj-card-tag {
  transform: scale(0.9);
  transform-origin: left;
}

.dj-msg-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--border-color-light);
  flex-wrap: wrap;
}

.dj-actions-main {
  display: flex;
  gap: 8px;
}

/* 反馈簇：与主操作同行右对齐；窄宽度下整体换行不挤压 */
.dj-actions-feedback {
  display: flex;
  align-items: center;
  gap: 0;
  margin-left: auto;
}

.dj-actions-feedback .el-button {
  padding: 4px 8px;
}

.dj-actions-feedback .el-divider {
  margin: 0 2px;
  height: 14px;
}

.dj-error {
  color: var(--color-accent);
  font-size: 13px;
  margin-top: 6px;
}

.dj-input-wrap {
  border-top: 1px solid var(--border-color-light);
  padding-top: 10px;
}

.dj-image-preview {
  display: flex;
  gap: 8px;
  padding: 0 4px 8px;
  flex-wrap: wrap;
}

.dj-preview-item {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border-color-light);
}

.dj-preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.dj-preview-remove {
  position: absolute;
  top: 1px;
  right: 1px;
  width: 16px;
  height: 16px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 9px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dj-input {
  display: flex;
  gap: 8px;
  padding: 0 4px 4px;
  align-items: center;
}

.dj-input :deep(.el-upload) {
  display: inline-flex;
}

.dj-msg-images {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.dj-msg-thumb {
  max-width: 132px;
  max-height: 132px;
  border-radius: 8px;
  object-fit: cover;
  display: block;
}

.dj-msg-text {
  white-space: pre-wrap;
  word-break: break-word;
}

/* 抽屉本体融入主题 */
:global(.dj-drawer) {
  background: var(--bg-secondary) !important;
}

:global(.dj-drawer .el-drawer__header) {
  margin-bottom: 10px;
  color: var(--text-primary);
  font-weight: 600;
}

:global(.dj-drawer .el-drawer__body) {
  padding: 0 14px 10px;
}
</style>

.dj-card-reason {
  font-size: 12px;
  color: var(--color-primary-dark);
  background: rgba(201, 255, 69, 0.12);
  border-radius: var(--radius-sm);
  padding: 1px 6px;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}


.dj-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.dj-header-title {
  font-weight: 600;
  color: var(--text-primary);
}

.dj-config-tip {
  margin-top: 14px;
  padding: 10px 12px;
  border: 1px solid var(--border-color-light);
  border-radius: var(--radius-lg);
  color: var(--color-accent);
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.dj-cfg-badge {
  font-size: 11px;
  color: var(--text-tertiary);
  padding: 2px 8px;
  border: 1px solid var(--border-color-light);
  border-radius: 999px;
}

.dj-cfg-status {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 10px;
}

.dj-cfg-test {
  margin-top: 4px;
  color: var(--text-primary);
}

.dj-cfg-hint {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 4px;
  line-height: 1.4;
}

/* ========== 过程时间线 ========== */
.dj-timeline {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 6px;
}

.dj-step {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-tertiary);
  padding: 1px 0;
  transition: color 0.2s;
}

.dj-step.active {
  color: var(--color-primary-light);
}

.dj-step.done {
  color: var(--text-tertiary);
}

.dj-step-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-color);
  flex-shrink: 0;
  font-size: 11px;
}

.dj-step.active .dj-step-dot {
  border-color: var(--color-primary);
}

.dj-step.done .dj-step-dot {
  border-color: var(--border-color-light);
  color: var(--color-primary-light);
}

.dj-step-num {
  font-size: 10px;
  line-height: 1;
}

.dj-step-label {
  flex: 1;
}

.dj-step-ms {
  font-size: 11px;
  color: var(--text-tertiary);
  font-variant-numeric: tabular-nums;
}

.dj-thought {
  font-size: 12px;
  font-style: italic;
  color: var(--text-tertiary);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  padding: 4px 8px;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.dj-playlist-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-primary-dark);
  background: rgba(201, 255, 69, 0.12);
  border-radius: var(--radius-md);
  padding: 6px 10px;
  margin-top: 10px;
}

.dj-pending-card {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--surface-elevated);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-lg);
  padding: 10px 12px;
  margin-bottom: 14px;
  box-shadow: var(--shadow-glow);
}

.dj-pending-cover {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  object-fit: cover;
  flex-shrink: 0;
}

.dj-pending-info {
  flex: 1;
  min-width: 0;
}

.dj-pending-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.dj-pending-artist {
  font-size: 12px;
  color: var(--text-tertiary);
}
