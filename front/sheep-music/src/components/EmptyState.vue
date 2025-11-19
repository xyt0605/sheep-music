<template>
  <div class="empty-state" :class="{ compact }">
    <div class="empty-icon">
      <slot name="icon">
        <component :is="iconComponent" />
      </slot>
    </div>
    
    <div class="empty-content">
      <h3 v-if="title" class="empty-title">{{ title }}</h3>
      <p v-if="description" class="empty-description">{{ description }}</p>
      
      <slot name="extra"></slot>
      
      <div v-if="actionText || $slots.action" class="empty-action">
        <slot name="action">
          <el-button
            v-if="actionText"
            :type="actionType"
            :icon="actionIcon"
            @click="$emit('action')"
          >
            {{ actionText }}
          </el-button>
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  DocumentDelete,
  MusicNote,
  User,
  FolderOpened,
  Search,
  Warning,
  CircleClose,
  Sunny
} from '@element-plus/icons-vue'

const props = defineProps({
  type: {
    type: String,
    default: 'default', // default, music, user, folder, search, error, warning
    validator: (value) => {
      return ['default', 'music', 'user', 'folder', 'search', 'error', 'warning', 'success'].includes(value)
    }
  },
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  actionText: {
    type: String,
    default: ''
  },
  actionType: {
    type: String,
    default: 'primary'
  },
  actionIcon: {
    type: Object,
    default: null
  },
  compact: {
    type: Boolean,
    default: false
  }
})

defineEmits(['action'])

const iconComponent = computed(() => {
  const iconMap = {
    default: DocumentDelete,
    music: MusicNote,
    user: User,
    folder: FolderOpened,
    search: Search,
    error: CircleClose,
    warning: Warning,
    success: Sunny
  }
  return iconMap[props.type] || DocumentDelete
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-state.compact {
  padding: 40px 20px;
}

.empty-icon {
  font-size: 120px;
  color: var(--text-tertiary);
  margin-bottom: 24px;
  opacity: 0.6;
  animation: fadeInUp 0.6s ease;
}

.empty-state.compact .empty-icon {
  font-size: 80px;
  margin-bottom: 16px;
}

.empty-content {
  max-width: 480px;
  animation: fadeInUp 0.6s ease 0.1s backwards;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.empty-state.compact .empty-title {
  font-size: 18px;
  margin-bottom: 8px;
}

.empty-description {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 24px;
}

.empty-state.compact .empty-description {
  font-size: 13px;
  margin-bottom: 16px;
}

.empty-action {
  margin-top: 24px;
  animation: fadeInUp 0.6s ease 0.2s backwards;
}

.empty-state.compact .empty-action {
  margin-top: 16px;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .empty-state {
    padding: 60px 20px;
  }
  
  .empty-icon {
    font-size: 80px;
  }
  
  .empty-title {
    font-size: 18px;
  }
  
  .empty-description {
    font-size: 13px;
  }
}
</style>

