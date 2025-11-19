<template>
  <div class="gfi" :class="[{ focused, filled: !!innerValue }, size]">
    <input
      :type="type"
      class="gfi-input"
      :name="name"
      :autocomplete="autocomplete"
      :value="innerValue"
      @input="onInput"
      @focus="onFocus"
      @blur="onBlur"
    />
    <label class="gfi-label">{{ label }}</label>
    <div v-if="message" class="gfi-message">{{ message }}</div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits, ref } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  label: { type: String, default: '' },
  type: { type: String, default: 'text' },
  name: { type: String, default: '' },
  size: { type: String, default: 'lg' },
  message: { type: String, default: '' },
  autocomplete: { type: String, default: 'off' }
})

const emit = defineEmits(['update:modelValue', 'blur', 'change'])
const innerValue = ref(props.modelValue)
const focused = ref(false)

const onInput = (e) => {
  innerValue.value = e.target.value
  emit('update:modelValue', innerValue.value)
  emit('change', innerValue.value)
}

const onFocus = () => { focused.value = true }
const onBlur = () => { focused.value = false; emit('blur', innerValue.value) }
</script>

<style scoped>
.gfi {
  position: relative;
  margin: 8px 0 14px;
  width: 100%;
}

.gfi-input {
  width: 100%;
  padding: 16px 16px 14px;
  border-radius: 14px;
  outline: none;
  border: 1.5px solid var(--border-color);
  background: linear-gradient(180deg, var(--input-bg), rgba(255,255,255,0.92));
  color: var(--text-primary);
  box-shadow: var(--shadow-sm);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast), background var(--transition-fast);
}

.gfi-label {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-tertiary);
  background: transparent;
  padding: 0 6px;
  pointer-events: none;
  transition: all var(--transition-fast);
  font-weight: 600;
}

.gfi.focused .gfi-input,
.gfi.filled .gfi-input {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.18);
}

.gfi.focused .gfi-label,
.gfi.filled .gfi-label {
  top: 0;
  transform: translateY(-50%) scale(0.85);
  background: var(--card-bg);
  color: var(--color-primary);
}

.gfi-message {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.sm .gfi-input { padding: 12px 14px; border-radius: 10px; }
.lg .gfi-input { padding: 18px 18px; }
</style>
