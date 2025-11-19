<template>
  <div class="welcome-container">
    <div class="welcome-content">
      <!-- Logo 和标题 -->
      <div class="logo-section">
        <div class="music-icon">
          <svg width="72" height="72" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="g1" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#0ea5e9"/>
                <stop offset="100%" stop-color="#22d3ee"/>
              </linearGradient>
            </defs>
            <path d="M9 19a3 3 0 1 1-3-3 3 3 0 0 1 3 3Zm9-12v8.5a3.5 3.5 0 1 1-2-3.15V7h-7v8.5a3.5 3.5 0 1 1-2-3.15V5h11Z" stroke="url(#g1)" stroke-width="1.6" fill="none"/>
          </svg>
        </div>
        <h1 class="title">Sheep Music</h1>
        <p class="subtitle">发现音乐，享受生活</p>
      </div>
      
      <!-- 功能介绍 -->
      <div class="features">
        <div class="feature-item">
          <div class="feature-icon">
            <svg width="42" height="42" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="g2" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#06b6d4"/>
                  <stop offset="100%" stop-color="#3b82f6"/>
                </linearGradient>
              </defs>
              <path d="M6 18a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm12 0a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM9 12V6a3 3 0 0 1 3-3h4" stroke="url(#g2)" stroke-width="1.6" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="feature-text">海量音乐库</div>
        </div>
        <div class="feature-item">
          <div class="feature-icon">
            <svg width="42" height="42" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="g3" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#06b6d4"/>
                  <stop offset="100%" stop-color="#3b82f6"/>
                </linearGradient>
              </defs>
              <path d="M8 5h8a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3Z" stroke="url(#g3)" stroke-width="1.6"/>
              <path d="M10 12l5 3V9l-5 3Z" stroke="url(#g3)" stroke-width="1.6" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="feature-text">随时随地播放</div>
        </div>
        <div class="feature-item">
          <div class="feature-icon">
            <svg width="42" height="42" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="g4" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#06b6d4"/>
                  <stop offset="100%" stop-color="#3b82f6"/>
                </linearGradient>
              </defs>
              <path d="M12 21s-6-3.5-6-8a4 4 0 0 1 8 0 4 4 0 0 1 8 0c0 4.5-6 8-10 8Z" stroke="url(#g4)" stroke-width="1.6" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="feature-text">创建专属歌单</div>
        </div>
      </div>
      
      <!-- 操作按钮 -->
      <div class="action-buttons">
        <GalaxyButton size="lg" variant="primary" class="action-btn" @click="goToLogin">
          立即登录
        </GalaxyButton>
        <GalaxyButton size="lg" variant="secondary" class="action-btn outline" @click="goToRegister">
          免费注册
        </GalaxyButton>
      </div>
      
      <!-- 底部说明 -->
      <div class="footer-text">
        <p>开启你的音乐之旅</p>
      </div>
    </div>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'
import GalaxyButton from '@/components/GalaxyButton.vue'

export default {
  name: 'Welcome',
  components: { GalaxyButton },
  setup() {
    const router = useRouter()
    
    const goToLogin = () => {
      router.push('/login')
    }
    
    const goToRegister = () => {
      router.push('/register')
    }
    
    return {
      goToLogin,
      goToRegister
    }
  }
}
</script>

<style scoped>
.welcome-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background:
    linear-gradient(135deg, #0b1325 0%, #0f1e33 100%),
    radial-gradient(800px 400px at 20% 30%, rgba(14,165,233,0.12), transparent 60%),
    radial-gradient(700px 350px at 80% 20%, rgba(34,211,238,0.10), transparent 65%),
    repeating-linear-gradient(45deg, rgba(255,255,255,0.03) 0px, rgba(255,255,255,0.03) 2px, transparent 2px, transparent 8px);
  padding: 20px;
}

.welcome-content {
  text-align: center;
  color: var(--text-primary);
  animation: fadeIn 0.8s ease-in;
  background: var(--card-bg);
  border: 1px solid var(--border-color-light);
  box-shadow: var(--shadow-xl);
  backdrop-filter: blur(18px);
  border-radius: 20px;
  padding: 48px 56px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Logo 区域 */
.logo-section {
  margin-bottom: 60px;
}

.music-icon {
  margin-bottom: 16px;
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.title {
  font-size: 52px;
  font-weight: 800;
  letter-spacing: 0.5px;
  margin-bottom: 12px;
}

.subtitle {
  font-size: 18px;
  color: var(--text-tertiary);
}

/* 功能介绍 */
.features {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-bottom: 40px;
  flex-wrap: wrap;
}

.feature-item {
  text-align: center;
  padding: 18px 20px;
  border-radius: 16px;
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.08);
  box-shadow: 0 8px 24px rgba(0,0,0,0.25);
  backdrop-filter: blur(12px);
  min-width: 180px;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.feature-icon {
  margin-bottom: 10px;
}

.feature-icon svg {
  filter: drop-shadow(0 0 12px rgba(34, 211, 238, 0.35));
}

.feature-text {
  font-size: 15px;
  color: var(--text-secondary);
}

/* 按钮区域 */
.action-buttons {
  display: flex;
  gap: 20px;
  justify-content: center;
  margin-bottom: 36px;
  flex-wrap: wrap;
}

:deep(.galaxy-button.primary) {
  background: linear-gradient(135deg, #0ea5e9 0%, #22d3ee 100%);
  box-shadow: 0 8px 24px rgba(14, 165, 233, 0.35);
}

:deep(.galaxy-button.secondary) {
  background: linear-gradient(135deg, #1f2937 0%, #334155 100%);
  color: #e5e7eb;
}

.action-btn {
  min-width: 180px;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

.outline {
  background: transparent;
  border: 1.5px solid rgba(255,255,255,0.12);
  color: #e5e7eb;
}

/* 底部文字 */
.footer-text {
  font-size: 13px;
  color: var(--text-tertiary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .title { font-size: 36px; }
  .features { gap: 16px; }
  
  .action-buttons {
    flex-direction: column;
    align-items: center;
  }
  
  .action-btn { width: 100%; max-width: 320px; }
}
</style>

