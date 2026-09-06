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
            <div
              v-if="msg.statusText"
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
              v-if="msg.cards.length && msg.done"
              class="dj-msg-actions"
            >
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
              v-if="msg.cards.length && msg.done && !loading"
              class="dj-feedback"
            >
              <el-button
                size="small"
                text
                type="primary"
                @click="ask('多来点这样的')"
              >多来点这样的</el-button>
              <el-button
                size="small"
                text
                @click="ask('换个口味，来点不一样的')"
              >换个口味</el-button>
            </div>
            <div
              v-if="msg.error"
              class="dj-error"
            >{{ msg.error }}</div>
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
          <el-form-item label="模型">
            <el-select
              v-model="cfgForm.model"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入模型名"
              style="width: 100%"
            >
              <el-option
                v-for="m in activePreset?.models || []"
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
import { Loading, Setting } from '@element-plus/icons-vue'
import { streamDj, getAiConfig, saveAiConfig, clearAiConfig, testAiConfig } from '@/api/agent'
import { getExternalCover } from '@/api/externalMusic'
import { usePlayerStore } from '@/store/player'

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

// 厂商预设：任何 OpenAI 兼容端点均可（选"自定义"后手填）
const PROVIDER_PRESETS = [
  { key: 'zhipu', label: '智谱 BigModel', baseUrl: 'https://open.bigmodel.cn/api/paas/v4', models: ['glm-4.5-air', 'glm-4.5-flash', 'glm-4-plus'], hint: '免费注册 open.bigmodel.cn；flash 免费但当前响应慢' },
  { key: 'deepseek', label: 'DeepSeek', baseUrl: 'https://api.deepseek.com', models: ['deepseek-chat', 'deepseek-reasoner'], hint: '推荐 deepseek-chat（reasoner 思考耗大量 token）' },
  { key: 'moonshot', label: '月之暗面 Kimi', baseUrl: 'https://api.moonshot.cn/v1', models: ['moonshot-v1-8k', 'moonshot-v1-32k', 'kimi-k2-0711-preview'] },
  { key: 'qwen', label: '阿里通义千问', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', models: ['qwen-max', 'qwen-plus', 'qwen-turbo'] },
  { key: 'doubao', label: '字节豆包（火山方舟）', baseUrl: 'https://ark.cn-beijing.volces.com/api/v3', models: ['（填你的接入点 ID）'], hint: '模型填方舟控制台的接入点 ID' },
  { key: 'siliconflow', label: '硅基流动（聚合）', baseUrl: 'https://api.siliconflow.cn/v1', models: ['deepseek-ai/DeepSeek-V3', 'Qwen/Qwen2.5-72B-Instruct'] },
  { key: 'openai', label: 'OpenAI', baseUrl: 'https://api.openai.com/v1', models: ['gpt-4o-mini', 'gpt-4o'] },
  { key: 'gemini', label: 'Google Gemini', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', models: ['gemini-2.0-flash', 'gemini-1.5-pro'] },
  { key: 'ollama', label: 'Ollama 本地（零成本）', baseUrl: 'http://localhost:11434/v1', models: ['qwen2.5:7b', 'llama3.1:8b'], hint: '需本机已装 Ollama 并拉取模型' },
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
}

function genSessionId() {
  return (crypto.randomUUID && crypto.randomUUID()) || `dj-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}
const messages = ref([]) // {role:'user'|'dj', text, statusText, cards[], done, error}
const listRef = ref(null)
const defaultCover = '/default-cover.svg'

import { computed, watch } from 'vue'
watch(visible, v => { if (v) loadConfig() })
const playerStore = usePlayerStore()

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

// 外源卡片封面异步回填（与搜索页同款逻辑）
const fillCardCover = (card) => {
  if (card.isExternal && !card.cover && card.sourceTrackId) {
    getExternalCover({ source: 'gequhai', trackId: card.sourceTrackId })
      .then(res => { if (res.code === 200 && res.data) card.cover = res.data })
      .catch(() => {})
  }
}

const ask = async () => {
  const query = draft.value.trim()
  if (!query || loading.value) return
  draft.value = ''
  loading.value = true
  messages.value.push({ role: 'user', text: query })

  const dj = { role: 'dj', text: '', statusText: STAGE_TEXT.dispatcher, cards: [], done: false, error: '' }
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
            dj.statusText = `DJ 在自我改进（第 ${data.round} 轮检索）…`
          }
        } else if (event === 'action') {
          const argText = data.args?.keyword ? `“${data.args.keyword}”` : ''
          const actionText = { search_local: '本地曲库', search_web: '歌曲海', recommend: '你的听歌偏好', finish: '整理候选' }[data.action] || data.action
          dj.statusText = `${actionText}${argText}…`
        } else if (event === 'text_delta') {
          dj.statusText = ''
          dj.text += data.delta || ''
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
  gap: 8px;
  margin-top: 10px;
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

.dj-feedback {
  display: flex;
  gap: 4px;
  margin-top: 6px;
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
