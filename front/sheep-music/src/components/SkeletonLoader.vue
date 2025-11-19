<template>
  <div class="skeleton-loader">
    <!-- 歌曲列表骨架屏 -->
    <div v-if="type === 'song-list'" class="skeleton-song-list">
      <div v-for="i in rows" :key="i" class="skeleton-song-item">
        <div class="skeleton-index shimmer"></div>
        <div class="skeleton-cover shimmer"></div>
        <div class="skeleton-info">
          <div class="skeleton-title shimmer"></div>
          <div class="skeleton-artist shimmer"></div>
        </div>
        <div class="skeleton-stats shimmer"></div>
        <div class="skeleton-actions">
          <div class="skeleton-btn shimmer"></div>
          <div class="skeleton-btn shimmer"></div>
          <div class="skeleton-btn shimmer"></div>
        </div>
      </div>
    </div>

    <!-- 歌曲网格骨架屏 -->
    <div v-else-if="type === 'song-grid'" class="skeleton-song-grid">
      <div v-for="i in rows" :key="i" class="skeleton-card">
        <div class="skeleton-card-cover shimmer"></div>
        <div class="skeleton-card-title shimmer"></div>
        <div class="skeleton-card-artist shimmer"></div>
      </div>
    </div>

    <!-- 歌手卡片骨架屏 -->
    <div v-else-if="type === 'artist-grid'" class="skeleton-artist-grid">
      <div v-for="i in rows" :key="i" class="skeleton-artist-card">
        <div class="skeleton-artist-avatar shimmer"></div>
        <div class="skeleton-artist-name shimmer"></div>
        <div class="skeleton-artist-stats shimmer"></div>
      </div>
    </div>

    <!-- 默认骨架屏 -->
    <div v-else class="skeleton-default">
      <el-skeleton :rows="rows" animated />
    </div>
  </div>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'default' // song-list, song-grid, artist-grid, default
  },
  rows: {
    type: Number,
    default: 5
  }
})
</script>

<style scoped>
.skeleton-loader {
  width: 100%;
  padding: 20px 0;
}

/* 闪烁动画 */
@keyframes shimmer {
  0% {
    background-position: -1000px 0;
  }
  100% {
    background-position: 1000px 0;
  }
}

.shimmer {
  animation: shimmer 2s infinite linear;
  background: linear-gradient(
    90deg,
    var(--bg-secondary) 0%,
    var(--bg-tertiary) 50%,
    var(--bg-secondary) 100%
  );
  background-size: 1000px 100%;
}

/* 歌曲列表骨架 */
.skeleton-song-list {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 10px;
}

.skeleton-song-item {
  display: flex;
  align-items: center;
  padding: 12px;
  gap: 15px;
  margin-bottom: 8px;
}

.skeleton-index {
  width: 30px;
  height: 20px;
  border-radius: 4px;
}

.skeleton-cover {
  width: 60px;
  height: 60px;
  border-radius: 8px;
}

.skeleton-info {
  flex: 1;
}

.skeleton-title {
  height: 16px;
  width: 200px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.skeleton-artist {
  height: 14px;
  width: 120px;
  border-radius: 4px;
}

.skeleton-stats {
  width: 80px;
  height: 14px;
  border-radius: 4px;
  margin-right: 20px;
}

.skeleton-actions {
  display: flex;
  gap: 10px;
}

.skeleton-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
}

/* 歌曲网格骨架 */
.skeleton-song-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.skeleton-card {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 15px;
}

.skeleton-card-cover {
  width: 100%;
  padding-bottom: 100%;
  border-radius: 8px;
  margin-bottom: 12px;
}

.skeleton-card-title {
  height: 16px;
  width: 80%;
  border-radius: 4px;
  margin-bottom: 8px;
}

.skeleton-card-artist {
  height: 14px;
  width: 60%;
  border-radius: 4px;
}

/* 歌手网格骨架 */
.skeleton-artist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 24px;
}

.skeleton-artist-card {
  text-align: center;
}

.skeleton-artist-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  margin: 0 auto 16px;
}

.skeleton-artist-name {
  height: 18px;
  width: 100px;
  border-radius: 4px;
  margin: 0 auto 8px;
}

.skeleton-artist-stats {
  height: 14px;
  width: 80px;
  border-radius: 4px;
  margin: 0 auto;
}

/* 响应式 */
@media (max-width: 768px) {
  .skeleton-song-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 15px;
  }

  .skeleton-artist-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 16px;
  }

  .skeleton-artist-avatar {
    width: 100px;
    height: 100px;
  }
}
</style>

