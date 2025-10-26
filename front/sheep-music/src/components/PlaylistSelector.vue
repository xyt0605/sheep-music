<template>
  <el-dialog
    v-model="visible"
    title="添加到歌单"
    width="500px"
    @close="handleClose"
  >
    <div class="playlist-selector">
      <div class="create-new">
        <el-button type="primary" size="small" @click="showCreateDialog">
          <el-icon><Plus /></el-icon> 创建新歌单
        </el-button>
      </div>
      
      <div v-if="loading" class="loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      
      <div v-else-if="playlists.length > 0" class="playlist-list">
        <div 
          v-for="playlist in playlists" 
          :key="playlist.id"
          class="playlist-item"
          :class="{ 'disabled': playlist.disabled }"
          @click="selectPlaylist(playlist)"
        >
          <PlaylistCover 
            :cover="playlist.cover" 
            :size="50" 
            :default-cover="defaultCover"
            class="playlist-cover"
          />
          <div class="playlist-info">
            <div class="playlist-name">{{ playlist.name }}</div>
            <div class="playlist-count">{{ playlist.songCount || 0 }} 首歌曲</div>
          </div>
          <el-icon v-if="playlist.disabled" class="check-icon" color="#67C23A"><Check /></el-icon>
        </div>
      </div>
      
      <div v-else class="empty-state">
        <el-empty description="还没有歌单">
          <el-button type="primary" @click="showCreateDialog">创建第一个歌单</el-button>
        </el-empty>
      </div>
    </div>
    
    <!-- 创建歌单对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="创建歌单"
      width="400px"
      append-to-body
    >
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="歌单名称" required>
          <el-input 
            v-model="createForm.name" 
            placeholder="请输入歌单名称"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="createForm.category" placeholder="选择分类">
            <el-option label="流行" value="流行" />
            <el-option label="摇滚" value="摇滚" />
            <el-option label="民谣" value="民谣" />
            <el-option label="电子" value="电子" />
            <el-option label="纯音乐" value="纯音乐" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script>
import { ref, watch } from 'vue'
import { getMyAllPlaylists, createPlaylist, addSongToPlaylist } from '@/api/playlist'
import { ElMessage } from 'element-plus'
import { Plus, Loading, Check } from '@element-plus/icons-vue'
import PlaylistCover from './PlaylistCover.vue'

export default {
  name: 'PlaylistSelector',
  components: {
    PlaylistCover,
    Plus,
    Loading,
    Check
  },
  props: {
    modelValue: Boolean,
    songId: {
      type: Number,
      default: null
    }
  },
  emits: ['update:modelValue', 'success'],
  setup(props, { emit }) {
    const visible = ref(false)
    const loading = ref(false)
    const playlists = ref([])
    const defaultCover = 'https://via.placeholder.com/60?text=Playlist'
    
    const createDialogVisible = ref(false)
    const createForm = ref({
      name: '',
      category: ''
    })
    
    // 监听 modelValue 变化
    watch(() => props.modelValue, (val) => {
      visible.value = val
      if (val) {
        loadPlaylists()
      }
    })
    
    // 监听 visible 变化
    watch(visible, (val) => {
      emit('update:modelValue', val)
    })
    
    // 加载歌单列表
    const loadPlaylists = async () => {
      loading.value = true
      try {
        const res = await getMyAllPlaylists()
        if (res.code === 200) {
          playlists.value = res.data || []
        }
      } catch (error) {
        console.error('加载歌单失败:', error)
      } finally {
        loading.value = false
      }
    }
    
    // 选择歌单
    const selectPlaylist = async (playlist) => {
      if (playlist.disabled || !props.songId) return
      
      try {
        const res = await addSongToPlaylist(playlist.id, props.songId)
        if (res.code === 200) {
          ElMessage.success('已添加到歌单')
          emit('success')
          visible.value = false
        }
      } catch (error) {
        const errorMsg = error.response?.data?.message || '添加失败'
        if (errorMsg.includes('已在歌单中')) {
          // 标记为已添加
          playlist.disabled = true
          ElMessage.info('该歌曲已在此歌单中')
        } else {
          ElMessage.error(errorMsg)
        }
      }
    }
    
    // 显示创建对话框
    const showCreateDialog = () => {
      createForm.value = {
        name: '',
        category: ''
      }
      createDialogVisible.value = true
    }
    
    // 创建歌单
    const handleCreate = async () => {
      if (!createForm.value.name.trim()) {
        ElMessage.warning('请输入歌单名称')
        return
      }
      
      try {
        const res = await createPlaylist({
          name: createForm.value.name,
          category: createForm.value.category,
          isPublic: false
        })
        
        if (res.code === 200) {
          ElMessage.success('创建成功')
          createDialogVisible.value = false
          
          // 重新加载歌单列表
          await loadPlaylists()
          
          // 自动添加到新创建的歌单
          if (props.songId && res.data) {
            selectPlaylist(res.data)
          }
        }
      } catch (error) {
        ElMessage.error('创建失败')
      }
    }
    
    // 关闭对话框
    const handleClose = () => {
      visible.value = false
    }
    
    return {
      visible,
      loading,
      playlists,
      defaultCover,
      createDialogVisible,
      createForm,
      loadPlaylists,
      selectPlaylist,
      showCreateDialog,
      handleCreate,
      handleClose
    }
  }
}
</script>

<style scoped>
.playlist-selector {
  min-height: 300px;
  max-height: 500px;
}

.create-new {
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #409eff;
  font-size: 14px;
  gap: 10px;
}

.loading .el-icon {
  font-size: 32px;
}

.playlist-list {
  max-height: 400px;
  overflow-y: auto;
}

.playlist-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  gap: 12px;
}

.playlist-item:hover:not(.disabled) {
  background: #f5f7fa;
}

.playlist-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.playlist-cover {
  flex-shrink: 0;
}

.playlist-info {
  flex: 1;
  min-width: 0;
}

.playlist-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playlist-count {
  font-size: 12px;
  color: #999;
}

.check-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.empty-state {
  padding: 60px 0;
}
</style>

