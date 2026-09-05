<template>
  <section class="management-section">
    <div class="section-toolbar">
      <div class="toolbar-left">
        <div class="search-box">
          <el-icon><Search /></el-icon>
          <input v-model="keyword" type="search" placeholder="搜索歌曲或歌手" @keyup.enter="search">
          <button v-if="keyword" type="button" aria-label="清空搜索" @click="clearSearch">
            <el-icon><Close /></el-icon>
          </button>
        </div>
        <el-select v-model="status" class="status-select" placeholder="全部状态" clearable @change="search">
          <el-option label="全部状态" :value="null" />
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
      </div>
      <div class="toolbar-actions">
        <span class="result-count">共 {{ pagination.total }} 首歌曲</span>
        <el-button text :loading="loading" @click="fetchSongs">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="songList" class="admin-table" table-layout="fixed">
      <el-table-column label="歌曲" min-width="300">
        <template #default="{ row }">
          <div class="song-cell">
            <el-image :src="row.cover" fit="cover" class="song-cover">
              <template #error><div class="cover-fallback"><el-icon><Headset /></el-icon></div></template>
            </el-image>
            <div class="song-copy">
              <strong>{{ row.title }}</strong>
              <span>{{ artistsText(row) }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="albumName" label="专辑" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.albumName || '未设置专辑' }}</template>
      </el-table-column>
      <el-table-column label="标签" min-width="150">
        <template #default="{ row }">
          <div class="tag-stack">
            <el-tag v-if="row.genre" size="small">{{ row.genre }}</el-tag>
            <el-tag v-if="row.language" size="small" type="success">{{ row.language }}</el-tag>
            <span v-if="!row.genre && !row.language">未设置</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="时长" width="90">
        <template #default="{ row }">{{ formatDuration(row.duration) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '上架' : '下架' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right" align="right">
        <template #default="{ row }">
          <el-button text @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除这首歌曲吗？" @confirm="removeSong(row.id)">
            <template #reference><el-button text type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
      <template #empty><el-empty description="没有找到歌曲" /></template>
    </el-table>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchSongs"
        @current-change="fetchSongs"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="编辑歌曲" width="720px" top="6vh" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-position="top">
        <div class="form-grid two-columns">
          <el-form-item label="歌曲名称" prop="title"><el-input v-model="formData.title" maxlength="100" /></el-form-item>
          <el-form-item label="专辑名称"><el-input v-model="formData.albumName" /></el-form-item>
        </div>
        <el-form-item label="歌手" prop="artistIds">
          <el-select v-model="formData.artistIds" multiple filterable collapse-tags collapse-tags-tooltip placeholder="选择歌手" style="width: 100%">
            <el-option v-for="artist in artists" :key="artist.id" :label="artist.name" :value="artist.id" />
          </el-select>
        </el-form-item>
        <div class="form-grid three-columns">
          <el-form-item label="类型"><el-select v-model="formData.genre" clearable style="width: 100%"><el-option v-for="item in genres" :key="item" :label="item" :value="item" /></el-select></el-form-item>
          <el-form-item label="语言"><el-select v-model="formData.language" clearable style="width: 100%"><el-option v-for="item in languages" :key="item" :label="item" :value="item" /></el-select></el-form-item>
          <el-form-item label="状态"><el-select v-model="formData.status" style="width: 100%"><el-option label="上架" :value="1" /><el-option label="下架" :value="0" /></el-select></el-form-item>
        </div>
        <div class="edit-resources">
          <el-form-item label="封面 URL"><el-input v-model="formData.cover" placeholder="可选" /></el-form-item>
          <el-form-item label="音频 URL" prop="url"><el-input v-model="formData.url" /></el-form-item>
        </div>
        <el-form-item label="歌词"><el-input v-model="formData.lyric" type="textarea" :rows="5" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Headset, Refresh, Search } from '@element-plus/icons-vue'
import { deleteSong, getSongList, updateSong } from '@/api/song'
import { getAllArtists } from '@/api/artist'

const loading = ref(false)
const submitting = ref(false)
const keyword = ref('')
const status = ref(null)
const songList = ref([])
const artists = ref([])
const dialogVisible = ref(false)
const formRef = ref()
const pagination = reactive({ page: 1, size: 10, total: 0 })
const formData = reactive({ id: null, title: '', artistIds: [], albumName: '', genre: '', language: '', cover: '', url: '', lyric: '', status: 1 })
const genres = ['流行', '摇滚', '民谣', '电子', '说唱', 'R&B', '爵士', '古典', '轻音乐', '纯音乐']
const languages = ['国语', '粤语', '英语', '日语', '韩语', '其他']
const formRules = {
  title: [{ required: true, message: '请输入歌曲名称', trigger: 'blur' }],
  artistIds: [{ type: 'array', min: 1, required: true, message: '至少选择一位歌手', trigger: 'change' }],
  url: [{ required: true, message: '音频地址不能为空', trigger: 'blur' }]
}

const fetchSongs = async () => {
  loading.value = true
  try {
    const res = await getSongList({ page: pagination.page - 1, size: pagination.size, keyword: keyword.value || undefined, status: status.value === null ? undefined : status.value })
    songList.value = res.data?.content || []
    pagination.total = res.data?.totalElements || 0
  } catch (error) {
    ElMessage.error(error.message || '获取歌曲列表失败')
  } finally {
    loading.value = false
  }
}

const fetchArtists = async () => {
  try {
    const res = await getAllArtists()
    artists.value = res.data || []
  } catch (error) {
    ElMessage.error('歌手列表加载失败')
  }
}

const search = () => { pagination.page = 1; fetchSongs() }
const clearSearch = () => { keyword.value = ''; search() }

const openEdit = (row) => {
  Object.assign(formData, {
    id: row.id,
    title: row.title || '',
    artistIds: row.artists?.map(item => item.id) || [],
    albumName: row.albumName || '',
    genre: row.genre || '',
    language: row.language || '',
    cover: row.cover || '',
    url: row.url || '',
    lyric: row.lyric || '',
    status: row.status ?? 1
  })
  dialogVisible.value = true
}

const submitEdit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await updateSong(formData.id, { title: formData.title.trim(), artistIds: formData.artistIds, albumName: formData.albumName || null, genre: formData.genre || null, language: formData.language || null, cover: formData.cover || null, url: formData.url.trim(), lyric: formData.lyric || null, status: formData.status })
    ElMessage.success('歌曲信息已更新')
    dialogVisible.value = false
    await fetchSongs()
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    submitting.value = false
  }
}

const removeSong = async (id) => {
  try {
    await deleteSong(id)
    ElMessage.success('歌曲已删除')
    if (songList.value.length === 1 && pagination.page > 1) pagination.page -= 1
    await fetchSongs()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

const artistsText = row => row.artists?.length ? row.artists.map(item => item.name).join(' / ') : '未关联歌手'
const formatDuration = seconds => seconds ? `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}` : '00:00'

onMounted(() => { fetchSongs(); fetchArtists() })
</script>

<style scoped>
.management-section { display: grid; gap: 14px; }
.section-toolbar, .toolbar-left, .toolbar-actions { display: flex; align-items: center; gap: 12px; }
.section-toolbar { justify-content: space-between; }
.search-box { display: flex; align-items: center; gap: 8px; width: min(360px, 100%); height: 38px; padding: 0 10px; border: 1px solid var(--border-color); border-radius: var(--radius-md); background: var(--surface-raised); color: var(--text-tertiary); }
.search-box input { min-width: 0; flex: 1; border: 0; outline: 0; background: transparent; color: var(--text-primary); font-size: 13px; }
.search-box button { display: inline-grid; width: 24px; height: 24px; place-items: center; padding: 0; border: 0; background: transparent; color: var(--text-tertiary); cursor: pointer; }
.status-select { width: 120px; }
.result-count { color: var(--text-tertiary); font-family: var(--font-mono); font-size: 11px; }
.song-cell { display: flex; align-items: center; gap: 12px; min-width: 0; }
.song-cover, .cover-fallback { width: 46px; height: 46px; flex: 0 0 46px; border-radius: var(--radius-sm); }
.cover-fallback { display: grid; place-items: center; background: var(--surface-sunken); color: var(--text-tertiary); }
.song-copy { display: grid; gap: 4px; min-width: 0; }
.song-copy strong, .song-copy span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.song-copy span, .tag-stack > span { color: var(--text-tertiary); font-size: 12px; }
.tag-stack { display: flex; flex-wrap: wrap; gap: 5px; }
.pagination-row { display: flex; justify-content: flex-end; padding-top: 4px; }
.form-grid, .edit-resources { display: grid; gap: 14px; }
.two-columns, .edit-resources { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.three-columns { grid-template-columns: repeat(3, minmax(0, 1fr)); }
@media (max-width: 760px) { .section-toolbar { align-items: stretch; flex-direction: column; } .toolbar-left { align-items: stretch; flex-direction: column; } .search-box, .status-select { width: 100%; } .toolbar-actions { justify-content: space-between; } .two-columns, .three-columns, .edit-resources { grid-template-columns: 1fr; } }
</style>
