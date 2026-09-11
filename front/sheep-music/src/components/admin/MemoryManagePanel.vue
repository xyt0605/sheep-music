<template>
  <section class="management-section memory-manage">
    <!-- 工具栏 -->
    <div class="section-toolbar">
      <div class="toolbar-left">
        <div class="search-box">
          <el-icon><Search /></el-icon>
          <input
            v-model="keyword"
            type="search"
            placeholder="搜索标题或配文"
          >
        </div>
        <el-select
          v-model="statusFilter"
          class="status-select"
          placeholder="全部状态"
          clearable
        >
          <el-option
            label="全部状态"
            :value="null"
          />
          <el-option
            label="已发布"
            value="published"
          />
          <el-option
            label="已下架"
            value="draft"
          />
        </el-select>
      </div>
      <div class="toolbar-actions">
        <span class="result-count">共 {{ filtered.length }} 个素材</span>
        <el-button
          text
          :loading="loading"
          @click="load"
        >
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button
          type="primary"
          @click="uploadRef?.open()"
        >
          <el-icon><Plus /></el-icon>
          上传素材
        </el-button>
      </div>
    </div>

    <!-- 素材列表 -->
    <el-table
      v-loading="loading"
      :data="filtered"
      class="admin-table"
      table-layout="fixed"
    >
      <el-table-column
        label="素材"
        min-width="280"
      >
        <template #default="{ row }">
          <div class="media-cell">
            <img
              :src="row.type === 'photo' ? ossThumb(row.mediaUrl, 100) : (row.coverUrl || videoPosterFallback(row))"
              class="media-thumb"
            >
            <span
              v-if="row.type === 'video'"
              class="media-type-badge"
            >视频</span>
            <div class="media-copy">
              <strong>{{ row.title || '（未命名）' }}</strong>
              <span class="media-caption">{{ row.caption || '无配文' }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        label="日期"
        width="120"
      >
        <template #default="{ row }">
          {{ row.memoryDate || row.createTime?.slice(0, 10) || '—' }}
        </template>
      </el-table-column>
      <el-table-column
        label="那天的歌"
        min-width="160"
      >
        <template #default="{ row }">
          <span v-if="row.songTitle">{{ row.songTitle }}</span>
          <span
            v-if="row.songSource === 'gequhai'"
            class="source-tag"
          >试听</span>
          <span
            v-if="!row.songTitle"
            class="dim"
          >未绑定</span>
        </template>
      </el-table-column>
      <el-table-column
        label="星星"
        width="70"
      >
        <template #default="{ row }">
          ★ {{ row.starCount || 0 }}
        </template>
      </el-table-column>
      <el-table-column
        label="状态"
        width="90"
      >
        <template #default="{ row }">
          <el-tag
            size="small"
            :type="row.status === 'published' ? 'success' : 'info'"
          >
            {{ row.status === 'published' ? '已发布' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="230"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            text
            size="small"
            @click="openEdit(row)"
          >编辑</el-button>
          <el-button
            text
            size="small"
            :type="row.status === 'published' ? 'info' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'published' ? '下架' : '发布' }}
          </el-button>
          <el-button
            text
            size="small"
            type="danger"
            @click="remove(row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 星星动态（近 14 天） -->
    <div class="star-feed-card">
      <h4 class="star-feed-title">
        ✦ 星星动态（近 14 天）
      </h4>
      <div
        v-if="!starFeed.length"
        class="star-feed-empty"
      >
        还没有星星被点亮
      </div>
      <ul
        v-else
        class="star-feed-list"
      >
        <li
          v-for="s in starFeed"
          :key="s.id"
          class="star-feed-item"
        >
          <span class="star-feed-date">{{ fmtTime(s.createTime) }}</span>
          <span class="star-feed-text">{{ s.nickname }} 点亮了「{{ itemTitle(s.itemId) }}」</span>
        </li>
      </ul>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editVisible"
      title="编辑素材"
      width="560px"
      append-to-body
    >
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input
            v-model="editForm.title"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item label="配文（写给她的那句话）">
          <el-input
            v-model="editForm.caption"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="拍摄日期">
          <el-date-picker
            v-model="editForm.memoryDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span>那天的歌</span>
            <el-button
              size="small"
              text
              type="primary"
              style="margin-left: 8px"
              @click="editSongSelectorVisible = true"
            >{{ editForm.songId ? '换一首' : '选歌' }}</el-button>
          </template>
          <div
            v-if="editForm.songId || editForm.songExternalId"
            class="bound-song"
          >
            <span>{{ editForm.songTitle }} · {{ editForm.songArtist || '未知歌手' }}</span>
            <span
              v-if="editForm.songSource === 'gequhai'"
              class="source-tag"
            >试听</span>
            <el-button
              text
              size="small"
              @click="clearEditSong"
            >移除</el-button>
          </div>
          <span
            v-else
            class="dim"
          >未绑定</span>
        </el-form-item>
        <el-form-item label="同日排序权重（越大越靠前）">
          <el-input-number
            v-model="editForm.sortOrder"
            :min="0"
            :max="9999"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="saveEdit"
        >保存</el-button>
      </template>
    </el-dialog>

    <SongSelector
      v-model="editSongSelectorVisible"
      @select="onEditSongSelected"
    />
    <MemoryUploadDialog
      ref="uploadRef"
      @saved="load"
    />
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { getMemoryAdminList, updateMemoryItem, changeMemoryStatus, deleteMemoryItem, getMemoryStarFeed } from '@/api/memory'
import { ossThumb } from '@/utils/image'
import SongSelector from '@/components/SongSelector.vue'
import MemoryUploadDialog from '@/components/admin/MemoryUploadDialog.vue'

const loading = ref(false)
const items = ref([])
const starFeed = ref([])
const keyword = ref('')
const statusFilter = ref(null)
const uploadRef = ref(null)

const filtered = computed(() => {
  let list = items.value
  if (statusFilter.value) list = list.filter(i => i.status === statusFilter.value)
  if (keyword.value) {
    const k = keyword.value.toLowerCase()
    list = list.filter(i => (i.title || '').toLowerCase().includes(k) || (i.caption || '').toLowerCase().includes(k))
  }
  return list
})

const itemTitle = (itemId) => items.value.find(i => i.id === itemId)?.title || '一个瞬间'

const fmtTime = (t) => {
  if (!t) return ''
  return t.replace('T', ' ').slice(5, 16)
}

const videoPosterFallback = (row) => row.mediaUrl

const load = async () => {
  loading.value = true
  try {
    const [listRes, feedRes] = await Promise.all([getMemoryAdminList(), getMemoryStarFeed()])
    items.value = listRes.data || []
    starFeed.value = feedRes.data || []
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
onMounted(load)

// ===== 编辑 =====
const editVisible = ref(false)
const editSongSelectorVisible = ref(false)
const saving = ref(false)
const editForm = ref({
  id: null, title: '', caption: '', memoryDate: '', songId: null,
  songSource: 'local', songExternalId: '',
  songTitle: '', songArtist: '', songCover: '', sortOrder: 0
})

const openEdit = (row) => {
  editForm.value = {
    id: row.id,
    title: row.title || '',
    caption: row.caption || '',
    memoryDate: row.memoryDate || '',
    songId: row.songId || null,
    songSource: row.songSource || 'local',
    songExternalId: row.songExternalId || '',
    songTitle: row.songTitle || '',
    songArtist: row.songArtist || '',
    songCover: row.songCover || '',
    sortOrder: row.sortOrder || 0
  }
  editVisible.value = true
}

const onEditSongSelected = (song) => {
  if (song.isExternal) {
    editForm.value.songId = null
    editForm.value.songSource = 'gequhai'
    editForm.value.songExternalId = String(song.sourceTrackId)
    editForm.value.songTitle = song.name || song.title || ''
    editForm.value.songArtist = song.artist || ''
    editForm.value.songCover = song.cover || ''
    return
  }
  editForm.value.songSource = 'local'
  editForm.value.songExternalId = ''
  editForm.value.songId = song.id
  editForm.value.songTitle = song.title || song.name || ''
  editForm.value.songArtist = song.artist || song.artistName || (song.artists || []).map(a => a.name).join(' / ')
  editForm.value.songCover = song.cover || ''
}
const clearEditSong = () => {
  editForm.value.songId = null
  editForm.value.songSource = 'local'
  editForm.value.songExternalId = ''
  editForm.value.songTitle = ''
  editForm.value.songArtist = ''
  editForm.value.songCover = ''
}

const saveEdit = async () => {
  saving.value = true
  try {
    const f = editForm.value
    const external = f.songSource === 'gequhai'
    await updateMemoryItem(f.id, {
      title: f.title,
      caption: f.caption,
      memoryDate: f.memoryDate || null,
      songId: f.songId || null,
      songSource: f.songSource || 'local',
      songExternalId: external ? f.songExternalId : null,
      songTitle: external ? f.songTitle : undefined,
      songArtist: external ? f.songArtist : undefined,
      songCover: external ? f.songCover : undefined,
      sortOrder: f.sortOrder
    })
    ElMessage.success('已保存')
    editVisible.value = false
    load()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

// ===== 上下架/删除 =====
const toggleStatus = async (row) => {
  const next = row.status === 'published' ? 'draft' : 'published'
  try {
    await changeMemoryStatus(row.id, next)
    row.status = next
    ElMessage.success(next === 'published' ? '已发布' : '已下架')
  } catch (e) {
    // 拦截器已提示
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `删除后无法恢复（OSS 文件也会删除）：${row.title || '未命名素材'}`,
      '确定删除这个瞬间吗？',
      { confirmButtonText: '删除', cancelButtonText: '再想想', type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteMemoryItem(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    // 拦截器已提示
  }
}
</script>

<style scoped>
.memory-manage { display: grid; gap: 18px; }

.media-cell { display: flex; align-items: center; gap: 12px; }
.media-thumb {
  width: 56px; height: 56px;
  border-radius: 10px;
  object-fit: cover;
  background: var(--bg-secondary, #f5f5f5);
  flex: 0 0 56px;
}
.media-type-badge {
  position: absolute;
  margin-left: 40px;
  margin-top: -46px;
  background: rgba(0,0,0,0.55);
  color: #fff;
  font-size: 10px;
  border-radius: 4px;
  padding: 1px 5px;
}
.media-copy { display: grid; gap: 3px; min-width: 0; }
.media-copy strong { font-size: 13.5px; }
.media-caption {
  font-size: 12px;
  color: var(--text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 220px;
}
.dim { color: var(--text-tertiary); font-size: 12px; }

.star-feed-card {
  border: 1px solid var(--border-color-light);
  border-radius: var(--radius-lg);
  padding: 16px 18px;
}
.star-feed-title { font-size: 14px; margin-bottom: 10px; }
.star-feed-empty { font-size: 12.5px; color: var(--text-tertiary); }
.star-feed-list {
  list-style: none;
  margin: 0; padding: 0;
  display: grid;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
}
.star-feed-item { display: flex; gap: 12px; font-size: 12.5px; }
.star-feed-date { color: var(--text-tertiary); font-variant-numeric: tabular-nums; white-space: nowrap; }
.star-feed-text { color: var(--text-primary); }

.bound-song { display: flex; align-items: center; gap: 10px; }
.source-tag {
  font-size: 10px;
  color: #b8860b;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 4px;
  padding: 1px 5px;
}
</style>
