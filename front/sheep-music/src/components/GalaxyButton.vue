<template>
  <button
    class="galaxy-button"
    :class="[size, variant, { loading, disabled }]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span class="glow"></span>
    <span class="content">
      <slot />
    </span>
  </button>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  size: { type: String, default: 'md' },
  variant: { type: String, default: 'primary' }
})

const emit = defineEmits(['click'])

const handleClick = (e) => {
  if (props.disabled || props.loading) return
  emit('click', e)
}
</script>

<style scoped>
.galaxy-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.75rem 1.25rem;
  border-radius: 9999px;
  color: var(--text-inverse);
  background: var(--gradient-primary);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.35);
  outline: none;
  border: none;
  cursor: pointer;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast), filter var(--transition-fast);
  overflow: hidden;
}

.galaxy-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(102, 126, 234, 0.5);
}

.galaxy-button:active {
  transform: translateY(0);
}

.glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(120px 120px at var(--mx, 50%) var(--my, 50%), rgba(255,255,255,0.35), transparent 60%);
  opacity: 0.35;
  pointer-events: none;
}

.content {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.sm { padding: 0.5rem 1rem; font-size: 0.875rem; }
.md { padding: 0.75rem 1.25rem; font-size: 1rem; }
.lg { padding: 0.9rem 1.5rem; font-size: 1.05rem; }

.primary { background: var(--gradient-primary); }
.secondary { background: var(--gradient-secondary); }
.warm { background: var(--gradient-warm); }

.disabled,
.galaxy-button:disabled {
  cursor: not-allowed;
  filter: grayscale(35%);
  opacity: 0.8;
}

.loading {
  pointer-events: none;
  animation: none; /* 移除旋转动画效果 */
}
</style>

<style>
.galaxy-button:hover .glow {
  opacity: 0.5;
}
</style>
