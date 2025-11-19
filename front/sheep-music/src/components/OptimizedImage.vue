<template>
  <div class="optimized-image" :style="{ aspectRatio: aspectRatio }">
    <transition name="fade">
      <div v-if="loading && !error" class="image-placeholder">
        <div class="placeholder-shimmer"></div>
        <el-icon class="placeholder-icon"><Picture /></el-icon>
      </div>
    </transition>
    
    <transition name="fade">
      <img
        v-if="!error"
        v-show="!loading"
        :src="currentSrc"
        :alt="alt"
        :class="imgClass"
        @load="handleLoad"
        @error="handleError"
        loading="lazy"
      />
    </transition>
    
    <transition name="fade">
      <div v-if="error" class="image-error" @click="retry">
        <el-icon class="error-icon"><Picture /></el-icon>
        <span class="error-text">{{ errorText }}</span>
        <el-button size="small" text @click.stop="retry">重试</el-button>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Picture } from '@element-plus/icons-vue'

const props = defineProps({
  src: {
    type: String,
    required: true
  },
  alt: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '/placeholder-loading.svg'
  },
  fallback: {
    type: String,
    default: '/placeholder-error.svg'
  },
  aspectRatio: {
    type: String,
    default: 'auto'
  },
  imgClass: {
    type: String,
    default: ''
  },
  errorText: {
    type: String,
    default: '加载失败'
  }
})

const loading = ref(true)
const error = ref(false)
const currentSrc = ref(props.placeholder)
const retryCount = ref(0)
const maxRetries = 3

watch(() => props.src, (newSrc) => {
  if (newSrc) {
    loadImage()
  }
})

onMounted(() => {
  if (props.src) {
    loadImage()
  }
})

const loadImage = () => {
  loading.value = true
  error.value = false
  currentSrc.value = props.src || props.placeholder
}

const handleLoad = () => {
  loading.value = false
  error.value = false
  retryCount.value = 0
}

const handleError = () => {
  loading.value = false
  
  if (retryCount.value < maxRetries) {
    // 自动重试
    retryCount.value++
    setTimeout(() => {
      currentSrc.value = `${props.src}?retry=${retryCount.value}`
    }, 1000 * retryCount.value)
  } else {
    error.value = true
    currentSrc.value = props.fallback
  }
}

const retry = () => {
  retryCount.value = 0
  loadImage()
}
</script>

<style scoped>
.optimized-image {
  position: relative;
  width: 100%;
  overflow: hidden;
  background-color: var(--bg-tertiary);
  border-radius: inherit;
}

.optimized-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.image-placeholder,
.image-error {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-tertiary);
}

.placeholder-shimmer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    90deg,
    var(--bg-tertiary) 0%,
    var(--bg-secondary) 50%,
    var(--bg-tertiary) 100%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

.placeholder-icon,
.error-icon {
  font-size: 48px;
  color: var(--text-tertiary);
  position: relative;
  z-index: 1;
}

.error-icon {
  margin-bottom: 8px;
}

.image-error {
  cursor: pointer;
  transition: background-color var(--transition-base);
}

.image-error:hover {
  background-color: var(--bg-secondary);
}

.error-text {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

/* 淡入淡出过渡 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

