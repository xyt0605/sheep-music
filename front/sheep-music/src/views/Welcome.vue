<template>
  <div class="welcome-container">
    <div class="welcome-content">
      <!-- Logo 和标题 -->
      <div class="logo-section">
        <div class="music-icon">🎵</div>
        <h1 class="title">Sheep Music</h1>
        <p class="subtitle">发现音乐，享受生活</p>
      </div>
      
      <!-- 功能介绍 -->
      <div class="features">
        <div class="feature-item">
          <div class="feature-icon">🎧</div>
          <div class="feature-text">海量音乐库</div>
        </div>
        <div class="feature-item">
          <div class="feature-icon">📱</div>
          <div class="feature-text">随时随地播放</div>
        </div>
        <div class="feature-item">
          <div class="feature-icon">❤️</div>
          <div class="feature-text">创建专属歌单</div>
        </div>
      </div>
      
      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button 
          type="primary" 
          size="large" 
          round
          @click="goToLogin"
          class="action-btn"
        >
          立即登录
        </el-button>
        <el-button 
          size="large" 
          round
          @click="goToRegister"
          class="action-btn register-btn"
        >
          免费注册
        </el-button>
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

export default {
  name: 'Welcome',
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.welcome-content {
  text-align: center;
  color: white;
  animation: fadeIn 0.8s ease-in;
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
  font-size: 80px;
  margin-bottom: 20px;
  animation: bounce 2s infinite;
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
  font-size: 56px;
  font-weight: bold;
  margin-bottom: 15px;
  text-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.subtitle {
  font-size: 20px;
  opacity: 0.9;
  font-weight: 300;
}

/* 功能介绍 */
.features {
  display: flex;
  justify-content: center;
  gap: 60px;
  margin-bottom: 50px;
  flex-wrap: wrap;
}

.feature-item {
  text-align: center;
}

.feature-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.feature-text {
  font-size: 16px;
  opacity: 0.9;
}

/* 按钮区域 */
.action-buttons {
  display: flex;
  gap: 20px;
  justify-content: center;
  margin-bottom: 40px;
  flex-wrap: wrap;
}

.action-btn {
  min-width: 160px;
  height: 50px;
  font-size: 16px;
  font-weight: bold;
  transition: all 0.3s ease;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

.register-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid white;
  color: white;
}

.register-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: white;
  color: white;
}

/* 底部文字 */
.footer-text {
  font-size: 14px;
  opacity: 0.8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .title {
    font-size: 40px;
  }
  
  .features {
    gap: 30px;
  }
  
  .action-buttons {
    flex-direction: column;
    align-items: center;
  }
  
  .action-btn {
    width: 100%;
    max-width: 300px;
  }
}
</style>

