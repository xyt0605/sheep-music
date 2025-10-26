<template>
  <div class="artist-management">
    <!-- 操作栏 -->
    <div class="toolbar">
      <el-button type="primary" icon="Plus" @click="showAddDialog">
        新增歌手
      </el-button>
      <el-button icon="Refresh" @click="fetchArtists">刷新</el-button>
    </div>
    
    <!-- 歌手列表 -->
    <el-table
      :data="artistList"
      v-loading="loading"
      border
      stripe
      style="width: 100%"
    >
      <el-table-column type="index" label="序号" width="60" align="center" />
      
      <el-table-column label="头像" width="80" align="center">
        <template #default="{ row }">
          <el-avatar :size="50" :src="row.avatar">
            {{ row.name?.charAt(0) }}
          </el-avatar>
        </template>
      </el-table-column>
      
      <el-table-column prop="name" label="歌手名称" width="150" />
      
      <el-table-column prop="region" label="国家/地区" width="120" />
      
      <el-table-column prop="description" label="简介" show-overflow-tooltip />
      
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="showEditDialog(row)">
            编辑
          </el-button>
          <el-popconfirm
            title="确定要删除这个歌手吗？"
            @confirm="handleDelete(row.id)"
          >
            <template #reference>
              <el-button type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 分页 -->
    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.size"
      :total="pagination.total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="fetchArtists"
      @current-change="fetchArtists"
      style="margin-top: 20px; justify-content: center;"
    />
    
    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增歌手' : '编辑歌手'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="歌手名称" prop="name">
          <el-input
            v-model="formData.name"
            placeholder="请输入歌手名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="头像URL" prop="avatar">
          <el-input
            v-model="formData.avatar"
            placeholder="请输入头像URL"
            type="url"
          />
          <div style="margin-top: 10px;" v-if="formData.avatar">
            <el-avatar :size="80" :src="formData.avatar">
              {{ formData.name?.charAt(0) }}
            </el-avatar>
          </div>
        </el-form-item>
        
        <el-form-item label="国家/地区" prop="region">
          <el-input
            v-model="formData.region"
            placeholder="例如：中国台湾"
            maxlength="50"
          />
        </el-form-item>
        
        <el-form-item label="简介" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请输入歌手简介"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getArtistList, createArtist, updateArtist, deleteArtist } from '@/api/artist'

export default {
  name: 'ArtistManagement',
  setup() {
    const loading = ref(false)
    const submitLoading = ref(false)
    const artistList = ref([])
    const dialogVisible = ref(false)
    const dialogMode = ref('add') // 'add' or 'edit'
    const formRef = ref(null)
    
    // 分页
    const pagination = reactive({
      page: 1,
      size: 10,
      total: 0
    })
    
    // 表单数据
    const formData = reactive({
      id: null,
      name: '',
      avatar: '',
      region: '',
      description: ''
    })
    
    // 表单验证规则
    const formRules = {
      name: [
        { required: true, message: '请输入歌手名称', trigger: 'blur' },
        { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' }
      ]
    }
    
    // 获取歌手列表
    const fetchArtists = async () => {
      loading.value = true
      try {
        const res = await getArtistList({
          page: pagination.page - 1, // 后端从 0 开始
          size: pagination.size
        })
        artistList.value = res.data.content || []
        pagination.total = res.data.totalElements || 0
      } catch (error) {
        ElMessage.error('获取歌手列表失败')
      } finally {
        loading.value = false
      }
    }
    
    // 显示新增对话框
    const showAddDialog = () => {
      dialogMode.value = 'add'
      resetForm()
      dialogVisible.value = true
    }
    
    // 显示编辑对话框
    const showEditDialog = (row) => {
      dialogMode.value = 'edit'
      formData.id = row.id
      formData.name = row.name
      formData.avatar = row.avatar || ''
      formData.region = row.region || ''
      formData.description = row.description || ''
      dialogVisible.value = true
    }
    
    // 重置表单
    const resetForm = () => {
      formData.id = null
      formData.name = ''
      formData.avatar = ''
      formData.region = ''
      formData.description = ''
      if (formRef.value) {
        formRef.value.clearValidate()
      }
    }
    
    // 提交表单
    const handleSubmit = async () => {
      if (!formRef.value) return
      
      await formRef.value.validate(async (valid) => {
        if (!valid) return
        
        try {
          submitLoading.value = true
          
          const data = {
            name: formData.name,
            avatar: formData.avatar || null,
            region: formData.region || null,
            description: formData.description || null
          }
          
          if (dialogMode.value === 'add') {
            await createArtist(data)
            ElMessage.success('新增成功')
          } else {
            await updateArtist(formData.id, data)
            ElMessage.success('更新成功')
          }
          
          dialogVisible.value = false
          fetchArtists()
        } catch (error) {
          ElMessage.error(error.message || '操作失败')
        } finally {
          submitLoading.value = false
        }
      })
    }
    
    // 删除歌手
    const handleDelete = async (id) => {
      try {
        await deleteArtist(id)
        ElMessage.success('删除成功')
        fetchArtists()
      } catch (error) {
        ElMessage.error(error.message || '删除失败')
      }
    }
    
    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      return date.toLocaleString('zh-CN')
    }
    
    // 初始化
    onMounted(() => {
      fetchArtists()
    })
    
    return {
      loading,
      submitLoading,
      artistList,
      pagination,
      dialogVisible,
      dialogMode,
      formRef,
      formData,
      formRules,
      fetchArtists,
      showAddDialog,
      showEditDialog,
      handleSubmit,
      handleDelete,
      formatDate
    }
  }
}
</script>

<style scoped>
.artist-management {
  padding: 20px;
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}
</style>

