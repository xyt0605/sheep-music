<template>
  <div class="galaxy-loader" :class="size">
    <div class="ring"></div>
    <div class="core"></div>
  </div>
</template>

<script setup>
import { defineProps } from 'vue'

defineProps({
  size: { type: String, default: 'md' }
})
</script>

<style scoped>
.galaxy-loader {
  position: relative;
  display: inline-block;
}

.ring {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: conic-gradient(from 0deg, rgba(102,126,234,0.1), rgba(118,75,162,0.35), rgba(245,87,108,0.35), rgba(102,126,234,0.1));
  mask: radial-gradient(closest-side, transparent 55%, black 56%);
  animation: spin 1.2s linear infinite;
  box-shadow: 0 0 24px rgba(118, 75, 162, 0.35);
}

.core {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, #fff, rgba(255,255,255,0.2) 60%, transparent 70%);
  box-shadow: 0 0 24px rgba(255,255,255,0.6);
}

.sm .ring { width: 40px; height: 40px; }
.sm .core { width: 12px; height: 12px; }
.md .ring { width: 64px; height: 64px; }
.md .core { width: 18px; height: 18px; }
.lg .ring { width: 80px; height: 80px; }
.lg .core { width: 22px; height: 22px; }

@keyframes spin { to { transform: rotate(360deg); } }
</style>
