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
        <span class="dj-header-title">小屋 DJ</span>
        <el-button
          size="small"
          text
          @click="openSettings"
        >
          <el-icon><Setting /></el-icon>
          AI 连接
        </el-button>
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
          <p>跟 DJ 说一句你想听什么</p>
          <p class="dj-empty-hint">比如："来点适合下雨天听的歌"</p>
          <div
            v-if="cfgLoaded && !cfg.configured"
            class="dj-config-tip"
          >
            当前用户还没有配置密钥，无法使用
            <el-button
              size="small"
              type="primary"
              @click="openSettings"
            >去配置</el-button>
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
            {{ msg.text }}
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

      <!-- AI 连接设置对话框（P3 BYOK 可视化配置板块） -->
      <el-dialog
        v-model="cfgVisible"
        title="AI 连接设置"
        width="420px"
        append-to-body
      >
        <el-form label-position="top">
          <el-form-item label="厂商">
            <el-select
              v-model="providerKey"
              style="width: 100%"
              @change="applyPreset"
            >
              <el-option
                v-for="p in PROVIDER_PRESETS"
                :key="p.key"
                :label="p.label"
                :value="p.key"
              />
            </el-select>
            <div
              v-if="activePreset?.hint"
              class="dj-cfg-hint"
            >{{ activePreset.hint }}</div>
          </el-form-item>
          <el-form-item
            label="API Key"
            required
          >
            <el-input
              v-model="cfgForm.apiKey"
              type="password"
              show-password
              :placeholder="cfg.apiKeyMasked || '该厂商的 API Key'"
            />
          </el-form-item>
          <el-form-item label="接口地址">
            <el-input
              v-model="cfgForm.baseUrl"
              :placeholder="activePreset?.baseUrl || cfg.defaultBaseUrl"
            />
          </el-form-item>
          <el-form-item>
            <template #label>
              <span>模型</span>
              <el-button
                size="small"
                text
                type="primary"
                :loading="fetchingModels"
                :disabled="!cfgForm.baseUrl"
                style="margin-left: 8px"
                @click="fetchModels"
              >获取实时列表</el-button>
            </template>
            <el-select
              v-model="cfgForm.model"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入模型名"
              style="width: 100%"
            >
              <el-option
                v-for="m in modelOptions"
                :key="m"
                :label="m"
                :value="m"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <div class="dj-cfg-status">
          <template v-if="cfg.configured">
            已配置：{{ cfg.model }} @ {{ cfg.baseUrl }}（Key: {{ cfg.apiKeyMasked }}）
          </template>
          <template v-else>
            未配置
          </template>
          <div
            v-if="testResult"
            class="dj-cfg-test"
          >{{ testResult }}</div>
        </div>
        <template #footer>
          <el-button
            size="small"
            type="danger"
            text
            @click="clearCfg"
          >清除配置</el-button>
          <el-button
            size="small"
            :loading="testing"
            @click="testCfg"
          >测试连接</el-button>
          <el-button
            type="primary"
            size="small"
            :loading="saving"
            @click="saveCfg"
          >保存</el-button>
        </template>
      </el-dialog>

      <!-- 输入区 -->
      <div class="dj-input">
        <el-input
          v-model="draft"
          placeholder="跟 DJ 说点想听什么…"
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
  </el-drawer>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, Setting, Check, FolderAdd, Close } from '@element-plus/icons-vue'
import { streamDj, getAiConfig, saveAiConfig, clearAiConfig, testAiConfig, getModelList } from '@/api/agent'
import { getExternalCover } from '@/api/externalMusic'
import { usePlayerStore } from '@/store/player'
import { useRouter } from 'vue-router'

const visible = ref(false)
const draft = ref('')
const loading = ref(false)
const sessionId = ref(genSessionId())
const cfgVisible = ref(false)
const cfgLoaded = ref(false)
const saving = ref(false)
const testing = ref(false)
const testResult = ref('')
const cfg = ref({ configured: false, defaultBaseUrl: '', defaultModel: '' })
const cfgForm = ref({ apiKey: '', baseUrl: '', model: '' })
const fetchedModels = ref([])
// 待播卡片（play_ref 命令的歌，等用户点击播放——绕开 autoplay 拦截）
const djPending = ref(null)
const fetchingModels = ref(false)

// 模型下拉选项 = 厂商预设 + 实时拉取（去重）
const modelOptions = computed(() => {
  const merged = [...(activePreset.value?.models || []), ...fetchedModels.value]
  return [...new Set(merged)]
})

// 厂商预设：任何 OpenAI 兼容端点均可（选"自定义"后手填）
const PROVIDER_PRESETS = [
  { key: 'zhipu', label: '智谱 BigModel', baseUrl: 'https://open.bigmodel.cn/api/paas/v4', models: ['glm-4.6', 'glm-4.5-air', 'glm-4.5-flash', 'glm-4-plus'], hint: 'glm-4.6 为当前旗舰；flash 免费但当前响应慢' },
  { key: 'deepseek', label: 'DeepSeek', baseUrl: 'https://api.deepseek.com', models: ['deepseek-chat', 'deepseek-reasoner'], hint: '两个别名自动跟随 DeepSeek 最新版；reasoner 思考耗大量 token' },
  { key: 'moonshot', label: '月之暗面 Kimi', baseUrl: 'https://api.moonshot.cn/v1', models: ['kimi-latest', 'kimi-k2-turbo-preview', 'kimi-k2-0905-preview', 'moonshot-v1-8k'] },
  { key: 'qwen', label: '阿里通义千问', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', models: ['qwen3-max', 'qwen-plus-latest', 'qwen-turbo-latest', 'qwen-max'] },
  { key: 'doubao', label: '字节豆包（火山方舟）', baseUrl: 'https://ark.cn-beijing.volces.com/api/v3', models: ['（填你的接入点 ID）'], hint: '模型填方舟控制台的接入点 ID' },
  { key: 'siliconflow', label: '硅基流动（聚合）', baseUrl: 'https://api.siliconflow.cn/v1', models: ['deepseek-ai/DeepSeek-V3.1', 'Qwen/Qwen3-235B-A22B', 'moonshotai/Kimi-K2-Instruct'] },
  { key: 'openai', label: 'OpenAI', baseUrl: 'https://api.openai.com/v1', models: ['gpt-5', 'gpt-5-mini', 'gpt-4.1', 'gpt-4o'] },
  { key: 'gemini', label: 'Google Gemini', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', models: ['gemini-2.5-pro', 'gemini-2.5-flash', 'gemini-2.0-flash'] },
  { key: 'ollama', label: 'Ollama 本地（零成本）', baseUrl: 'http://localhost:11434/v1', models: ['qwen3:8b', 'deepseek-r1:8b', 'gpt-oss:20b', 'llama3.1:8b'], hint: '需本机已装 Ollama 并拉取模型；或用 ollama pull 拉新模型' },
  { key: 'custom', label: '自定义', baseUrl: '', models: [] }
]
const providerKey = ref('zhipu')
const activePreset = computed(() => PROVIDER_PRESETS.find(pp => pp.key === providerKey.value))

const applyPreset = () => {
  const preset = activePreset.value
  if (preset && preset.baseUrl) {
    cfgForm.value.baseUrl = preset.baseUrl
  }
  cfgForm.value.model = ''
  fetchedModels.value = []
}

const fetchModels = async () => {
  if (!cfgForm.value.baseUrl || fetchingModels.value) return
  fetchingModels.value = true
  try {
    const res = await getModelList({
      baseUrl: cfgForm.value.baseUrl,
      apiKey: cfgForm.value.apiKey || undefined
    })
    if (res.code === 200 && res.data) {
      if (res.data.supported && res.data.models?.length) {
        fetchedModels.value = res.data.models
        ElMessage.success(`已拉取 ${res.data.models.length} 个模型`)
      } else {
        ElMessage.info(res.data.message || '该厂商不支持模型列表，请手动输入模型名')
      }
    } else {
      ElMessage.error(res.message || '拉取失败')
    }
  } catch (e) {
    ElMessage.error('拉取失败：' + (e?.message || '网络错误'))
  } finally {
    fetchingModels.value = false
  }
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
  critic: 'DJ 在自我质检…',
  dj: '正在组织语言…'
}

// 后端 song_card → player 歌曲对象（写操作只认 songId：外源卡只有播放/入队）
const toPlayerSong = (card) => ({
  id: card.source === 'local' ? card.songId : `ext:gequhai:${card.sourceTrackId}`,
  title: card.title,
  artists: [{ name: card.artist }],
  cover: card.cover,
  url: card.streamUrl || card.url || '',
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
      ask()
      return
    }
  }
}

const ask = async () => {
  const query = draft.value.trim()
  if (!query || loading.value) return
  draft.value = ''
  loading.value = true
  messages.value.push({ role: 'user', text: query })

  const dj = { role: 'dj', text: '', statusText: STAGE_TEXT.dispatcher, cards: [], done: false, error: '', steps: [], activeStep: -1, thought: '', playlistInfo: null }
  // 请求一发出就显示第一步（不等后端事件——LLM 排队也能看到时间线）
  ensureStep(dj, 'dispatcher')
  dj.activeStep = 0
  messages.value.push(dj)
  scrollBottom()

  try {
    await streamDj(query, sessionId.value, {
      onEvent: (event, data) => {
        if (event === 'session' && data.sessionId) {
          sessionId.value = data.sessionId  // 服务端归一化后的会话 ID（P2 会话记忆）
        }
        if (event === 'stage') {
          dj.statusText = STAGE_TEXT[data.stage] || dj.statusText
          if (data.stage === 'librarian' && data.round > 1) {
            dj.statusText = `小羊驼在自我改进（第 ${data.round} 轮检索）…`
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
            streamUrl: data.streamUrl,
            reason: data.reason || '',
            isExternal: !!data.isExternal,
            key: `${data.source}-${data.songId || data.sourceTrackId}`
          }
          dj.cards.push(card)
          fillCardCover(card)
        } else if (event === 'error') {
          dj.error = data.message || 'DJ 开小差了，稍后再试'
          if (/还没有配置密钥/.test(dj.error)) openSettings()
        } else if (event === 'done') {
          dj.done = true
          dj.statusText = ''
          dj.steps.forEach(st => { st.done = true })
          dj.activeStep = -1
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
  }
}

// ===== AI 连接配置（P3 BYOK 可视化配置板块） =====
const loadConfig = async () => {
  try {
    const res = await getAiConfig()
    if (res.code === 200) {
      cfg.value = res.data || {}
      cfgForm.value.baseUrl = cfg.value.baseUrl || ''
      cfgForm.value.model = cfg.value.model || ''
      cfgForm.value.apiKey = ''
      const match = PROVIDER_PRESETS.find(pp => pp.baseUrl && pp.baseUrl === cfg.value.baseUrl)
      providerKey.value = match ? match.key : (cfg.value.baseUrl ? 'custom' : 'zhipu')
      cfgLoaded.value = true
    }
  } catch (e) {
    console.error('加载 AI 配置失败:', e)
  }
}

const openSettings = () => {
  testResult.value = ''
  cfgVisible.value = true
  loadConfig()
}

const saveCfg = async () => {
  if (!cfgForm.value.apiKey && !cfg.value.configured) {
    ElMessage.warning('请填写 API Key')
    return
  }
  saving.value = true
  try {
    const res = await saveAiConfig({
      apiKey: cfgForm.value.apiKey || undefined,
      baseUrl: cfgForm.value.baseUrl || undefined,
      model: cfgForm.value.model || undefined
    })
    if (res.code === 200) {
      cfg.value = res.data || {}
      cfgForm.value.apiKey = ''
      testResult.value = ''
      ElMessage.success('AI 连接配置已保存')
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const testCfg = async () => {
  testing.value = true
  testResult.value = '测试中…'
  try {
    const res = await testAiConfig({
      apiKey: cfgForm.value.apiKey || undefined,
      baseUrl: cfgForm.value.baseUrl || undefined,
      model: cfgForm.value.model || undefined
    })
    if (res.code === 200 && res.data) {
      testResult.value = (res.data.ok ? '✓ ' : '✗ ') + (res.data.message || '') + `（${res.data.latencyMs}ms）`
    } else {
      testResult.value = res.message || '测试失败'
    }
  } catch (e) {
    testResult.value = '测试失败：' + (e?.message || '网络错误')
  } finally {
    testing.value = false
  }
}

const clearCfg = async () => {
  try {
    const res = await clearAiConfig()
    if (res.code === 200) {
      cfg.value = res.data || { configured: false }
      cfgForm.value = { apiKey: '', baseUrl: '', model: '' }
      ElMessage.success('已清除 AI 连接配置')
    }
  } catch (e) {
    ElMessage.error('清除失败')
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

.dj-input {
  display: flex;
  gap: 8px;
  padding: 12px 4px 4px;
  border-top: 1px solid var(--border-color-light);
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
