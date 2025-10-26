<template>
  <div class="playlist-cover" :style="{ width: size + 'px', height: size + 'px' }">
    <!-- 显示封面（使用最新添加的歌曲封面，类似QQ音乐） -->
    <img
      :src="coverUrl || defaultCover"
      alt="歌单封面"
      class="cover-image"
      @error="handleImageError"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 封面数据：可以是字符串（单张URL）或数组（多张URL）
  cover: {
    type: [String, Array],
    default: null
  },
  // 封面尺寸
  size: {
    type: Number,
    default: 200
  },
  // 默认封面
  defaultCover: {
    type: String,
    default: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
  }
})

// 获取封面URL（单张封面，类似QQ音乐）
const coverUrl = computed(() => {
  if (!props.cover) return null
  
  // 如果是字符串，直接返回（新的封面策略）
  if (typeof props.cover === 'string') {
    return props.cover
  }
  
  // 兼容旧的数组格式（如果存在）
  if (Array.isArray(props.cover) && props.cover.length > 0) {
    return props.cover[0]
  }
  
  return null
})

// 图片加载失败处理
const handleImageError = (e) => {
  e.target.src = props.defaultCover
}
</script>

<style scoped>
.playlist-cover {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  background-color: #f5f5f5;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.3s;
}

.playlist-cover:hover .cover-image {
  transform: scale(1.05);
}
</style>

