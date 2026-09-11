<template>
  <section class="management-section system-settings">
    <!-- ===== OSS 对象存储 ===== -->
    <div class="settings-card">
      <header class="card-head">
        <div>
          <h3 class="card-title">
            <el-icon><Box /></el-icon>
            OSS 对象存储
          </h3>
          <p class="card-desc">
            照片/视频/音频/封面的存储桶。保存后立即生效，无需重启。
          </p>
        </div>
        <el-tag
          :type="oss.configured ? 'success' : 'warning'"
          size="small"
        >
          {{ oss.configured ? '面板配置' : 'yml 默认配置' }}
        </el-tag>
      </header>

      <el-form
        label-position="top"
        class="settings-form"
      >
        <div class="form-grid">
          <el-form-item label="Endpoint">
            <el-input
              v-model="ossForm.endpoint"
              :placeholder="oss.defaultEndpoint || 'oss-cn-hangzhou.aliyuncs.com'"
            />
          </el-form-item>
          <el-form-item label="BucketName">
            <el-input
              v-model="ossForm.bucketName"
              :placeholder="oss.defaultBucketName || 'sheepmusic'"
            />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="AccessKeyId">
            <el-input
              v-model="ossForm.accessKeyId"
              :placeholder="oss.accessKeyIdMasked || '阿里云 RAM 子账号的 AccessKeyId'"
            />
          </el-form-item>
          <el-form-item label="AccessKeySecret">
            <el-input
              v-model="ossForm.accessKeySecret"
              type="password"
              show-password
              :placeholder="oss.hasSecret ? '已保存（不显示）——留空则沿用' : '阿里云 RAM 子账号的 AccessKeySecret'"
            />
          </el-form-item>
        </div>
        <el-form-item label="URL 前缀">
          <el-input
            v-model="ossForm.urlPrefix"
            :placeholder="oss.defaultUrlPrefix || 'https://sheepmusic.oss-cn-hangzhou.aliyuncs.com/'"
          />
        </el-form-item>
        <div
          v-if="oss.configured"
          class="current-line"
        >
          当前 AccessKeyId：<b>{{ oss.accessKeyIdMasked }}</b>
        </div>
      </el-form>

      <div class="card-actions">
        <el-button
          type="primary"
          :loading="ossSaving"
          @click="saveOss"
        >保存</el-button>
        <el-button
          :loading="ossTesting"
          @click="testOss"
        >测试连接</el-button>
        <el-button
          v-if="oss.configured"
          text
          type="danger"
          @click="resetOss"
        >恢复默认</el-button>
      </div>
      <div
        v-if="ossTestResult"
        class="test-result"
        :class="{ ok: ossTestResult.startsWith('✓') }"
      >{{ ossTestResult }}</div>
      <p class="card-hint">
        在哪拿密钥：阿里云控制台 → RAM 访问控制 → 创建子用户（授权 AliyunOSSFullAccess）→ 生成 AccessKey。建议用子账号而非主账号密钥。
      </p>
    </div>

    <!-- ===== DJ Agent 模型 ===== -->
    <div class="settings-card">
      <header class="card-head">
        <div>
          <h3 class="card-title">
            <el-icon><MagicStick /></el-icon>
            奶包 Agent 模型
          </h3>
          <p class="card-desc">
            奶包（小羊驼音乐管家）的 AI 连接。全局一份，所有用户直接使用。
          </p>
        </div>
        <el-tag
          :type="agent.configured ? 'success' : 'warning'"
          size="small"
        >
          {{ agent.configured ? `已配置：${agent.model}` : '未配置' }}
        </el-tag>
      </header>

      <el-form
        label-position="top"
        class="settings-form"
      >
        <el-form-item label="厂商">
          <el-select
            v-model="agentForm.providerKey"
            style="width: 100%"
            @change="applyAgentPreset"
          >
            <el-option
              v-for="p in PROVIDER_PRESETS"
              :key="p.key"
              :label="p.label"
              :value="p.key"
            />
          </el-select>
          <div
            v-if="agentPreset?.hint"
            class="field-hint"
          >{{ agentPreset.hint }}</div>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="API Key">
            <el-input
              v-model="agentForm.apiKey"
              type="password"
              show-password
              :placeholder="agent.apiKeyMasked || '该厂商的 API Key（留空沿用已保存的）'"
            />
          </el-form-item>
          <el-form-item label="接口地址">
            <el-input
              v-model="agentForm.baseUrl"
              :placeholder="agentPreset?.baseUrl || agent.defaultBaseUrl"
            />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item>
            <template #label>
              <span>模型</span>
              <el-button
                size="small"
                text
                type="primary"
                :loading="fetchingModels"
                :disabled="!agentForm.baseUrl"
                style="margin-left: 8px"
                @click="fetchModels"
              >获取实时列表</el-button>
            </template>
            <el-select
              v-model="agentForm.model"
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
          <el-form-item label="思考模式">
            <el-select
              v-model="agentForm.thinkingMode"
              style="width: 100%"
            >
              <el-option
                label="自动（跟随模型默认）"
                value="auto"
              />
              <el-option
                label="开启思考（质量优先）"
                value="enabled"
              />
              <el-option
                label="关闭思考（速度优先，更快）"
                value="disabled"
              />
            </el-select>
          </el-form-item>
        </div>
        <div
          v-if="agent.configured"
          class="current-line"
        >
          当前 Key：<b>{{ agent.apiKeyMasked }}</b> @ {{ agent.baseUrl }}
        </div>
      </el-form>

      <div class="card-actions">
        <el-button
          type="primary"
          :loading="agentSaving"
          @click="saveAgent"
        >保存</el-button>
        <el-button
          :loading="agentTesting"
          @click="testAgent"
        >测试连接</el-button>
        <el-button
          v-if="agent.configured"
          text
          type="danger"
          @click="clearAgent"
        >清除配置</el-button>
      </div>
      <div
        v-if="agentTestResult"
        class="test-result"
        :class="{ ok: agentTestResult.startsWith('✓') }"
      >{{ agentTestResult }}</div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Box, MagicStick } from '@element-plus/icons-vue'
import { getOssConfig, saveOssConfig, testOssConfig, resetOssConfig } from '@/api/systemConfig'
import { getAiConfig, saveAiConfig, clearAiConfig, testAiConfig, getModelList } from '@/api/agent'
import { PROVIDER_PRESETS, findProviderPreset, providerKeyOfBaseUrl } from '@/utils/aiProviders'

// ===== OSS =====
const oss = ref({})
const ossForm = ref({ endpoint: '', accessKeyId: '', accessKeySecret: '', bucketName: '', urlPrefix: '' })
const ossSaving = ref(false)
const ossTesting = ref(false)
const ossTestResult = ref('')

const loadOss = async () => {
  try {
    const res = await getOssConfig()
    oss.value = res.data || {}
    ossForm.value = { endpoint: '', accessKeyId: '', accessKeySecret: '', bucketName: '', urlPrefix: '' }
  } catch (e) { /* 拦截器已提示 */ }
}

const saveOss = async () => {
  ossSaving.value = true
  try {
    const res = await saveOssConfig({
      endpoint: ossForm.value.endpoint || undefined,
      accessKeyId: ossForm.value.accessKeyId || undefined,
      accessKeySecret: ossForm.value.accessKeySecret || undefined,
      bucketName: ossForm.value.bucketName || undefined,
      urlPrefix: ossForm.value.urlPrefix || undefined
    })
    if (res.code === 200) {
      oss.value = res.data || {}
      ossForm.value.accessKeyId = ''
      ossForm.value.accessKeySecret = ''
      ossTestResult.value = ''
      ElMessage.success('OSS 配置已保存，上传即刻可用')
    }
  } catch (e) { /* 拦截器已提示 */ } finally {
    ossSaving.value = false
  }
}

const testOss = async () => {
  ossTesting.value = true
  ossTestResult.value = '测试中…'
  try {
    const res = await testOssConfig({
      endpoint: ossForm.value.endpoint || undefined,
      accessKeyId: ossForm.value.accessKeyId || undefined,
      accessKeySecret: ossForm.value.accessKeySecret || undefined,
      bucketName: ossForm.value.bucketName || undefined
    })
    if (res.code === 200 && res.data) {
      ossTestResult.value = (res.data.ok ? '✓ ' : '✗ ') + (res.data.message || '') + `（${res.data.latencyMs}ms）`
    } else {
      ossTestResult.value = '✗ ' + (res.message || '测试失败')
    }
  } catch (e) {
    ossTestResult.value = '✗ 测试失败：' + (e?.message || '网络错误')
  } finally {
    ossTesting.value = false
  }
}

const resetOss = async () => {
  try {
    await ElMessageBox.confirm(
      '面板里的 OSS 配置将被删除，回落到 application.yml 的默认配置。确定吗？',
      '恢复默认',
      { confirmButtonText: '恢复', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) { return }
  try {
    const res = await resetOssConfig()
    if (res.code === 200) {
      oss.value = res.data || {}
      ossTestResult.value = ''
      ElMessage.success('已恢复默认配置')
    }
  } catch (e) { /* 拦截器已提示 */ }
}

// ===== DJ Agent =====
const agent = ref({})
const agentForm = ref({ providerKey: 'zhipu', apiKey: '', baseUrl: '', model: '', thinkingMode: 'auto' })
const agentSaving = ref(false)
const agentTesting = ref(false)
const agentTestResult = ref('')
const fetchedModels = ref([])
const fetchingModels = ref(false)

const agentPreset = computed(() => findProviderPreset(agentForm.value.providerKey))
const modelOptions = computed(() => {
  const merged = [...(agentPreset.value?.models || []), ...fetchedModels.value]
  return [...new Set(merged)]
})

const loadAgent = async () => {
  try {
    const res = await getAiConfig()
    agent.value = res.data || {}
    agentForm.value.baseUrl = agent.value.baseUrl || ''
    agentForm.value.model = agent.value.model || ''
    agentForm.value.thinkingMode = agent.value.thinkingMode || 'auto'
    agentForm.value.apiKey = ''
    agentForm.value.providerKey = providerKeyOfBaseUrl(agent.value.baseUrl)
    fetchedModels.value = []
  } catch (e) { /* 拦截器已提示 */ }
}

const applyAgentPreset = () => {
  const preset = agentPreset.value
  if (preset && preset.baseUrl) {
    agentForm.value.baseUrl = preset.baseUrl
  }
  agentForm.value.model = ''
  fetchedModels.value = []
}

const fetchModels = async () => {
  if (!agentForm.value.baseUrl || fetchingModels.value) return
  fetchingModels.value = true
  try {
    const res = await getModelList({
      baseUrl: agentForm.value.baseUrl,
      apiKey: agentForm.value.apiKey || undefined
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

const saveAgent = async () => {
  agentSaving.value = true
  try {
    const res = await saveAiConfig({
      apiKey: agentForm.value.apiKey || undefined,
      baseUrl: agentForm.value.baseUrl || undefined,
      model: agentForm.value.model || undefined,
      thinkingMode: agentForm.value.thinkingMode || 'auto'
    })
    if (res.code === 200) {
      agent.value = res.data || {}
      agentForm.value.apiKey = ''
      agentTestResult.value = ''
      ElMessage.success('DJ 模型配置已保存，全员生效')
    }
  } catch (e) { /* 拦截器已提示 */ } finally {
    agentSaving.value = false
  }
}

const testAgent = async () => {
  agentTesting.value = true
  agentTestResult.value = '测试中…'
  try {
    const res = await testAiConfig({
      apiKey: agentForm.value.apiKey || undefined,
      baseUrl: agentForm.value.baseUrl || undefined,
      model: agentForm.value.model || undefined
    })
    if (res.code === 200 && res.data) {
      agentTestResult.value = (res.data.ok ? '✓ ' : '✗ ') + (res.data.message || '') + `（${res.data.latencyMs}ms）`
    } else {
      agentTestResult.value = '✗ ' + (res.message || '测试失败')
    }
  } catch (e) {
    agentTestResult.value = '✗ 测试失败：' + (e?.message || '网络错误')
  } finally {
    agentTesting.value = false
  }
}

const clearAgent = async () => {
  try {
    const res = await clearAiConfig()
    if (res.code === 200) {
      agent.value = res.data || { configured: false }
      agentTestResult.value = ''
      ElMessage.success('已清除 DJ 模型配置')
    }
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(() => {
  loadOss()
  loadAgent()
})
</script>

<style scoped>
.system-settings {
  display: grid;
  gap: 20px;
}

.settings-card {
  border: 1px solid var(--border-color-light);
  border-radius: var(--radius-lg);
  padding: 20px 22px;
  background: var(--panel-bg, transparent);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  margin: 0 0 4px;
}
.card-desc {
  font-size: 12.5px;
  color: var(--text-tertiary);
  margin: 0;
}

.settings-form { max-width: 760px; }
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}
@media (max-width: 720px) {
  .form-grid { grid-template-columns: 1fr; }
}

.field-hint {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 4px;
  line-height: 1.5;
}

.current-line {
  font-size: 12.5px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.current-line b { color: var(--text-primary); font-variant-numeric: tabular-nums; }

.card-actions {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}

.test-result {
  margin-top: 12px;
  font-size: 13px;
  color: var(--el-color-danger);
  word-break: break-all;
}
.test-result.ok { color: var(--el-color-success); }

.card-hint {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--text-tertiary);
  line-height: 1.6;
}
</style>
