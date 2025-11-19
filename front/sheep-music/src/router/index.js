import { createRouter, createWebHashHistory } from 'vue-router'
import Layout from '@/layouts/Layout.vue'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const routes = [
  // 欢迎页
  {
    path: '/welcome',
    name: 'Welcome',
    component: () => import('@/views/Welcome.vue'),
    meta: { requiresAuth: false }
  },
  
  // 登录
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { requiresAuth: false }
  },
  
  // 注册
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { requiresAuth: false }
  },
  
  // 主布局（包含导航栏和播放器）
  {
    path: '/',
    component: Layout,
    meta: { requiresAuth: true },
    children: [
      // 首页
      {
        path: '/home',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { requiresAuth: true }
      },
      
      // 歌单广场
      {
        path: '/playlist',
        name: 'Playlist',
        component: () => import('@/views/Playlist.vue'),
        meta: { requiresAuth: true }
      },
      
      // 排行榜
      {
        path: '/rank',
        name: 'Rank',
        component: () => import('@/views/Rank.vue'),
        meta: { requiresAuth: true }
      },
      
      // 全部歌手
      {
        path: '/artists',
        name: 'Artists',
        component: () => import('@/views/Artists.vue'),
        meta: { requiresAuth: true }
      },
      
      // 发现音乐（个性化推荐）
      {
        path: '/discover',
        name: 'Discover',
        component: () => import('@/views/Discover.vue'),
        meta: { requiresAuth: true }
      },
      
      // 搜索
      {
        path: '/search',
        name: 'Search',
        component: () => import('@/views/Search.vue'),
        meta: { requiresAuth: true }
      },
      
      // 我的音乐（集成：收藏、历史、歌单）
      {
        path: '/my-music',
        name: 'MyMusic',
        component: () => import('@/views/MyMusic.vue'),
        meta: { requiresAuth: true }
      },
      
      // 个人中心
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { requiresAuth: true }
      },
      
      // 歌手详情
      {
        path: '/artist/:id',
        name: 'ArtistDetail',
        component: () => import('@/views/ArtistDetail.vue'),
        meta: { requiresAuth: true }
      },
      
      // 歌单详情
      {
        path: '/playlist/:id',
        name: 'PlaylistDetail',
        component: () => import('@/views/PlaylistDetail.vue'),
        meta: { requiresAuth: true }
      },
      
      // 管理后台（只有管理员可访问）
      {
        path: '/admin',
        name: 'Admin',
        component: () => import('@/views/Admin.vue'),
        meta: { 
          requiresAuth: true,
          requiresAdmin: true  // 需要管理员权限
        }
      },
      
      // 社交功能
      {
        path: '/friends',
        name: 'Friends',
        component: () => import('@/views/Social/Friends.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: '/chat/:friendId?',
        name: 'Chat',
        component: () => import('@/views/Social/Chat.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: '/share-square',
        name: 'ShareSquare',
        component: () => import('@/views/Social/ShareSquare.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: '/moments',
        name: 'Moments',
        component: () => import('@/views/Social/Moments.vue'),
        meta: { requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 路由守卫：检查登录和权限
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  // 1. 访问根路径时，根据登录状态跳转
  if (to.path === '/') {
    if (userStore.isLogin) {
      next('/home')  // 已登录 → 首页
    } else {
      next('/welcome')  // 未登录 → 欢迎页
    }
    return
  }
  
  // 2. 检查是否需要管理员权限
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    ElMessage.error('需要管理员权限')
    next('/home')
    return
  }
  
  // 3. 检查是否需要登录
  if (to.meta.requiresAuth && !userStore.isLogin) {
    next('/login')
    return
  }
  
  // 4. 如果已登录，访问登录/注册/欢迎页，跳转到首页
  if ((to.path === '/login' || to.path === '/register' || to.path === '/welcome') && userStore.isLogin) {
    next('/home')
    return
  }
  
  // 5. 放行
  next()
})

export default router
