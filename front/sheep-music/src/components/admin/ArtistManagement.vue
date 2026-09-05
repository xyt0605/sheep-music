<template>
  <section class="management-section">
    <div class="section-toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="keyword" type="search" placeholder="搜索歌手名称" @keyup.enter="search">
        <button v-if="keyword" type="button" aria-label="清空搜索" @click="clearSearch">
          <el-icon><Close /></el-icon>
        </button>
      </div>
      <div class="toolbar-actions">
        <span class="result-count">共 {{ pagination.total }} 位歌手</span>
        <el-button text :loading="loading" @click="fetchArtists">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="artistList" class="admin-table" table-layout="fixed">
      <el-table-column label="歌手" min-width="240">
        <template #default="{ row }">
          <div class="artist-cell">
            <el-avatar :size="42" :src="ossThumb(row.avatar, 100)">{{ row.name?.charAt(0) }}</el-avatar>
            <div>
              <strong>{{ row.name }}</strong>
              <span>{{ row.region || '未设置地区' }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="简介" min-width="320" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description || '暂无简介' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right" align="right">
        <template #default="{ row }">
          <el-button text @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除这位歌手吗？" @confirm="removeArtist(row.id)">
            <template #reference><el-button text type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="没有找到歌手" />
      </template>
    </el-table>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchArtists"
        @current-change="fetchArtists"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="编辑歌手" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-position="top">
        <el-form-item label="歌手名称" prop="name"><el-input v-model="formData.name" maxlength="100" /></el-form-item>
        <div class="form-grid">
          <el-form-item label="国家/地区"><el-input v-model="formData.region" placeholder="可选" /></el-form-item>
          <el-form-item label="头像 URL"><el-input v-model="formData.avatar" placeholder="可选" /></el-form-item>
        </div>
        <el-form-item label="简介"><el-input v-model="formData.description" type="textarea" :rows="4" maxlength="1000" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ossThumb } from '@/utils/image'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Refresh, Search } from '@element-plus/icons-vue'
import { deleteArtist, getArtistList, updateArtist } from '@/api/artist'

const loading = ref(false)
const submitting = ref(false)
const keyword = ref('')
const artistList = ref([])
const dialogVisible = ref(false)
const formRef = ref()
const pagination = reactive({ page: 1, size: 10, total: 0 })
const formData = reactive({ id: null, name: '', avatar: '', region: '', description: '' })
const formRules = { name: [{ required: true, message: '请输入歌手名称', trigger: 'blur' }] }

const fetchArtists = async () => {
  loading.value = true
  try {
    const res = await getArtistList({ page: pagination.page - 1, size: pagination.size, keyword: keyword.value || undefined })
    artistList.value = res.data?.content || []
    pagination.total = res.data?.totalElements || 0
  } catch (error) {
    ElMessage.error(error.message || '获取歌手列表失败')
  } finally {
    loading.value = false
  }
}

const search = () => {
  pagination.page = 1
  fetchArtists()
}

const clearSearch = () => {
  keyword.value = ''
  search()
}

const openEdit = (row) => {
  Object.assign(formData, { id: row.id, name: row.name || '', avatar: row.avatar || '', region: row.region || '', description: row.description || '' })
  dialogVisible.value = true
}

const submitEdit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await updateArtist(formData.id, { name: formData.name.trim(), avatar: formData.avatar || null, region: formData.region || null, description: formData.description || null })
    ElMessage.success('歌手信息已更新')
    dialogVisible.value = false
    await fetchArtists()
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    submitting.value = false
  }
}

const removeArtist = async (id) => {
  try {
    await deleteArtist(id)
    ElMessage.success('歌手已删除')
    if (artistList.value.length === 1 && pagination.page > 1) pagination.page -= 1
    await fetchArtists()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

const formatDate = (value) => value ? new Date(value).toLocaleDateString('zh-CN') : '-'

onMounted(fetchArtists)
</script>

<style scoped>
.management-section { display: grid; gap: 14px; }
.section-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.search-box { display: flex; align-items: center; gap: 8px; width: min(360px, 100%); height: 38px; padding: 0 10px; border: 1px solid var(--border-color); border-radius: var(--radius-md); background: var(--surface-raised); color: var(--text-tertiary); }
.search-box input { min-width: 0; flex: 1; border: 0; outline: 0; background: transparent; color: var(--text-primary); font-size: 13px; }
.search-box button { display: inline-grid; width: 24px; height: 24px; place-items: center; padding: 0; border: 0; background: transparent; color: var(--text-tertiary); cursor: pointer; }
.toolbar-actions { display: flex; align-items: center; gap: 14px; }
.result-count { color: var(--text-tertiary); font-family: var(--font-mono); font-size: 11px; }
.artist-cell { display: flex; align-items: center; gap: 12px; min-width: 0; }
.artist-cell > div { display: grid; gap: 3px; min-width: 0; }
.artist-cell strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.artist-cell span { color: var(--text-tertiary); font-size: 12px; }
.pagination-row { display: flex; justify-content: flex-end; padding-top: 4px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
@media (max-width: 680px) { .section-toolbar { align-items: stretch; flex-direction: column; } .search-box { width: 100%; } .toolbar-actions { justify-content: space-between; } .form-grid { grid-template-columns: 1fr; } }
</style>
