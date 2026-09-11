<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <div class="admin-page">
    <header class="page-header">
      <div>
        <span class="eyebrow">LIBRARY OPERATIONS</span>
        <h2>内容后台</h2>
        <p class="subtitle">集中处理歌手、歌曲与发布状态，导入流程已合并为一个入口。</p>
      </div>
      <div class="admin-user">
        <span>当前管理员</span>
        <strong>{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员' }}</strong>
      </div>
    </header>

    <el-tabs v-model="activeTab" class="admin-tabs" stretch>
      <el-tab-pane name="import">
        <template #label><span class="tab-label"><el-icon><Upload /></el-icon>导入中心</span></template>
        <ContentImport />
      </el-tab-pane>
      <el-tab-pane name="artists">
        <template #label><span class="tab-label"><el-icon><Microphone /></el-icon>歌手管理</span></template>
        <ArtistManagement />
      </el-tab-pane>
      <el-tab-pane name="songs">
        <template #label><span class="tab-label"><el-icon><Headset /></el-icon>歌曲管理</span></template>
        <SongManagement />
      </el-tab-pane>
      <el-tab-pane name="memories">
        <template #label><span class="tab-label"><el-icon><Sunny /></el-icon>记忆管理</span></template>
        <MemoryManagePanel />
      </el-tab-pane>
      <el-tab-pane name="settings">
        <template #label><span class="tab-label"><el-icon><Setting /></el-icon>系统设置</span></template>
        <SystemSettingsPanel />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Headset, Microphone, Setting, Sunny, Upload } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import ArtistManagement from '@/components/admin/ArtistManagement.vue'
import ContentImport from '@/components/admin/ContentImport.vue'
import SongManagement from '@/components/admin/SongManagement.vue'
import MemoryManagePanel from '@/components/admin/MemoryManagePanel.vue'
import SystemSettingsPanel from '@/components/admin/SystemSettingsPanel.vue'

const userStore = useUserStore()
const activeTab = ref('import')
</script>

<style scoped>
.admin-page { width: 100%; }
.eyebrow { color: var(--text-tertiary); font-family: var(--font-mono); font-size: 10px; letter-spacing: .16em; }
.subtitle { max-width: 620px; }
.admin-user { display: grid; gap: 4px; align-self: flex-end; padding: 9px 0 0 20px; border-left: 1px solid var(--border-color); color: var(--text-tertiary); font-size: 11px; }
.admin-user strong { color: var(--text-primary); font-size: 13px; }
.tab-label { display: inline-flex; align-items: center; gap: 7px; }
@media (max-width: 680px) { .admin-user { display: none; } }
</style>
