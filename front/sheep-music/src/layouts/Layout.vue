<template>
  <div class="app-layout">
    <!-- 顶部导航栏 -->
    <header class="app-header">
      <div class="header-content">
        <!-- Logo -->
        <div class="logo" @click="goHome">
          <span class="logo-icon">🎵</span>
          <span class="logo-text">Sheep Music</span>
        </div>
        
        <!-- 导航菜单 -->
        <nav class="nav-menu">
          <router-link 
            v-for="item in menuItems" 
            :key="item.path" 
            :to="item.path"
            class="nav-item"
            active-class="active"
          >
            <span v-if="item.icon" class="menu-icon">{{ item.icon }}</span>
            {{ item.name }}
          </router-link>
        </nav>
        
        <!-- 右侧用户信息 -->
        <div class="user-section">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :src="userStore.userInfo?.avatar" size="default">
                {{ userStore.userInfo?.nickname?.charAt(0) }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.nickname }}</span>
              <span v-if="userStore.isAdmin" class="admin-badge">管理员</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>
    
    <!-- 主内容区 -->
    <main class="app-main">
      <router-view />
    </main>
    
    <!-- 全局音乐播放器 -->
    <MusicPlayer />
  </div>
</template>

<script>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import MusicPlayer from '@/components/MusicPlayer.vue'

export default {
  name: 'Layout',
  components: {
    MusicPlayer
  },
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    
    // 根据角色动态生成菜单
    const menuItems = computed(() => {
      const baseMenu = [
        { name: '首页', path: '/home', icon: '🏠' },
        { name: '发现', path: '/discover', icon: '✨' },
        { name: '搜索', path: '/search', icon: '🔍' },
        { name: '歌单广场', path: '/playlist', icon: '📃' },
        { name: '排行榜', path: '/rank', icon: '📊' },
        { name: '我的音乐', path: '/my-music' }
      ]
      
      // 如果是管理员，添加管理后台
      if (userStore.isAdmin) {
        baseMenu.push({ 
          name: '管理后台', 
          path: '/admin', 
          icon: '🔧' 
        })
      }
      
      return baseMenu
    })
    
    // 回到首页
    const goHome = () => {
      router.push('/home')
    }
    
    // 下拉菜单操作
    const handleCommand = async (command) => {
      switch (command) {
        case 'profile':
          router.push('/profile')
          break
        case 'settings':
          ElMessage.info('设置功能待开发')
          break
        case 'logout':
          // 退出登录确认
          ElMessageBox.confirm('确定要退出登录吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            userStore.logout()
            ElMessage.success('已退出登录')
            router.push('/login')
          }).catch(() => {})
          break
      }
    }
    
    return {
      userStore,
      menuItems,
      goHome,
      handleCommand
    }
  }
}
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

/* 顶部导航栏 */
.app-header {
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 20px;
  justify-content: space-between;
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: white;
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
  transition: opacity 0.3s;
}

.logo:hover {
  opacity: 0.8;
}

.logo-icon {
  font-size: 28px;
}

/* 导航菜单 */
.nav-menu {
  display: flex;
  gap: 30px;
  flex: 1;
  margin-left: 50px;
}

.nav-item {
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  font-size: 15px;
  padding: 5px 10px;
  border-radius: 4px;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 5px;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.nav-item.active {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  font-weight: bold;
}

.menu-icon {
  font-size: 16px;
}

/* 用户区域 */
.user-section {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 5px 15px;
  border-radius: 20px;
  transition: background 0.3s;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.1);
}

.username {
  color: white;
  font-size: 14px;
}

.admin-badge {
  background: #ffd700;
  color: #333;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: bold;
}

/* 主内容区 */
.app-main {
  flex: 1;
  margin-top: 60px;
  margin-bottom: 100px;
  max-width: 1400px;
  width: 100%;
  margin-left: auto;
  margin-right: auto;
  padding: 20px;
}

/* 响应式 */
@media (max-width: 768px) {
  .nav-menu {
    display: none;
  }
  
  .logo-text {
    display: none;
  }
}
</style>