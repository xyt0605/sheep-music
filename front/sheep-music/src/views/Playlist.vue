<template>
  <div class="playlist-square">
    <div class="page-header">
      <h2>歌单广场</h2>
      <p class="subtitle">
        发现更多精彩歌单
      </p>
    </div>
    
    <!-- 分类筛选 -->
    <div class="category-filter">
      <el-radio-group
        v-model="selectedCategory"
        @change="handleCategoryChange"
      >
        <el-radio-button label="">
          全部
        </el-radio-button>
        <el-radio-button label="流行">
          流行
        </el-radio-button>
        <el-radio-button label="摇滚">
          摇滚
        </el-radio-button>
        <el-radio-button label="民谣">
          民谣
        </el-radio-button>
        <el-radio-button label="电子">
          电子
        </el-radio-button>
        <el-radio-button label="纯音乐">
          纯音乐
        </el-radio-button>
        <el-radio-button label="其他">
          其他
        </el-radio-button>
      </el-radio-group>
    </div>
    
    <!-- Tab切换 -->
    <el-tabs
      v-model="activeTab"
      @tab-change="handleTabChange"
    >
      <!-- 推荐歌单 -->
      <el-tab-pane name="recommend">
        <template #label>
          <span>
            推荐歌单
            <el-tag
              v-if="isLogin && !selectedCategory && currentPage === 1"
              size="small"
              type="success"
              style="margin-left: 8px;"
            >个性化</el-tag>
          </span>
        </template>
        
        <div
          v-if="loading"
          class="loading-container"
        >
          <el-icon class="is-loading">
            <Loading />
          </el-icon>
        </div>
        
        <div
          v-else-if="playlists.length > 0"
          class="playlist-grid"
        >
          <div 
            v-for="playlist in playlists" 
            :key="playlist.id"
            class="playlist-card"
            @click="goToPlaylistDetail(playlist.id)"
          >
            <div class="playlist-cover-wrapper">
              <PlaylistCover 
                :cover="playlist.cover" 
                :size="200" 
                :default-cover="defaultCover"
                class="playlist-cover"
              />
              <div class="playlist-overlay">
                <el-icon class="play-icon">
                  <CaretRight />
                </el-icon>
              </div>
            </div>
            <div class="playlist-info">
              <h4
                class="playlist-name"
                :title="playlist.name"
              >
                {{ playlist.name }}
              </h4>
              <p class="playlist-creator">
                <el-icon><User /></el-icon>
                {{ playlist.creator?.nickname || '未知' }}
              </p>
              <p class="playlist-stats">
                <span>
                  <el-icon><List /></el-icon>
                  {{ playlist.songCount || 0 }} 首
                </span>
                <span>
                  <el-icon><Headset /></el-icon>
                  {{ formatCount(playlist.playCount) }}
                </span>
              </p>
            </div>
          </div>
        </div>
        
        <div
          v-else
          class="empty-state"
        >
          <el-empty description="暂无歌单">
            <template #image>
              <el-icon style="font-size: 80px; color: #ddd;">
                <FolderOpened />
              </el-icon>
            </template>
            <template
              v-if="!isLogin"
              #description
            >
              <p>暂无推荐歌单</p>
              <el-button
                type="primary"
                @click="$router.push('/login')"
              >
                登录查看个性化推荐
              </el-button>
            </template>
          </el-empty>
        </div>
        
        <!-- 分页 -->
        <el-pagination
          v-if="total > pageSize"
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          class="pagination"
          @current-change="loadPlaylists"
        />
      </el-tab-pane>
      
      <!-- 热门歌单 -->
      <el-tab-pane
        label="热门歌单"
        name="hot"
      >
        <div
          v-if="loading"
          class="loading-container"
        >
          <el-icon class="is-loading">
            <Loading />
          </el-icon>
        </div>
        
        <div
          v-else-if="hotPlaylists.length > 0"
          class="playlist-grid"
        >
          <div 
            v-for="playlist in hotPlaylists" 
            :key="playlist.id"
            class="playlist-card"
            @click="goToPlaylistDetail(playlist.id)"
          >
            <div class="playlist-cover-wrapper">
              <PlaylistCover 
                :cover="playlist.cover" 
                :size="200" 
                :default-cover="defaultCover"
                class="playlist-cover"
              />
              <div class="playlist-overlay">
                <el-icon class="play-icon">
                  <CaretRight />
                </el-icon>
              </div>
            </div>
            <div class="playlist-info">
              <h4
                class="playlist-name"
                :title="playlist.name"
              >
                {{ playlist.name }}
              </h4>
              <p class="playlist-creator">
                <el-icon><User /></el-icon>
                {{ playlist.creator?.nickname || '未知' }}
              </p>
              <p class="playlist-stats">
                <span>
                  <el-icon><List /></el-icon>
                  {{ playlist.songCount || 0 }} 首
                </span>
                <span>
                  <el-icon><Headset /></el-icon>
                  {{ formatCount(playlist.playCount) }}
                </span>
              </p>
            </div>
          </div>
        </div>
        
        <div
          v-else
          class="empty-state"
        >
          <el-empty description="暂无热门歌单">
            <template #image>
              <el-icon style="font-size: 80px; color: #ddd;">
                <FolderOpened />
              </el-icon>
            </template>
          </el-empty>
        </div>
        
        <!-- 分页 -->
        <el-pagination
          v-if="hotTotal > hotPageSize"
          v-model:current-page="hotCurrentPage"
          :page-size="hotPageSize"
          :total="hotTotal"
          layout="prev, pager, next, total"
          class="pagination"
          @current-change="loadHotPlaylists"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getPublicPlaylists, getHotPlaylists } from '@/api/playlist'
import { getRecommendedPlaylists } from '@/api/recommendation'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { Loading, CaretRight, User, List, Headset, FolderOpened } from '@element-plus/icons-vue'
import PlaylistCover from '@/components/PlaylistCover.vue'

export default {
  name: 'PlaylistSquare',
  components: {
    PlaylistCover,
    Loading,
    CaretRight,
    User,
    List,
    Headset
  },
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    
    const activeTab = ref('recommend')
    const selectedCategory = ref('')
    const loading = ref(false)
    
    // 推荐歌单
    const playlists = ref([])
    const currentPage = ref(1)
    const pageSize = ref(20)
    const total = ref(0)
    
    // 热门歌单
    const hotPlaylists = ref([])
    const hotCurrentPage = ref(1)
    const hotPageSize = ref(20)
    const hotTotal = ref(0)
    
  const defaultCover = '/default-playlist.svg'
    
    // 判断是否登录
    const isLogin = computed(() => userStore.isLogin)
    
    // 加载推荐歌单
    const loadPlaylists = async () => {
      loading.value = true
      try {
        let res
        
        // 如果用户已登录，使用个性化推荐
        if (isLogin.value && !selectedCategory.value && currentPage.value === 1) {
          try {
            res = await getRecommendedPlaylists({ limit: pageSize.value })
            if (res.code === 200 && res.data && res.data.length > 0) {
              playlists.value = res.data
              total.value = res.data.length
              loading.value = false
              return
            }
          } catch (error) {
            console.log('个性化推荐失败，使用默认推荐:', error)
          }
        }
        
        // 否则使用公开歌单列表
        res = await getPublicPlaylists({
          page: currentPage.value - 1,
          size: pageSize.value,
          category: selectedCategory.value || undefined
        })
        
        if (res.code === 200) {
          playlists.value = res.data.content || []
          total.value = res.data.totalElements || 0
        } else {
          ElMessage.warning(res.message || '加载失败')
        }
      } catch (error) {
        console.error('加载歌单失败:', error)
        ElMessage.error('加载歌单失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
    
    // 加载热门歌单
    const loadHotPlaylists = async () => {
      loading.value = true
      try {
        const res = await getHotPlaylists({
          page: hotCurrentPage.value - 1,
          size: hotPageSize.value
        })
        
        if (res.code === 200) {
          hotPlaylists.value = res.data.content || []
          hotTotal.value = res.data.totalElements || 0
        } else {
          ElMessage.warning(res.message || '加载失败')
        }
      } catch (error) {
        console.error('加载热门歌单失败:', error)
        ElMessage.error('加载热门歌单失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
    
    // 处理分类变化
    const handleCategoryChange = () => {
      currentPage.value = 1
      if (activeTab.value === 'recommend') {
        loadPlaylists()
      }
    }
    
    // 处理Tab切换
    const handleTabChange = (tab) => {
      if (tab === 'recommend') {
        if (playlists.value.length === 0) {
          loadPlaylists()
        }
      } else if (tab === 'hot') {
        if (hotPlaylists.value.length === 0) {
          loadHotPlaylists()
        }
      }
    }
    
    // 跳转到歌单详情
    const goToPlaylistDetail = (playlistId) => {
      router.push(`/playlist/${playlistId}`)
    }
    
    // 格式化数量
    const formatCount = (count) => {
      if (!count) return 0
      if (count >= 10000) {
        return (count / 10000).toFixed(1) + '万'
      }
      return count
    }
    
    onMounted(() => {
      loadPlaylists()
    })
    
    return {
      activeTab,
      selectedCategory,
      loading,
      playlists,
      currentPage,
      pageSize,
      total,
      hotPlaylists,
      hotCurrentPage,
      hotPageSize,
      hotTotal,
      defaultCover,
      isLogin,
      loadPlaylists,
      loadHotPlaylists,
      handleCategoryChange,
      handleTabChange,
      goToPlaylistDetail,
      formatCount
    }
  }
}
</script>

<style scoped>
.playlist-square {
  padding: 30px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h2 {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

/* 分类筛选 */
.category-filter {
  margin-bottom: 30px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

/* 加载状态 */
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  font-size: 32px;
  color: #409eff;
}

/* 歌单网格 */
.playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.playlist-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.playlist-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.playlist-cover-wrapper {
  position: relative;
  width: 100%;
  padding-top: 100%;
  overflow: hidden;
  background: #f5f5f5;
}

.playlist-cover {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.playlist-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.playlist-card:hover .playlist-overlay {
  opacity: 1;
}

.play-icon {
  font-size: 48px;
  color: white;
}

.playlist-info {
  padding: 15px;
}

.playlist-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playlist-creator {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}

.playlist-stats {
  display: flex;
  align-items: center;
  gap: 15px;
  font-size: 12px;
  color: #999;
}

.playlist-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 空状态 */
.empty-state {
  padding: 60px 0;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

/* 响应式 */
@media (max-width: 768px) {
  .playlist-square {
    padding: 15px;
  }
  
  .page-header h2 {
    font-size: 24px;
  }
  
  .category-filter {
    padding: 15px;
    overflow-x: auto;
  }
  
  .category-filter :deep(.el-radio-group) {
    display: flex;
    flex-wrap: nowrap;
  }
  
  .playlist-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 15px;
  }
}
</style>
