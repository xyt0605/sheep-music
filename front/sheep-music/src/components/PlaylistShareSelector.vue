<template>
  <el-dialog
    v-model="visible"
    title="选择歌单"
    width="600px"
    @close="handleClose"
  >
    <div class="playlist-selector">
      <!-- 搜索框 -->
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索歌单名称"
          prefix-icon="Search"
          clearable
        />
      </div>
      
      <div v-loading="loading" class="playlist-list">
        <div v-if="filteredPlaylists.length === 0 && !loading" class="empty-state">
          <el-empty description="还没有歌单">
            <el-button type="primary" @click="goToCreatePlaylist">创建第一个歌单</el-button>
          </el-empty>
        </div>
        
        <div
          v-for="playlist in filteredPlaylists"
          :key="playlist.id"
          class="playlist-item"
          @click="selectPlaylist(playlist)"
        >
          <el-image 
            :src="playlist.cover" 
            fit="cover"
            class="playlist-cover"
          >
            <template #error>
              <div class="image-error">
                <el-icon><List /></el-icon>
              </div>
            </template>
          </el-image>
          <div class="playlist-info">
            <div class="playlist-name">{{ playlist.name }}</div>
            <div class="playlist-desc">
              <span>{{ playlist.songCount || 0 }} 首歌曲</span>
              <span v-if="playlist.playCount"> · {{ formatPlayCount(playlist.playCount) }} 次播放</span>
            </div>
          </div>
          <el-button type="primary" size="small">选择</el-button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { List } from '@element-plus/icons-vue'
import { getMyAllPlaylists } from '@/api/playlist'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'select'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const playlists = ref([])
const searchKeyword = ref('')

// 过滤歌单
const filteredPlaylists = computed(() => {
  if (!searchKeyword.value) {
    return playlists.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return playlists.value.filter(p =>
    p.name.toLowerCase().includes(keyword)
  )
})

// 格式化播放次数
const formatPlayCount = (count) => {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万'
  }
  return count
}

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
    ElMessage.error('加载歌单失败')
  } finally {
    loading.value = false
  }
}

// 选择歌单
const selectPlaylist = (playlist) => {
  if (!playlist) return
  emit('select', playlist)
  visible.value = false
}

// 跳转到创建歌单
const goToCreatePlaylist = () => {
  ElMessage.info('请先在"我的歌单"页面创建歌单')
  visible.value = false
}

// 关闭对话框
const handleClose = () => {
  searchKeyword.value = ''
}

// 监听对话框打开
watch(visible, (val) => {
  if (val) {
    loadPlaylists()
  } else {
    handleClose()
  }
})
</script>

<style scoped>
.playlist-selector {
  min-height: 400px;
  max-height: 600px;
}

.search-box {
  margin-bottom: 16px;
}

.playlist-list {
  max-height: 500px;
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
  margin-bottom: 8px;
}

.playlist-item:hover {
  background: #f5f7fa;
}

.playlist-cover {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  flex-shrink: 0;
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 24px;
}

.playlist-info {
  flex: 1;
  min-width: 0;
}

.playlist-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playlist-desc {
  font-size: 12px;
  color: #999;
}

.empty-state {
  padding: 60px 0;
  text-align: center;
}
</style>
