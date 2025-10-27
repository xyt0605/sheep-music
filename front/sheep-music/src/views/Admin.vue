<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>🔧 管理后台</h2>
      <p class="subtitle">系统管理与数据维护</p>
    </div>
    
    <el-alert
      title="管理员专属区域"
      type="success"
      :closable="false"
      show-icon
      style="margin-bottom: 30px;"
    >
      <template #default>
        欢迎 <strong>{{ userStore.userInfo?.nickname }}</strong>，你当前拥有管理员权限
      </template>
    </el-alert>
    
    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" type="card">
      <!-- 歌手管理 -->
      <el-tab-pane label="🎤 歌手管理" name="artist">
        <ArtistManagement />
      </el-tab-pane>
      
      <!-- 歌曲管理 -->
      <el-tab-pane label="🎵 歌曲管理" name="song">
        <SongManagement />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useUserStore } from '@/store/user'
import ArtistManagement from '@/components/admin/ArtistManagement.vue'
import SongManagement from '@/components/admin/SongManagement.vue'

export default {
  name: 'Admin',
  components: {
    ArtistManagement,
    SongManagement
  },
  setup() {
    const userStore = useUserStore()
    const activeTab = ref('artist')
    
    return {
      userStore,
      activeTab
    }
  }
}
</script>

<style scoped>
.admin-page {
  padding: 30px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h2 {
  font-size: 28px;
  color: #333;
  margin-bottom: 10px;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

/* 响应式 - 全局样式已覆盖 */
@media (max-width: 768px) {
  .admin-page {
    padding: 15px 10px;
  }
  
  .el-tabs {
    font-size: 14px;
  }
}
</style>
