<template>
  <div class="app-layout">
    <div class="galaxy-bg"></div>
    <!-- 顶部导航栏 -->
    <header class="app-header">
      <div class="header-content">
        <!-- Logo -->
        <div class="logo" @click="goHome">
          <span class="logo-icon">🎵</span>
          <span class="logo-text">Sheep Music</span>
        </div>
        
        <!-- 导航菜单（桌面端） -->
        <nav class="nav-menu desktop-menu">
          <router-link 
            v-for="item in menuItems" 
            :key="item.path" 
            :to="item.path"
            class="nav-item"
            active-class="active"
          >
            <span v-if="item.icon" class="menu-icon">{{ item.icon }}</span>
            {{ item.name }}
            <el-badge
              v-if="item.badge && item.badge > 0"
              :value="item.badge"
              :max="99"
              :is-dot="false"
              class="menu-badge"
            />
          </router-link>
        </nav>
        
        <!-- 移动端菜单按钮 -->
        <div class="mobile-menu-btn" @click="toggleMobileMenu">
          <span class="menu-icon">☰</span>
        </div>
        
        <!-- 右侧用户信息 -->
        <div class="user-section">
          <!-- 桌面歌词按钮 -->
          <el-tooltip content="桌面歌词" placement="bottom">
            <el-button 
              :icon="desktopLyricVisible ? 'ChatLineSquare' : 'ChatDotSquare'"
              circle 
              @click="toggleDesktopLyric"
              :type="desktopLyricVisible ? 'primary' : ''"
              class="lyric-toggle-btn"
            />
          </el-tooltip>
          
          <!-- 主题切换按钮 -->
          <ThemeToggle />
          
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
    
    <!-- 移动端侧边栏菜单 -->
    <transition name="slide">
      <div v-if="mobileMenuOpen" class="mobile-menu-overlay" @click="toggleMobileMenu">
        <nav class="mobile-menu" @click.stop>
          <div class="mobile-menu-header">
            <span class="menu-title">菜单</span>
            <span class="close-btn" @click="toggleMobileMenu">✕</span>
          </div>
          <router-link 
            v-for="item in menuItems" 
            :key="item.path" 
            :to="item.path"
            class="mobile-nav-item"
            active-class="active"
            @click="toggleMobileMenu"
          >
            <span v-if="item.icon" class="menu-icon">{{ item.icon }}</span>
            {{ item.name }}
            <el-badge
              v-if="item.badge && item.badge > 0"
              :value="item.badge"
              :max="99"
              class="mobile-menu-badge"
            />
          </router-link>
        </nav>
      </div>
    </transition>
    
    <!-- 主内容区 -->
    <main class="app-main">
      <router-view v-slot="{ Component, route }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </main>
    
    <!-- 桌面歌词 -->
    <DesktopLyric ref="desktopLyricRef" />
  </div>
</template>

<script>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { usePlayerStore } from '@/store/player'
import { useSocialStore } from '@/store/social'
import ThemeToggle from '@/components/ThemeToggle.vue'
import DesktopLyric from '@/components/DesktopLyric.vue'
import wsClient from '@/ws/client'
import { notifyInfo } from '@/utils/message'

export default {
  name: 'Layout',
  components: {
    ThemeToggle,
    DesktopLyric
  },
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    const playerStore = usePlayerStore()
    const socialStore = useSocialStore()
    const mobileMenuOpen = ref(false)
    const desktopLyricRef = ref(null)
    const desktopLyricVisible = ref(false)
    
    // 消息提示音
    const playNotificationSound = () => {
      try {
        // 使用 Web Audio API 生成简单的提示音
        const audioContext = new (window.AudioContext || window.webkitAudioContext)()
        const oscillator = audioContext.createOscillator()
        const gainNode = audioContext.createGain()
        
        oscillator.connect(gainNode)
        gainNode.connect(audioContext.destination)
        
        oscillator.frequency.value = 800 // 频率
        oscillator.type = 'sine' // 波形
        
        gainNode.gain.setValueAtTime(0.3, audioContext.currentTime)
        gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.2)
        
        oscillator.start(audioContext.currentTime)
        oscillator.stop(audioContext.currentTime + 0.2)
      } catch (error) {
        console.warn('播放提示音失败:', error)
      }
    }
    
    // 显示桌面通知
    const showDesktopNotification = (title, body, icon) => {
      // 检查浏览器是否支持通知
      if (!('Notification' in window)) {
        return
      }
      
      // 检查权限
      if (Notification.permission === 'granted') {
        new Notification(title, {
          body,
          icon: icon || '/logo.png',
          badge: '/logo.png',
          tag: 'chat-message', // 相同tag的通知会替换旧的
          requireInteraction: false,
          silent: true // 静音，因为我们有自己的提示音
        })
      } else if (Notification.permission !== 'denied') {
        // 请求权限
        Notification.requestPermission().then(permission => {
          if (permission === 'granted') {
            new Notification(title, {
              body,
              icon: icon || '/logo.png',
              badge: '/logo.png',
              tag: 'chat-message',
              requireInteraction: false,
              silent: true
            })
          }
        })
      }
    }
    
    // 根据角色动态生成菜单
    const menuItems = computed(() => {
      const baseMenu = [
        { name: '首页', path: '/home', icon: '🏠' },
        { name: '发现', path: '/discover', icon: '✨' },
        { name: '搜索', path: '/search', icon: '🔍' },
        { name: '歌手', path: '/artists', icon: '🎤' },
        { name: '歌单广场', path: '/playlist', icon: '📃' },
        { name: '排行榜', path: '/rank', icon: '📊' },
        { name: '我的音乐', path: '/my-music' },
        { name: '好友', path: '/friends', icon: '👥', badge: socialStore.friendRequestCount },
        { name: '聊天', path: '/chat', icon: '💬', badge: socialStore.unreadMessageCount },
        { name: '动态', path: '/moments', icon: '📱' },
        { name: '分享广场', path: '/share-square', icon: '🔗' }
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
    
    // 初始化社交数据
    onMounted(() => {
      if (userStore.isLogin) {
        // 异步初始化，不阻塞页面加载
        socialStore.initSocialData().catch(error => {
          console.error('初始化社交数据失败:', error)
        })

        // 初始化 WebSocket 连接与订阅
        const token = localStorage.getItem('token')
        const uid = userStore.userInfo?.id
        if (uid && token) {
          wsClient.connect({ userId: uid, token })

          // 聊天消息到达：智能处理未读数和通知
          const offChat = wsClient.onChatMessage(async (msg) => {
            try {
              if (!msg) return
              
              // 判断是否在当前聊天窗口
              const currentRoute = router.currentRoute.value
              const isInChatPage = currentRoute.path && currentRoute.path.startsWith('/chat')
              const currentChatFriendId = currentRoute.params.id  // 修复：参数名是 id 而不是 friendId
              const isCurrentChat = isInChatPage && currentChatFriendId &&
                (msg.senderId == currentChatFriendId || msg.receiverId == currentChatFriendId)
              
              // 添加小延迟，给网络传输时间
              // 后端已在事务提交后才推送消息，所以不需要太长延迟
              await new Promise(resolve => setTimeout(resolve, 200))
              
              // 始终更新未读消息数（导航栏的红点）
              await socialStore.updateUnreadMessageCount()
              
              // 如果不在当前聊天窗口，才显示通知
              if (!isCurrentChat) {
                
                // 显示消息通知（仅当消息是发给自己的）
                if (msg.receiverId === userStore.userInfo?.id) {
                  const senderName = msg.senderName || '好友'
                  let content = msg.content || ''
                  
                  // 根据消息类型显示不同内容
                  if (msg.type === 'song') {
                    content = '[分享了一首歌曲]'
                  } else if (msg.type === 'playlist') {
                    content = '[分享了一个歌单]'
                  } else if (content.length > 20) {
                    content = content.substring(0, 20) + '...'
                  }
                  
                  // 显示应用内通知
                  notifyInfo('新消息', `${senderName}：${content}`)
                  
                  // 显示桌面通知
                  showDesktopNotification('新消息', `${senderName}：${content}`, msg.senderAvatar)
                  
                  // 播放提示音
                  playNotificationSound()
                }
              }
            } catch (e) {
              console.error('处理聊天消息失败:', e)
            }
          })

          // 通知消息：刷新未读通知徽标（避免重复订阅，记录取消函数）
          const offNotify = wsClient.onNotification(async (data) => {
            if (data && data.type === 'notification_unread_count') {
              socialStore.unreadNotificationCount = data.unreadCount || 0
            } else {
              await socialStore.updateUnreadNotificationCount().catch(() => {})
            }
          })

          // 组件卸载时，移除订阅，防止重复弹窗
          onUnmounted(() => {
            try { offChat && offChat() } catch (_) {}
            try { offNotify && offNotify() } catch (_) {}
          })
        }

        // 定期更新未读数
        const intervalId = setInterval(() => {
          if (userStore.isLogin) {
            socialStore.updateUnreadMessageCount().catch(error => {
              console.error('更新未读消息数失败:', error)
            })
            socialStore.updateUnreadNotificationCount().catch(error => {
              console.error('更新未读通知数失败:', error)
            })
          } else {
            clearInterval(intervalId)
          }
        }, 30000) // 每30秒更新一次

        // 组件卸载时清理定时器，避免重复创建
        onUnmounted(() => {
          try { clearInterval(intervalId) } catch (_) {}
        })
      }
    })
    
    // 切换移动端菜单
    const toggleMobileMenu = () => {
      mobileMenuOpen.value = !mobileMenuOpen.value
    }
    
    // 回到首页
    const goHome = () => {
      router.push('/home')
    }
    
    // 切换桌面歌词
    const toggleDesktopLyric = () => {
      if (desktopLyricRef.value) {
        desktopLyricRef.value.toggle()
        desktopLyricVisible.value = desktopLyricRef.value.visible
      }
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
            // 断开 WebSocket 连接
            try { wsClient.disconnect() } catch (e) {}
            // 清空播放器状态
            playerStore.clearPlayer()
            // 退出登录
            userStore.logout()
            ElMessage.success('已退出登录')
            router.push('/login')
          }).catch(() => {})
          break
      }
    }
    
    return {
      userStore,
      socialStore,
      menuItems,
      mobileMenuOpen,
      desktopLyricRef,
      desktopLyricVisible,
      toggleMobileMenu,
      toggleDesktopLyric,
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
  background-color: var(--bg-secondary);
  background-image: radial-gradient(circle at 1% 1%, rgba(102,126,234,0.06) 0%, transparent 25%),
                    radial-gradient(circle at 99% 99%, rgba(118,75,162,0.05) 0%, transparent 25%);
  position: relative;
  z-index: 1;
}

.app-layout::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  display: none;
}

.galaxy-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background:
    linear-gradient(180deg, #f7fafc 0%, #eef2f7 100%),
    repeating-linear-gradient(45deg, rgba(0,0,0,0.02) 0px, rgba(0,0,0,0.02) 2px, transparent 2px, transparent 6px),
    radial-gradient(400px 200px at 20% 30%, rgba(102,126,234,0.08), transparent 60%),
    radial-gradient(500px 250px at 80% 20%, rgba(118,75,162,0.06), transparent 65%);
}

/* 顶部导航栏 */
.app-header {
  height: 64px;
  background: var(--card-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-color-light);
  box-shadow: 0 8px 32px rgba(0,0,0,0.06);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  transition: all var(--transition-base);
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
  gap: 12px;
  color: var(--text-primary);
  text-shadow: 0 1px 2px rgba(0,0,0,0.25);
  font-size: 22px;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-base);
  padding: 8px 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
  flex-shrink: 0;
}

.logo:hover {
  transform: scale(1.05);
}

.logo-icon {
  font-size: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 2px 4px rgba(102, 126, 234, 0.3));
}

/* 导航菜单 */
.nav-menu {
  display: flex;
  gap: 6px;
  flex: 1;
  margin-left: 24px;
  flex-wrap: nowrap;
  overflow-x: auto;
  overflow-y: hidden;
}

.nav-menu::-webkit-scrollbar {
  height: 2px;
}

.nav-menu::-webkit-scrollbar-track {
  background: transparent;
}

.nav-menu::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 2px;
}

.nav-item {
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: 20px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative;
  white-space: nowrap;
  flex-shrink: 0;
}

.nav-item:hover {
  background: rgba(0,0,0,0.03);
  color: var(--text-primary);
  transform: translateY(0);
}

.nav-item.active {
  background: var(--primary-light, #ecf5ff);
  color: var(--color-primary);
  font-weight: 600;
  box-shadow: none;
}

.nav-item.active::after {
  display: none;
}

.menu-icon {
  font-size: 16px;
  opacity: 0.8;
  display: none;
}

.nav-item:hover .menu-icon,
.nav-item.active .menu-icon {
  display: inline-block;
  opacity: 1;
}

.menu-badge {
  margin-left: 6px;
  vertical-align: middle;
}

.menu-badge :deep(.el-badge__content) {
  background-color: #f56c6c;
  border: none;
  font-size: 12px;
  height: 18px;
  line-height: 18px;
  padding: 0 6px;
  min-width: 18px;
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(245, 108, 108, 0.4);
}

.nav-item:hover .menu-badge :deep(.el-badge__content) {
  background-color: #f78989;
}

.nav-item.active .menu-badge :deep(.el-badge__content) {
  background-color: #ff4d4f;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

/* 用户区域 */
.user-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-section :deep(.el-button.is-circle) {
  border: 1px solid transparent;
  background: transparent;
  transition: all 0.2s ease;
}

.user-section :deep(.el-button.is-circle:hover) {
  transform: none;
  box-shadow: none;
  background: rgba(0,0,0,0.03);
  color: var(--primary-color);
}

.lyric-toggle-btn {
  transition: all var(--transition-base);
}

.lyric-toggle-btn:hover {
  transform: none;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 20px;
  transition: all 0.2s;
  background: transparent;
  border: 1px solid transparent;
}

.user-info:hover {
  background: rgba(0,0,0,0.03);
  border-color: transparent;
  transform: none;
  box-shadow: none;
}

.username {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
}

.admin-badge {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(245, 87, 108, 0.3);
  letter-spacing: 0.5px;
}

/* 主内容区 */
.app-main {
  flex: 1;
  margin-top: 64px;
  margin-bottom: 130px; /* 增加到底部的距离，为悬浮播放器留出空间 */
  max-width: 1400px;
  width: 100%;
  margin-left: auto;
  margin-right: auto;
  padding: 24px;
  position: relative;
  z-index: 1;
}

/* 移动端菜单按钮（默认隐藏） */
.mobile-menu-btn {
  display: none;
  color: var(--text-primary);
  font-size: 28px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 10px;
  transition: all var(--transition-base);
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
}

.mobile-menu-btn:hover {
  background: var(--bg-tertiary);
  transform: scale(1.05);
}

/* 移动端侧边栏菜单 */
.mobile-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  z-index: 2000;
  display: flex;
  justify-content: flex-start;
}

.mobile-menu {
  width: 300px;
  max-width: 85%;
  background: var(--card-bg);
  height: 100%;
  overflow-y: auto;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.2);
  border-right: 1px solid var(--border-color);
}

.mobile-menu-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border-bottom: 1px solid var(--border-color);
}

.menu-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.close-btn {
  font-size: 28px;
  cursor: pointer;
  padding: 6px 12px;
  transition: all var(--transition-base);
  color: var(--text-secondary);
  border-radius: 8px;
}

.close-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
  transform: rotate(90deg);
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 16px 24px;
  margin: 6px 12px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 16px;
  border-radius: 12px;
  transition: all var(--transition-base);
  font-weight: 500;
  white-space: nowrap;
}

.mobile-nav-item:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
  transform: translateX(4px);
}

.mobile-nav-item.active {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.15) 100%);
  color: var(--color-primary);
  font-weight: 600;
  border-left: 4px solid var(--color-primary);
  padding-left: 20px;
}

.mobile-nav-item .menu-icon {
  font-size: 22px;
  opacity: 0.8;
}

.mobile-nav-item:hover .menu-icon,
.mobile-nav-item.active .menu-icon {
  opacity: 1;
}

.mobile-menu-badge {
  margin-left: auto;
}

.mobile-menu-badge :deep(.el-badge__content) {
  background-color: #f56c6c;
  border: none;
  font-size: 12px;
  height: 20px;
  line-height: 20px;
  padding: 0 6px;
  min-width: 20px;
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(245, 108, 108, 0.4);
  position: static;
  transform: none;
}

/* 移动端菜单动画 */
.slide-enter-active,
.slide-leave-active {
  transition: opacity 0.3s;
}

.slide-enter-active .mobile-menu,
.slide-leave-active .mobile-menu {
  transition: transform 0.3s;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
}

.slide-enter-from .mobile-menu,
.slide-leave-to .mobile-menu {
  transform: translateX(-100%);
}

/* 平板适配 (768px - 1024px) */
@media (max-width: 1024px) {
  .header-content {
    padding: 0 16px;
  }
  
  .nav-menu {
    gap: 4px;
    margin-left: 16px;
  }
  
  .nav-item {
    font-size: 14px;
    padding: 8px 12px;
  }
  
  .app-main {
    padding: 16px;
  }
  
  .logo-text {
    font-size: 18px;
  }
  
  /* 移除图标，节省空间 */
  .nav-item .menu-icon {
    display: none;
  }
}

/* 移动端适配 (< 768px) */
@media (max-width: 768px) {
  /* 隐藏桌面端导航菜单 */
  .desktop-menu {
    display: none !important;
  }
  
  /* 显示移动端菜单按钮 */
  .mobile-menu-btn {
    display: block;
  }
  
  /* Logo 文字隐藏 */
  .logo-text {
    display: none;
  }
  
  .logo-icon {
    font-size: 28px;
  }
  
  .logo {
    padding: 6px 12px;
  }
  
  /* 用户信息简化 */
  .username {
    display: none;
  }
  
  .admin-badge {
    display: none;
  }
  
  .user-info {
    padding: 6px;
    background: transparent;
    border: none;
  }
  
  .user-section {
    gap: 8px;
  }
  
  .user-section :deep(.el-button.is-circle) {
    width: 36px;
    height: 36px;
  }
  
  /* 主内容区 */
  .app-main {
    padding: 12px;
    margin-top: 64px;
    margin-bottom: 100px;
  }
  
  /* 顶部导航栏 */
  .app-header {
    height: 56px;
  }
  
  .header-content {
    padding: 0 12px;
  }
}

/* 小屏手机适配 (< 480px) */
@media (max-width: 480px) {
  .app-header {
    height: 50px;
  }
  
  .logo-icon {
    font-size: 28px;
  }
  
  .app-main {
    padding: 8px;
    margin-top: 50px;
    margin-bottom: 90px;
  }
  
  .mobile-menu {
    width: 260px;
  }
}
</style>
