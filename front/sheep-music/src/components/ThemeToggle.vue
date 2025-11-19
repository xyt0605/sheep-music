<template>
  <div class="theme-toggle">
    <el-tooltip :content="isDark ? '切换到亮色模式' : '切换到暗黑模式'" placement="bottom">
      <el-button 
        :icon="isDark ? Sunny : Moon" 
        circle 
        @click="toggleTheme"
        class="theme-toggle-btn"
      />
    </el-tooltip>
  </div>
</template>

<script setup>
import { Moon, Sunny } from '@element-plus/icons-vue'
import { useTheme } from '@/composables/useTheme'

const { isDark, toggleTheme } = useTheme()
</script>

<style scoped>
.theme-toggle-btn {
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.theme-toggle-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.theme-toggle-btn:active {
  transform: scale(0.95);
}
</style>

