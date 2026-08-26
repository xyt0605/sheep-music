<template>
  <div id="app">
    <!-- 动态背景 -->
    <DynamicBackground />
    
    <!-- 路由视图带过渡动画 -->
    <router-view v-slot="{ Component, route }">
      <component
        :is="Component"
        :key="route.path"
      />
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
@import '@/styles/theme.css';

/* Global styles are now handled in theme.css */
#app {
  min-height: 100vh;
  /* Remove background to show DynamicBackground */
  background-color: transparent;
  color: var(--text-primary);
  overflow-x: hidden; /* Prevent scrollbar flickering */
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
