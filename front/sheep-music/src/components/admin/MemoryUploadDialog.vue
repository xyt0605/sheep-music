<template>
  <el-dialog
    v-model="visible"
    title="往小屋里添加新素材"
    width="640px"
    :close-on-click-modal="false"
    append-to-body
  >
    <!-- 第一步：选文件 -->
    <template v-if="step === 'pick'">
      <el-upload
        drag
        multiple
        :auto-upload="false"
        :show-file-list="false"
        :on-change="onPick"
        accept="image/*,video/mp4,video/quicktime,video/webm"
      >
        <div class="upload-drag-body">
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <p>把照片或视频拖到这里，或点击选择</p>
          <p class="upload-hint">
            图片 ≤10MB；视频 mp4/mov/webm ≤200MB（建议 1 分钟内 1080p）
          </p>
        </div>
      </el-upload>
    </template>

    <!-- 第二步：上传进度 -->
    <template v-else-if="step === 'uploading'">
      <div class="upload-progress-list">
        <div
          v-for="f in files"
          :key="f.uid"
          class="upload-progress-item"
        >
          <span class="up-name">{{ f.name }}</span>
          <el-progress
            :percentage="f.progress"
            :status="f.status === 'error' ? 'exception' : (f.status === 'done' ? 'success' : undefined)"
            style="flex: 1"
          />
          <span class="up-status">{{ statusText(f) }}</span>
        </div>
      </div>
      <div
        v-if="failedCount"
        class="upload-failed-tip"
      >
        {{ failedCount }} 个文件上传失败，可先保存成功的，稍后重试失败的
      </div>
    </template>

    <!-- 第三步：补元信息 -->
    <template v-else>
      <el-alert
        type="info"
        :closable="false"
        class="meta-alert"
        show-icon
      >
        已上传 {{ uploaded.length }} 个素材（{{ uploaded.filter(u => u.kind === 'video').length }} 个视频）。配文和歌曲可以在列表里逐张编辑，这里先填默认值。
      </el-alert>
      <el-form label-position="top">
        <el-form-item label="拍摄日期（用于按月分组）">
          <el-date-picker
            v-model="batch.memoryDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选一天"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="默认配文（可留空，之后在列表里逐张写）">
          <el-input
            v-model="batch.caption"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="写给婉婉的一句话"
          />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span>绑定一首歌（可选）</span>
            <el-button
              size="small"
              text
              type="primary"
              style="margin-left: 8px"
              @click="songSelectorVisible = true"
            >选歌</el-button>
          </template>
          <div
            v-if="batch.songId || batch.songExternalId"
            class="bound-song"
          >
            <img
              v-if="batch.songCover"
              :src="ossThumb(batch.songCover, 60)"
              class="bound-song-cover"
            >
            <span>{{ batch.songTitle }} · {{ batch.songArtist || '未知歌手' }}</span>
            <span
              v-if="batch.songSource === 'gequhai'"
              class="source-tag"
            >试听</span>
            <el-button
              text
              size="small"
              @click="clearSong"
            >移除</el-button>
          </div>
          <span
            v-else
            class="no-song"
          >那天的 BGM，不选也可以</span>
        </el-form-item>
      </el-form>
    </template>

    <template #footer>
      <el-button
        v-if="step === 'pick'"
        @click="visible = false"
      >取消</el-button>
      <template v-else-if="step === 'uploading'">
        <el-button
          v-if="uploaded.length"
          :disabled="uploadingNow"
          @click="step = 'meta'"
        >跳过失败的，继续</el-button>
      </template>
      <template v-else>
        <el-button @click="visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="saveAll"
        >全部保存（{{ uploaded.length }}）</el-button>
      </template>
    </template>

    <!-- 复用站内选歌器 -->
    <SongSelector
      v-model="songSelectorVisible"
      @select="onSongSelected"
    />
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { uploadMemoryFile, createMemoryItem } from '@/api/memory'
import { ossThumb } from '@/utils/image'
import SongSelector from '@/components/SongSelector.vue'

const emit = defineEmits(['saved'])
const visible = ref(false)
const step = ref('pick') // pick → uploading → meta
const files = ref([]) // {uid, name, raw, kind, progress, status}
const uploaded = ref([]) // {mediaUrl, kind}
const uploadingNow = ref(false)
const saving = ref(false)
const songSelectorVisible = ref(false)

const batch = ref({
  memoryDate: '',
  caption: '',
  songId: null,
  songSource: 'local',
  songExternalId: '',
  songTitle: '',
  songArtist: '',
  songCover: ''
})

const failedCount = computed(() => files.value.filter(f => f.status === 'error').length)

const open = () => {
  step.value = 'pick'
  files.value = []
  uploaded.value = []
  batch.value = { memoryDate: '', caption: '', songId: null, songSource: 'local', songExternalId: '', songTitle: '', songArtist: '', songCover: '' }
  visible.value = true
}
defineExpose({ open })

const kindOf = (file) => {
  const t = file.type || ''
  if (t.startsWith('image/')) return 'photo'
  if (t.startsWith('video/')) return 'video'
  return /\.(mp4|mov|webm)$/i.test(file.name) ? 'video' : null
}

const onPick = (file) => {
  const kind = kindOf(file.raw)
  if (!kind) {
    ElMessage.error(`不支持的文件类型：${file.name}`)
    return
  }
  files.value.push({ uid: file.uid, name: file.name, raw: file.raw, kind, progress: 0, status: 'pending' })
  // multiple 每次触发一个 on-change；进入 uploading 一次串行传
  if (!uploadingNow.value && files.value.every(f => f.status !== 'uploading')) {
    startUpload()
  }
}

const startUpload = async () => {
  step.value = 'uploading'
  uploadingNow.value = true
  for (const f of files.value) {
    if (f.status === 'done' || f.status === 'error') continue
    f.status = 'uploading'
    f.progress = 1
    try {
      const res = await uploadMemoryFile(f.raw, (e) => {
        if (e.total) f.progress = Math.round((e.loaded / e.total) * 100)
      })
      if (res.code === 200 && res.data?.url) {
        uploaded.value.push({ mediaUrl: res.data.url, kind: res.data.kind || f.kind })
        f.status = 'done'
        f.progress = 100
      } else {
        f.status = 'error'
      }
    } catch (e) {
      f.status = 'error'
    }
  }
  uploadingNow.value = false
  if (uploaded.value.length && !failedCount.value) {
    step.value = 'meta'
  }
}

const statusText = (f) => {
  if (f.status === 'done') return '已上传'
  if (f.status === 'error') return '失败'
  if (f.status === 'uploading') return '上传中'
  return '等待'
}

const onSongSelected = (song) => {
  if (song.isExternal) {
    // 歌曲海试听源：绑定外部曲目 ID，播放走免登录流式代理
    batch.value.songId = null
    batch.value.songSource = 'gequhai'
    batch.value.songExternalId = String(song.sourceTrackId)
    batch.value.songTitle = song.name || song.title || ''
    batch.value.songArtist = song.artist || ''
    batch.value.songCover = song.cover || ''
    return
  }
  batch.value.songSource = 'local'
  batch.value.songExternalId = ''
  batch.value.songId = song.id
  batch.value.songTitle = song.title || song.name || ''
  batch.value.songArtist = song.artist || song.artistName || (song.artists || []).map(a => a.name).join(' / ')
  batch.value.songCover = song.cover || ''
}

const clearSong = () => {
  batch.value.songId = null
  batch.value.songSource = 'local'
  batch.value.songExternalId = ''
  batch.value.songTitle = ''
  batch.value.songArtist = ''
  batch.value.songCover = ''
}

const saveAll = async () => {
  saving.value = true
  let ok = 0
  for (const u of uploaded.value) {
    try {
      await createMemoryItem({
        type: u.kind,
        mediaUrl: u.mediaUrl,
        title: u.kind === 'video' ? '视频' : '照片',
        caption: batch.value.caption || '',
        memoryDate: batch.value.memoryDate || null,
        songId: batch.value.songId || null,
        songSource: batch.value.songSource || 'local',
        songExternalId: batch.value.songExternalId || null,
        songTitle: batch.value.songSource === 'gequhai' ? (batch.value.songTitle || '') : undefined,
        songArtist: batch.value.songSource === 'gequhai' ? (batch.value.songArtist || '') : undefined,
        songCover: batch.value.songSource === 'gequhai' ? (batch.value.songCover || '') : undefined,
        status: 'published'
      })
      ok++
    } catch (e) {
      // 单个失败继续保存其余
    }
  }
  saving.value = false
  if (ok) {
    ElMessage.success(`已保存 ${ok} 个素材，小屋已更新`)
    emit('saved')
    visible.value = false
  }
}
</script>

<style scoped>
.upload-drag-body {
  padding: 26px 10px;
  text-align: center;
  color: var(--text-secondary);
}
.upload-icon { font-size: 44px; color: var(--el-color-primary); margin-bottom: 10px; }
.upload-hint { font-size: 12px; color: var(--text-tertiary); margin-top: 6px; }

.upload-progress-list { display: grid; gap: 12px; }
.upload-progress-item { display: flex; align-items: center; gap: 12px; }
.up-name {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
.up-status { font-size: 12px; color: var(--text-tertiary); width: 52px; }
.upload-failed-tip { margin-top: 12px; font-size: 12px; color: var(--el-color-warning); }

.meta-alert { margin-bottom: 16px; }
.bound-song { display: flex; align-items: center; gap: 10px; }
.bound-song-cover { width: 36px; height: 36px; border-radius: 8px; object-fit: cover; }
.source-tag {
  font-size: 10px;
  color: #b8860b;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 4px;
  padding: 1px 5px;
}
.no-song { font-size: 12px; color: var(--text-tertiary); }
</style>
