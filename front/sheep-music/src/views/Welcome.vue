<template>
  <div class="welcome-container">
    <video
      class="welcome-film"
      src="/editorial/night-loop.mp4"
      poster="/editorial/night-portrait.jpg"
      autoplay
      muted
      loop
      playsinline
    />
    <div class="welcome-veil" />

    <header class="welcome-brand">
      <span class="brand-glyph">S</span>
      <strong>Sheep Music</strong>
    </header>

    <main class="welcome-content">
      <p class="issue-label">PRIVATE LISTENING ROOM · VOL. 01</p>
      <h1 class="title">Sheep Music</h1>
      <p class="subtitle">
        把正在循环的歌、想留下的片段，以及朋友分享来的旋律，收进同一个私人空间。
      </p>

      <div class="action-buttons">
        <GalaxyButton
          size="lg"
          variant="primary"
          class="action-btn"
          @click="goToLogin"
        >
          立即登录
        </GalaxyButton>
        <GalaxyButton
          size="lg"
          variant="secondary"
          class="action-btn outline"
          @click="goToRegister"
        >
          免费注册
        </GalaxyButton>
      </div>

      <p class="welcome-note">SOUND ON · MEMORY IN MOTION</p>
    </main>

    <footer class="welcome-strip">
      <div><span>01</span><strong>私人歌单</strong></div>
      <div><span>02</span><strong>每日推荐</strong></div>
      <div><span>03</span><strong>好友电台</strong></div>
    </footer>
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
  position: relative;
  min-height: 100vh;
  display: grid;
  padding: clamp(28px, 4vw, 58px);
  align-content: center;
  overflow: hidden;
  background: #080a08;
  color: #f7f5ee;
}

.welcome-film,
.welcome-veil {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.welcome-film {
  object-fit: cover;
  object-position: center 34%;
}

.welcome-veil {
  background:
    linear-gradient(90deg, rgba(6, 8, 6, 0.9) 0%, rgba(6, 8, 6, 0.58) 40%, rgba(6, 8, 6, 0.12) 68%),
    linear-gradient(0deg, rgba(6, 8, 6, 0.82), transparent 42%);
}

.welcome-brand,
.welcome-content,
.welcome-strip {
  position: relative;
  z-index: 1;
}

.welcome-brand {
  position: absolute;
  top: clamp(28px, 4vw, 58px);
  left: clamp(28px, 4vw, 58px);
  display: flex;
  align-items: center;
  gap: 12px;
  font-family: Georgia, serif;
  font-size: 20px;
}

.brand-glyph {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  background: var(--signal-lime);
  color: #11120f;
  font-size: 21px;
  font-weight: 800;
}

.welcome-content {
  width: min(720px, 68vw);
  margin-top: 46px;
  animation: welcomeIn 0.7s ease both;
}

.issue-label,
.welcome-note,
.welcome-strip span {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 0.12em;
}

.issue-label {
  color: var(--signal-lime);
}

.title {
  margin: 24px 0 18px;
  font-family: var(--font-display);
  font-size: clamp(64px, 10vw, 150px);
  font-weight: 700;
  line-height: 0.88;
  letter-spacing: 0;
}

.subtitle {
  max-width: 620px;
  margin: 0;
  color: #d0d3c9;
  font-size: clamp(14px, 1.4vw, 18px);
  line-height: 1.9;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 34px;
  flex-wrap: wrap;
}

:deep(.galaxy-button.primary) {
  background: var(--signal-lime);
  color: #11120f;
}

:deep(.galaxy-button.secondary) {
  border: 1px solid rgba(255, 255, 255, 0.32);
  background: rgba(10, 12, 10, 0.52);
  color: #f7f5ee;
}

.action-btn {
  min-width: 154px;
}

.welcome-note {
  margin-top: 24px;
  color: #888d81;
}

.welcome-strip {
  position: absolute;
  right: clamp(28px, 4vw, 58px);
  bottom: clamp(28px, 4vw, 58px);
  left: clamp(28px, 4vw, 58px);
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-top: 1px solid rgba(255, 255, 255, 0.22);
}

.welcome-strip div {
  display: grid;
  min-height: 68px;
  padding: 14px 18px 0 0;
  align-content: start;
  gap: 4px;
  border-right: 1px solid rgba(255, 255, 255, 0.16);
}

.welcome-strip div:last-child {
  border-right: 0;
  padding-left: 18px;
}

.welcome-strip div:nth-child(2) {
  padding-left: 18px;
}

.welcome-strip span {
  color: var(--signal-lime);
}

.welcome-strip strong {
  font-size: 12px;
  font-weight: 650;
}

@keyframes welcomeIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .welcome-container {
    min-height: 100svh;
    padding: 96px 24px 168px;
    align-content: end;
  }

  .welcome-film {
    object-position: 58% center;
  }

  .welcome-veil {
    background: linear-gradient(0deg, rgba(6, 8, 6, 0.96) 0%, rgba(6, 8, 6, 0.62) 54%, rgba(6, 8, 6, 0.18) 100%);
  }

  .welcome-brand {
    top: 26px;
    left: 24px;
  }

  .welcome-content {
    width: 100%;
    margin: 0;
  }

  .title {
    font-size: clamp(58px, 20vw, 92px);
  }
  
  .action-buttons {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
  
  .action-btn {
    width: 100%;
    min-width: 0;
  }

  .welcome-strip {
    right: 24px;
    bottom: 18px;
    left: 24px;
  }

  .welcome-strip div {
    min-height: 58px;
    padding-top: 11px;
  }

  .welcome-strip strong {
    font-size: 10px;
  }
}
</style>

