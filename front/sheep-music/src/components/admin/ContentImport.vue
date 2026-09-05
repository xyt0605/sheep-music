<template>
  <section class="content-import">
    <div class="import-intro">
      <div>
        <span class="eyebrow">CONTENT INTAKE</span>
        <h3>把内容一次导入到位</h3>
        <p>歌手支持批量去重，歌曲支持直接复用已有歌手，也可以在导入时自动创建新歌手。</p>
      </div>
      <div class="import-mode" role="tablist" aria-label="导入类型">
        <button
          type="button"
          :class="['mode-button', { active: mode === 'artist' }]"
          @click="switchMode('artist')"
        >
          <el-icon><Microphone /></el-icon>
          <span>导入歌手</span>
        </button>
        <button
          type="button"
          :class="['mode-button', { active: mode === 'song' }]"
          @click="switchMode('song')"
        >
          <el-icon><Headset /></el-icon>
          <span>导入歌曲</span>
        </button>
      </div>
    </div>

    <div v-if="mode === 'artist'" class="import-panel">
      <div class="panel-heading">
        <div>
          <span class="step-index">01</span>
          <strong>批量导入歌手</strong>
        </div>
        <span class="helper-text">每行一个名称，重复项不会重复创建</span>
      </div>
      <el-form ref="artistFormRef" :model="artistForm" :rules="artistRules" label-position="top">
        <el-form-item label="歌手名称" prop="namesText">
          <el-input
            v-model="artistForm.namesText"
            type="textarea"
            :rows="10"
            resize="none"
            placeholder="例如：&#10;周杰伦&#10;陈奕迅&#10;Adele"
          />
        </el-form-item>
        <div class="form-actions">
          <span class="field-note">已识别 {{ parsedArtistNames.length }} 个有效名称</span>
          <el-button type="primary" :loading="submitting" @click="submitArtists">
            <el-icon><Upload /></el-icon>
            开始导入
          </el-button>
        </div>
      </el-form>
    </div>

    <div v-else class="import-panel song-import-panel">
      <div class="panel-heading">
        <div>
          <span class="step-index">01</span>
          <strong>导入歌曲</strong>
        </div>
        <span class="helper-text">必填项只有歌曲名、歌手和音频文件</span>
      </div>

      <el-form ref="songFormRef" :model="songForm" :rules="songRules" label-position="top">
        <div class="form-grid two-columns">
          <el-form-item label="歌曲名称" prop="title">
            <el-input v-model="songForm.title" placeholder="输入歌曲名称" clearable />
          </el-form-item>
          <el-form-item label="专辑名称">
            <el-input v-model="songForm.albumName" placeholder="可选" clearable />
          </el-form-item>
        </div>

        <div class="form-grid two-columns">
          <el-form-item label="已有歌手" prop="artistIds">
            <el-select
              v-model="songForm.artistIds"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="选择已有歌手"
              style="width: 100%"
            >
              <el-option v-for="artist in artists" :key="artist.id" :label="artist.name" :value="artist.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="新歌手名称">
            <el-select
              v-model="songForm.artistNames"
              multiple
              filterable
              allow-create
              default-first-option
              reserve-keyword
              placeholder="直接输入并回车，可自动创建"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <div class="form-grid three-columns">
          <el-form-item label="类型">
            <el-select v-model="songForm.genre" clearable placeholder="选择类型" style="width: 100%">
              <el-option v-for="item in genres" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="语言">
            <el-select v-model="songForm.language" clearable placeholder="选择语言" style="width: 100%">
              <el-option v-for="item in languages" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="时长（秒）">
            <el-input-number v-model="songForm.duration" :min="0" :max="7200" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>

        <div class="resource-grid">
          <div class="resource-field">
            <div class="resource-label">封面</div>
            <el-upload
              class="resource-upload cover-upload"
              action="/api/upload/cover"
              :headers="uploadHeaders"
              :show-file-list="false"
              :before-upload="beforeCoverUpload"
              :on-success="handleCoverSuccess"
              :on-error="handleUploadError"
            >
              <div v-if="songForm.cover" class="cover-preview">
                <img :src="songForm.cover" alt="歌曲封面">
                <span>重新上传</span>
              </div>
              <div v-else class="upload-placeholder">
                <el-icon><Picture /></el-icon>
                <span>上传封面</span>
              </div>
            </el-upload>
            <el-input v-model="songForm.cover" class="resource-url" placeholder="也可以粘贴图片 URL" clearable />
          </div>
          <div class="resource-field audio-field">
            <div class="resource-label">音频文件 <span>MP3 / WAV / FLAC</span></div>
            <el-upload
              class="resource-upload"
              action="/api/upload/music"
              :headers="uploadHeaders"
              :show-file-list="false"
              :before-upload="beforeMusicUpload"
              :on-success="handleMusicSuccess"
              :on-error="handleUploadError"
            >
              <el-button type="primary" plain :loading="musicUploading">
                <el-icon><Upload /></el-icon>
                {{ songForm.url ? '重新上传音频' : '上传音频' }}
              </el-button>
            </el-upload>
            <div v-if="songForm.url" class="resource-success">
              <el-icon><CircleCheck /></el-icon>
              <span>{{ compactUrl(songForm.url) }}</span>
            </div>
            <el-input v-model="songForm.url" class="resource-url" placeholder="也可以粘贴音频 URL" clearable />
          </div>
        </div>

        <el-form-item label="歌词">
          <el-input v-model="songForm.lyric" type="textarea" :rows="4" resize="none" placeholder="可选，支持 LRC 格式" />
        </el-form-item>

        <div class="form-actions">
          <span class="field-note">导入后会自动关联歌手并进入歌曲管理列表</span>
          <el-button type="primary" :loading="submitting" @click="submitSong">
            <el-icon><Upload /></el-icon>
            导入歌曲
          </el-button>
        </div>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, Headset, Microphone, Picture, Upload } from '@element-plus/icons-vue'
import { getAllArtists, importArtists } from '@/api/artist'
import { importSong } from '@/api/song'

const mode = ref('artist')
const submitting = ref(false)
const musicUploading = ref(false)
const artistFormRef = ref()
const songFormRef = ref()
const artists = ref([])
const artistForm = reactive({ namesText: '' })
const songForm = reactive({
  title: '',
  artistIds: [],
  artistNames: [],
  albumName: '',
  genre: '',
  language: '',
  duration: null,
  cover: '',
  url: '',
  lyric: '',
  status: 1
})

const genres = ['流行', '摇滚', '民谣', '电子', '说唱', 'R&B', '爵士', '古典', '轻音乐', '纯音乐']
const languages = ['国语', '粤语', '英语', '日语', '韩语', '其他']
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${localStorage.getItem('token') || ''}` }))
const parsedArtistNames = computed(() => [...new Set(artistForm.namesText.split(/[,，\n]+/).map(item => item.trim()).filter(Boolean))])
const artistRules = { namesText: [{ required: true, message: '请输入至少一个歌手名称', trigger: 'blur' }] }
const songRules = {
  title: [{ required: true, message: '请输入歌曲名称', trigger: 'blur' }],
  artistIds: [{ validator: (_rule, value, callback) => (value?.length || songForm.artistNames.length ? callback() : callback(new Error('请选择或填写至少一位歌手'))), trigger: 'change' }],
  url: [{ required: true, message: '请上传或填写音频地址', trigger: 'blur' }]
}

const switchMode = (nextMode) => {
  mode.value = nextMode
}

const loadArtists = async () => {
  try {
    const res = await getAllArtists()
    artists.value = res.data || []
  } catch (error) {
    ElMessage.error('歌手列表加载失败')
  }
}

const submitArtists = async () => {
  const valid = await artistFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = await importArtists(parsedArtistNames.value)
    const result = res.data || {}
    ElMessage.success(`导入完成：新增 ${result.createdCount || 0} 位，已存在 ${result.existingCount || 0} 位`)
    artistForm.namesText = ''
    await loadArtists()
  } catch (error) {
    ElMessage.error(error.message || '歌手导入失败')
  } finally {
    submitting.value = false
  }
}

const submitSong = async () => {
  const valid = await songFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await importSong({
      title: songForm.title.trim(),
      artistIds: songForm.artistIds,
      artistNames: songForm.artistNames,
      albumName: songForm.albumName || null,
      genre: songForm.genre || null,
      language: songForm.language || null,
      duration: songForm.duration || null,
      cover: songForm.cover || null,
      url: songForm.url.trim(),
      lyric: songForm.lyric || null,
      status: songForm.status
    })
    ElMessage.success('歌曲导入成功')
    resetSongForm()
    await loadArtists()
  } catch (error) {
    ElMessage.error(error.message || '歌曲导入失败')
  } finally {
    submitting.value = false
  }
}

const resetSongForm = () => {
  Object.assign(songForm, { title: '', artistIds: [], artistNames: [], albumName: '', genre: '', language: '', duration: null, cover: '', url: '', lyric: '', status: 1 })
  songFormRef.value?.clearValidate()
}

const beforeCoverUpload = (file) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('封面只能使用图片文件')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('封面不能超过 5MB')
    return false
  }
  return true
}

const beforeMusicUpload = (file) => {
  musicUploading.value = true
  if (!file.type.startsWith('audio/')) {
    musicUploading.value = false
    ElMessage.error('请选择音频文件')
    return false
  }
  if (file.size / 1024 / 1024 > 30) {
    musicUploading.value = false
    ElMessage.error('音频不能超过 30MB')
    return false
  }
  return true
}

const handleUploadError = (error) => {
  musicUploading.value = false
  let message = '上传失败，请稍后重试'
  try {
    const res = JSON.parse(error?.message)
    if (res?.message) message = res.message
  } catch (_) { /* 响应体不是 JSON 时用默认文案 */ }
  ElMessage.error(message)
}

const handleCoverSuccess = (response) => {
  if (response?.code === 200 && response.data?.url) {
    songForm.cover = response.data.url
  } else {
    ElMessage.error(response?.message || '封面上传失败')
  }
}

const handleMusicSuccess = (response) => {
  musicUploading.value = false
  if (response?.code === 200 && response.data?.url) {
    songForm.url = response.data.url
  } else {
    ElMessage.error(response?.message || '音频上传失败')
  }
}

const compactUrl = (url) => url ? url.split('/').pop() : ''

onMounted(loadArtists)
</script>

<style scoped>
.content-import { display: grid; gap: 18px; }
.import-intro { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; padding: 4px 0 20px; border-bottom: 1px solid var(--border-color); }
.eyebrow { color: var(--text-tertiary); font-family: var(--font-mono); font-size: 10px; letter-spacing: .16em; }
h3 { margin: 8px 0 6px; font-family: var(--font-display); font-size: 30px; line-height: 1.1; }
.import-intro p { max-width: 620px; margin: 0; color: var(--text-secondary); font-size: 13px; line-height: 1.7; }
.import-mode { display: flex; gap: 8px; flex: 0 0 auto; }
.mode-button { display: inline-flex; align-items: center; gap: 8px; min-height: 40px; padding: 0 14px; border: 1px solid var(--border-color); border-radius: var(--radius-md); background: var(--surface-raised); color: var(--text-secondary); cursor: pointer; font-weight: 700; transition: .2s ease; }
.mode-button:hover, .mode-button.active { border-color: var(--color-primary); color: var(--text-primary); background: var(--surface-elevated); }
.mode-button.active { box-shadow: 0 0 0 2px rgba(201,255,69,.12); }
.import-panel { padding: 22px; border: 1px solid var(--border-color); border-radius: var(--radius-lg); background: var(--surface-raised); }
.panel-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 22px; }
.panel-heading > div { display: flex; align-items: center; gap: 10px; }
.step-index { color: var(--signal-lime); font-family: var(--font-mono); font-size: 11px; font-weight: 800; }
.helper-text, .field-note { color: var(--text-tertiary); font-size: 12px; }
.form-grid { display: grid; gap: 16px; }
.two-columns { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.three-columns { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.resource-grid { display: grid; grid-template-columns: 180px minmax(0, 1fr); gap: 24px; margin: 2px 0 18px; }
.resource-label { display: flex; justify-content: space-between; margin-bottom: 8px; color: var(--text-secondary); font-size: 13px; font-weight: 700; }
.resource-label span { color: var(--text-tertiary); font-size: 11px; font-weight: 500; }
.resource-upload { display: block; }
.cover-upload :deep(.el-upload) { width: 160px; height: 160px; overflow: hidden; border: 1px dashed var(--border-strong); border-radius: var(--radius-md); }
.cover-preview, .upload-placeholder { display: grid; width: 100%; height: 100%; place-items: center; position: relative; color: var(--text-tertiary); }
.cover-preview img { width: 100%; height: 100%; object-fit: cover; }
.cover-preview span { position: absolute; right: 0; bottom: 0; left: 0; padding: 7px; background: rgba(0,0,0,.65); color: white; text-align: center; font-size: 11px; }
.upload-placeholder { gap: 8px; align-content: center; font-size: 12px; }
.upload-placeholder .el-icon { font-size: 25px; }
.resource-url { margin-top: 10px; }
.resource-success { display: flex; align-items: center; gap: 6px; margin: 12px 0 8px; color: var(--signal-lime); font-size: 12px; }
.resource-success span { overflow: hidden; color: var(--text-secondary); text-overflow: ellipsis; white-space: nowrap; }
.form-actions { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-top: 20px; padding-top: 18px; border-top: 1px solid var(--border-color); }
@media (max-width: 760px) { .import-intro, .panel-heading, .form-actions { align-items: flex-start; flex-direction: column; } .import-mode { width: 100%; } .mode-button { flex: 1; justify-content: center; } .two-columns, .three-columns, .resource-grid { grid-template-columns: 1fr; } .cover-upload :deep(.el-upload) { width: 130px; height: 130px; } }
</style>
