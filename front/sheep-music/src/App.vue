<template>
  <div id="app">
    <!-- 动态背景 -->
    <DynamicBackground />
    
    <!-- 路由视图带过渡动画 -->
    <router-view v-slot="{ Component, route }">
      <transition :name="route.meta.transition || 'fade'" mode="out-in">
        <component :is="Component" :key="route.path" />
      </transition>
    </router-view>
    
    <!-- 全局音乐播放器 -->
    <MusicPlayer />
    
    <!-- 全屏歌词 -->
    <FullscreenLyric 
      :visible="showFullscreenLyric" 
      @close="showFullscreenLyric = false"
    />
  </div>
</template>

<script>
import { ref, provide } from 'vue'
import DynamicBackground from '@/components/DynamicBackground.vue'
import MusicPlayer from '@/components/MusicPlayer.vue'
import FullscreenLyric from '@/components/FullscreenLyric.vue'

export default {
  name: 'App',
  components: {
    DynamicBackground,
    MusicPlayer,
    FullscreenLyric
  },
  setup() {
    const showFullscreenLyric = ref(false)
    
    // 提供全局方法用于显示全屏歌词
    provide('showFullscreenLyric', () => {
      showFullscreenLyric.value = true
    })
    
    return {
      showFullscreenLyric
    }
  }
}
</script>

<style>
@import '@/styles/animations.css';

:root {
  --primary-color: #409eff;
  --primary-light: #ecf5ff;
  --text-primary: #303133;
  --text-secondary: #909399;
  --bg-primary: #ffffff;
  --bg-secondary: #f5f7fa;
  --border-radius-sm: 8px;
  --border-radius-md: 12px;
  --border-radius-lg: 16px;
  --shadow-sm: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 16px 0 rgba(0, 0, 0, 0.08);
  --shadow-lg: 0 8px 24px 0 rgba(0, 0, 0, 0.12);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 全局滚动条美化 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(144, 147, 153, 0.3);
  border-radius: 4px;
  transition: background 0.3s;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(144, 147, 153, 0.5);
}

#app {
  font-family: 'Inter', 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  min-height: 100vh;
  background-color: var(--bg-secondary);
  color: var(--text-primary);
}

/* 全局过渡效果 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
