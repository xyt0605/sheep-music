<template>
  <div class="artists-page">
    <div class="page-header">
      <h2>全部歌手</h2>
      <p class="subtitle">
        发现你喜欢的音乐人
      </p>
    </div>
    
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索歌手名称..."
        clearable
        size="large"
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>
    
    <!-- 排序选项 -->
    <div class="sort-bar">
      <span class="sort-label">排序：</span>
      <el-radio-group
        v-model="sortType"
        size="small"
        @change="handleSortChange"
      >
        <el-radio-button value="songCount">
          歌曲数量
        </el-radio-button>
        <el-radio-button value="createTime">
          最新
        </el-radio-button>
        <el-radio-button value="name">
          名称
        </el-radio-button>
      </el-radio-group>
    </div>
    
    <!-- 加载状态 -->
    <div
      v-if="loading"
      class="loading-container"
    >
      <el-icon
        class="is-loading"
        :size="40"
      >
        <Loading />
      </el-icon>
      <p>加载中...</p>
    </div>
    
    <!-- 歌手网格 -->
    <div
      v-else-if="artists.length > 0"
      class="artists-grid"
    >
      <div 
        v-for="artist in artists" 
        :key="artist.id"
        class="artist-card"
        @click="goToArtistDetail(artist.id)"
      >
        <div class="artist-avatar-wrapper">
          <el-avatar 
            :src="ossThumb(artist.avatar, 200) || defaultAvatar" 
            :size="150"
            class="artist-avatar"
          >
            <el-icon :size="60">
              <UserFilled />
            </el-icon>
          </el-avatar>
          <div class="artist-overlay">
            <el-icon class="play-icon">
              <CaretRight />
            </el-icon>
          </div>
        </div>
        <div class="artist-info">
          <h3
            class="artist-name"
            :title="artist.name"
          >
            {{ artist.name }}
          </h3>
          <p
            class="artist-description"
            :title="artist.description"
          >
            {{ artist.description || '暂无简介' }}
          </p>
        </div>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div
      v-else
      class="empty-state"
    >
      <el-empty :description="searchKeyword ? '未找到相关歌手' : '暂无歌手'">
        <template #image>
          <el-icon style="font-size: 80px; color: #ddd;">
            <User />
          </el-icon>
        </template>
      </el-empty>
    </div>
    
    <!-- 分页 -->
    <div
      v-if="total > pageSize"
      class="pagination"
    >
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 36, 48]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Loading, User, UserFilled, CaretRight } from '@element-plus/icons-vue'
import { getArtists, searchArtists } from '@/api/artist'

import { ossThumb } from '@/utils/image'

export default {
  name: 'Artists',
  components: {
    Search,
    Loading,
    User,
    UserFilled,
    CaretRight
  },
  setup() {
    const router = useRouter()
    
    // 数据
    const artists = ref([])
    const loading = ref(false)
    const searchKeyword = ref('')
    const sortType = ref('songCount')  // 默认按歌曲数量排序
    const currentPage = ref(1)
    const pageSize = ref(24)
    const total = ref(0)
    const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
    
    let searchTimer = null
    
    // 加载歌手列表
    const loadArtists = async () => {
      try {
        loading.value = true
        const params = {
          page: currentPage.value - 1, // 后端从0开始
          size: pageSize.value,
          sortBy: sortType.value,
          order: sortType.value === 'name' ? 'asc' : 'desc'
        }
        
        const res = await getArtists(params)
        
        if (res.code === 200 && res.data) {
          artists.value = res.data.content || []
          total.value = res.data.totalElements || 0
        } else {
          throw new Error(res.message || '获取歌手列表失败')
        }
      } catch (error) {
        console.error('加载歌手列表失败:', error)
        ElMessage.error(error.message || '加载歌手列表失败')
        artists.value = []
        total.value = 0
      } finally {
        loading.value = false
      }
    }
    
    // 搜索歌手
    const performSearch = async () => {
      if (!searchKeyword.value.trim()) {
        // 清空搜索时重新加载列表
        currentPage.value = 1
        loadArtists()
        return
      }
      
      try {
        loading.value = true
        const res = await searchArtists(searchKeyword.value.trim())
        
        if (res.code === 200 && res.data) {
          artists.value = res.data
          total.value = res.data.length
        } else {
          throw new Error(res.message || '搜索失败')
        }
      } catch (error) {
        console.error('搜索歌手失败:', error)
        ElMessage.error(error.message || '搜索歌手失败')
        artists.value = []
        total.value = 0
      } finally {
        loading.value = false
      }
    }
    
    // 处理搜索（防抖）
    const handleSearch = () => {
      if (searchTimer) {
        clearTimeout(searchTimer)
      }
      searchTimer = setTimeout(() => {
        performSearch()
      }, 500)
    }
    
    // 处理排序变化
    const handleSortChange = () => {
      if (!searchKeyword.value) {
        currentPage.value = 1
        loadArtists()
      }
    }
    
    // 处理页码变化
    const handlePageChange = (page) => {
      currentPage.value = page
      if (!searchKeyword.value) {
        loadArtists()
      }
      // 滚动到顶部
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
    
    // 处理每页数量变化
    const handleSizeChange = (size) => {
      pageSize.value = size
      currentPage.value = 1
      if (!searchKeyword.value) {
        loadArtists()
      }
    }
    
    // 跳转到歌手详情
    const goToArtistDetail = (id) => {
      router.push(`/artist/${id}`)
    }
    
    // 初始化
    onMounted(() => {
      loadArtists()
    })

    // 组件卸载时清掉未触发的防抖定时器，避免卸载后仍发起请求
    onUnmounted(() => {
      if (searchTimer) {
        clearTimeout(searchTimer)
        searchTimer = null
      }
    })
    
    return {
      artists,
      loading,
      searchKeyword,
      sortType,
      currentPage,
      pageSize,
      total,
      defaultAvatar,
      handleSearch,
      handleSortChange,
      handlePageChange,
      handleSizeChange,
      goToArtistDetail
    }
  }
}
</script>

<style scoped>
.artists-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h2 {
  font-size: 32px;
  margin: 0 0 10px 0;
  color: #333;
}

.subtitle {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.search-bar {
  max-width: 600px;
  margin: 0 auto 20px;
}

.sort-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30px;
  gap: 10px;
}

.sort-label {
  color: #666;
  font-size: 14px;
}

.loading-container {
  text-align: center;
  padding: 80px 20px;
  color: #999;
}

.loading-container p {
  margin-top: 16px;
  font-size: 14px;
}

.artists-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}

.artist-card {
  cursor: pointer;
  transition: transform 0.3s;
  text-align: center;
}

.artist-card:hover {
  transform: translateY(-8px);
}

.artist-avatar-wrapper {
  position: relative;
  margin-bottom: 12px;
  display: inline-block;
}

.artist-avatar {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: box-shadow 0.3s;
}

.artist-card:hover .artist-avatar {
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.25);
}

.artist-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.artist-card:hover .artist-overlay {
  opacity: 1;
}

.play-icon {
  font-size: 48px;
  color: white;
}

.artist-info {
  padding: 0 8px;
}

.artist-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.artist-description {
  font-size: 13px;
  color: #999;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
  min-height: 36px;
}

.empty-state {
  padding: 80px 20px;
  text-align: center;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .artists-page {
    padding: 15px;
  }
  
  .page-header h2 {
    font-size: 24px;
  }
  
  .artists-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 16px;
  }
  
  .artist-avatar-wrapper :deep(.el-avatar) {
    width: 120px !important;
    height: 120px !important;
  }
  
  .artist-name {
    font-size: 14px;
  }
  
  .artist-description {
    font-size: 12px;
  }
  
  .sort-bar {
    flex-direction: column;
    gap: 8px;
  }
}

@media (max-width: 480px) {
  .artists-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .artist-avatar-wrapper :deep(.el-avatar) {
    width: 100px !important;
    height: 100px !important;
  }
  
  .play-icon {
    font-size: 32px;
  }
}
</style>

