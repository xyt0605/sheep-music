<template>
  <div class="login-container">
    <video
      class="ambient-video-backdrop"
      src="/editorial/night-loop.mp4"
      poster="/editorial/night-portrait.jpg"
      autoplay
      muted
      loop
      playsinline
      aria-hidden="true"
    />
    <video
      class="ambient-video"
      src="/editorial/night-loop.mp4"
      poster="/editorial/night-portrait.jpg"
      autoplay
      muted
      loop
      playsinline
    />
    <div class="ambient-shade" />

    <main class="login-stage">
      <section class="login-story">
        <div class="brand-lockup">
          <span class="brand-mark">S</span>
          <span>Sheep Music</span>
        </div>
        <p class="story-kicker">
          PRIVATE LISTENING ROOM · 08/26
        </p>
        <h1>婉婉的<br>音乐小屋</h1>
        <p class="story-copy">
          一段影像，一首正在循环的歌。戴上耳机，继续你的私人播放列表。
        </p>

        <div
          class="portrait-strip"
          aria-label="本期影像"
        >
          <figure class="portrait-card portrait-card-night">
            <img
              src="/editorial/night-portrait.jpg"
              alt="夜晚人像"
            >
            <figcaption>After dark · 01</figcaption>
          </figure>
          <figure class="portrait-card portrait-card-day">
            <img
              src="/editorial/bamboo-portrait.jpg"
              alt="竹林人像"
            >
            <figcaption>Daylight · 02</figcaption>
          </figure>
        </div>
      </section>

      <section class="login-box">
        <header class="form-heading">
          <p>MEMBER ACCESS</p>
          <h2>继续收听</h2>
          <span>登录你的 Sheep Music 账号</span>
        </header>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          autocomplete="off"
        >
          <input
            type="text"
            name="fake-username"
            style="display:none"
            autocomplete="off"
          >
          <input
            type="password"
            name="fake-password"
            style="display:none"
            autocomplete="new-password"
          >

          <el-form-item prop="username">
            <GalaxyFormInput
              v-model="loginForm.username"
              label="用户名"
              name="username"
              autocomplete="off"
              @blur="noop"
            />
          </el-form-item>

          <el-form-item prop="password">
            <GalaxyFormInput
              v-model="loginForm.password"
              type="password"
              label="密码"
              name="password"
              autocomplete="new-password"
              @blur="noop"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <GalaxyButton
              :loading="loading"
              size="lg"
              variant="primary"
              class="login-button"
              @click="handleLogin"
            >
              {{ loading ? '正在进入...' : '进入音乐空间' }}
            </GalaxyButton>
          </el-form-item>

          <div class="register-link">
            第一次来？
            <router-link to="/register">
              创建账号
            </router-link>
          </div>
        </el-form>
        <p class="form-note">
          SOUND ON · MEMORY IN MOTION
        </p>
      </section>
    </main>
    <div class="edition-label">
      VOL. 01 / A MOMENT IN MUSIC
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import GalaxyFormInput from '@/components/GalaxyFormInput.vue'
import GalaxyButton from '@/components/GalaxyButton.vue'
import { login } from '@/api/user'

export default {
  // eslint-disable-next-line vue/multi-word-component-names
  name: 'Login',
  components: {
    GalaxyFormInput,
    GalaxyButton
  },
  setup () {
    const router = useRouter()
    const userStore = useUserStore()
    const loginFormRef = ref(null)
    const loading = ref(false)

    // 表单数据
    const loginForm = reactive({
      username: '',
      password: ''
    })

    // 表单验证规则
    const loginRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
      ]
    }

    // 组件挂载时清空表单
    onMounted(() => {
      // 延迟清空，确保在浏览器自动填充之后执行
      setTimeout(() => {
        loginForm.username = ''
        loginForm.password = ''
        // 清除表单验证状态
        if (loginFormRef.value) {
          loginFormRef.value.clearValidate()
        }
        // 移除 readonly 属性（允许用户输入）
        removeReadonly()
      }, 100)
    })

    // 移除 readonly 属性的辅助函数
    const removeReadonly = () => {
      const inputs = document.querySelectorAll('.login-form input')
      inputs.forEach(input => {
        input.removeAttribute('readonly')
      })
    }

    // 聚焦时移除 readonly（用户名输入框）
    const handleUsernameFocus = (event) => {
      event.target.removeAttribute('readonly')
    }

    // 聚焦时移除 readonly（密码输入框）
    const handlePasswordFocus = (event) => {
      event.target.removeAttribute('readonly')
    }

    const noop = () => {}

    // 登录处理
    const handleLogin = async () => {
      // 1. 表单验证
      if (!loginFormRef.value) return

      await loginFormRef.value.validate(async (valid) => {
        if (!valid) return

        try {
          // 2. 显示加载状态
          loading.value = true

          // 3. 调用登录API
          const res = await login({
            username: loginForm.username,
            password: loginForm.password
          })

          // 4. 登录成功，保存token和用户信息
          userStore.setToken(res.data.token)
          userStore.setUserInfo(res.data.userInfo)

          // 5. 提示成功
          ElMessage.success('登录成功')

          // 6. 清空输入框
          loginForm.username = ''
          loginForm.password = ''
          // 清除表单验证状态（不使用 resetFields，避免恢复旧数据）
          if (loginFormRef.value) {
            loginFormRef.value.clearValidate()
          }

          // 7. 跳转到首页
          setTimeout(() => {
            router.push('/home')
          }, 500)
        } catch (error) {
          // 8. 登录失败，显示错误信息
          ElMessage.error(error.message || '登录失败')
        } finally {
          // 9. 隐藏加载状态
          loading.value = false
        }
      })
    }

    return {
      loginFormRef,
      loginForm,
      loginRules,
      loading,
      handleLogin,
      handleUsernameFocus,
      handlePasswordFocus,
      noop
    }
  }
}
</script>

<style scoped>
.login-container {
  /* 页面四周统一留白：品牌图标、卷号标签都对齐到这个值 */
  --gutter: clamp(20px, 2.2vw, 40px);
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: #0d1110;
  color: #f5f0e7;
}

.ambient-video-backdrop,
.ambient-video,
.ambient-shade {
  position: absolute;
}

/*
 * 环境底层：同一段视频铺满整屏并大幅放大 + 模糊，
 * 作用是把中间清晰视频带的颜色向两侧延伸，避免出现可见边界。
 * 盒子刻意超出视口，防止 blur 采样到视口外的透明像素产生暗边。
 */
.ambient-video-backdrop {
  inset: -12%;
  width: 124%;
  height: 124%;
  object-fit: cover;
  object-position: center 32%;
  opacity: 1;
  transform: scale(1.08);
  filter: blur(40px) saturate(1.02) brightness(0.8);
}

/*
 * 清晰视频带：贴顶贴底通高。56.25vh 正好是 9:16 素材在满高时的无裁切宽度，
 * 额外 +40px 是刻意加宽，代价是纵向溢出约 71px；用 object-position 把这部分
 * 主要从画面顶部（头发/暗部）切掉，底部的亲吻动作完整保留。
 * mask 的 ellipse 横向半径取 50%（在元素左右边缘刚好衰减到 0），
 * 纵向半径 126% 把衰减推到视口外，所以上下是满幅出血、没有浮框感。
 * 第一个色标放到 70%：中间大部分是纯清晰画面，只有靠边的窄带才和模糊层混合。
 */
.ambient-video {
  top: 0;
  left: 50%;
  height: 100%;
  width: calc(56.25vh + 40px);
  max-width: 100%;
  object-fit: cover;
  object-position: center 80%;
  opacity: 1;
  transform: translateX(-50%);
  filter: none;
  -webkit-mask-image: radial-gradient(ellipse 50% 126% at 50% 42%, #000 0%, #000 70%, rgba(0, 0, 0, 0.34) 88%, transparent 100%);
  mask-image: radial-gradient(ellipse 50% 126% at 50% 42%, #000 0%, #000 70%, rgba(0, 0, 0, 0.34) 88%, transparent 100%);
}

/* 压暗层放在两层视频之上，两侧亮度一致，不会在溶接处形成台阶 */
.ambient-shade {
  inset: 0;
  width: 100%;
  height: 100%;
  background:
    linear-gradient(90deg, rgba(6, 10, 9, 0.86) 0%, rgba(6, 10, 9, 0.58) 20%, rgba(6, 10, 9, 0.12) 42%, rgba(6, 10, 9, 0) 56%),
    linear-gradient(270deg, rgba(6, 10, 9, 0.52) 0%, rgba(6, 10, 9, 0.16) 22%, rgba(6, 10, 9, 0) 40%),
    linear-gradient(180deg, rgba(6, 10, 9, 0.5) 0%, rgba(6, 10, 9, 0) 24%),
    linear-gradient(0deg, rgba(6, 10, 9, 0.46) 0%, rgba(6, 10, 9, 0) 26%),
    linear-gradient(180deg, rgba(6, 10, 9, 0.14), rgba(6, 10, 9, 0.22));
}

.login-stage {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  max-width: none;
  margin: 0;
  /* 右侧多留一条 36px 的竖排卷号轨道，卡片不会压住 .edition-label */
  padding: var(--gutter) calc(var(--gutter) + 36px) var(--gutter) var(--gutter);
  box-sizing: border-box;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  align-items: start;
  gap: clamp(32px, 5vw, 76px);
}

.login-story {
  align-self: start;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  max-width: 600px;
  text-shadow: 0 2px 18px rgba(0, 0, 0, 0.42);
}

.brand-lockup {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 19px;
  font-weight: 700;
}

.brand-mark {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  color: #121714;
  background: #d8ff5f;
  font-size: 21px;
}

.story-kicker,
.form-heading p,
.form-note,
.edition-label {
  letter-spacing: 2px;
  font-size: 11px;
  font-weight: 700;
}

.story-kicker {
  margin: clamp(40px, 7vh, 74px) 0 18px;
  color: #d8ff5f;
}

.login-story h1 {
  margin: 0;
  font-family: 'Noto Serif SC', 'Songti SC', SimSun, serif;
  font-size: clamp(48px, 6vw, 82px);
  line-height: 1.08;
  letter-spacing: 0;
  font-weight: 600;
}

.story-copy {
  max-width: 470px;
  margin: 20px 0 0;
  color: rgba(245, 240, 231, 0.68);
  font-size: 15px;
  line-height: 1.9;
}

.portrait-strip {
  display: flex;
  align-items: flex-end;
  margin-top: clamp(30px, 5vh, 50px);
  padding-left: 18px;
}

.portrait-card {
  width: 118px;
  margin: 0;
  padding: 7px 7px 9px;
  background: #eee9df;
  color: #1c211f;
  box-shadow: 0 18px 38px rgba(0, 0, 0, 0.34);
}

.portrait-card img {
  display: block;
  width: 100%;
  aspect-ratio: 3 / 4;
  object-fit: cover;
}

.portrait-card figcaption {
  padding-top: 7px;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 10px;
  text-transform: uppercase;
}

.portrait-card-night {
  transform: rotate(-5deg);
}

.portrait-card-day {
  margin-left: -16px;
  transform: translateY(12px) rotate(6deg);
}

.login-box {
  width: 100%;
  max-width: 420px;
  justify-self: end;
  align-self: center;
  box-sizing: border-box;
  padding: 38px 34px 28px;
  background: rgba(248, 246, 240, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.45);
  border-radius: 8px;
  color: #1d2421;
  backdrop-filter: blur(24px);
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.36);
}

.form-heading p {
  margin: 0 0 14px;
  color: #68736c;
}

.form-heading h2 {
  margin: 0 0 8px;
  font-family: 'Noto Serif SC', 'Songti SC', SimSun, serif;
  font-size: 32px;
  letter-spacing: 0;
}

.form-heading span {
  color: #7a837e;
  font-size: 13px;
}

.login-form {
  width: 100%;
  margin-top: 28px;
  display: grid;
  gap: 4px;
}

.login-form :deep(.el-form-item),
.login-form :deep(.el-form-item__content),
.login-form :deep(.gfi) {
  width: 100%;
  min-width: 0;
}

.login-form :deep(.gfi-input) {
  display: block;
  width: 100%;
  box-sizing: border-box;
  border-radius: 4px;
  border-color: #d7d9d3;
  background: #fff;
  color: #1d2421;
  box-shadow: none;
}

.login-form :deep(.gfi-label) {
  color: #7a837e;
}

.login-form :deep(.gfi.focused .gfi-label),
.login-form :deep(.gfi.filled .gfi-label) {
  color: #536b12;
  background: #fff;
}

.login-form :deep(.gfi.focused .gfi-input),
.login-form :deep(.gfi.filled .gfi-input) {
  border-color: #9bb43f;
  box-shadow: 0 0 0 2px rgba(155, 180, 63, 0.14);
}

.login-button {
  width: 100%;
  min-height: 52px;
  margin-top: 4px;
  border-radius: 4px;
  color: #10150f;
  background: #d8ff5f;
  box-shadow: 0 10px 24px rgba(120, 145, 42, 0.24);
}

.login-button:hover {
  background: #c8ee51;
  box-shadow: 0 12px 28px rgba(120, 145, 42, 0.32);
}

.register-link {
  text-align: center;
  margin-top: 14px;
  font-size: 13px;
  color: #7a837e;
}

.register-link a {
  color: #31410c;
  text-decoration: none;
  font-weight: 700;
}

.register-link a:hover {
  color: #6f871d;
}

.form-note {
  margin: 26px 0 0;
  padding-top: 18px;
  border-top: 1px solid #d9ddd7;
  color: #9ca39f;
  text-align: center;
  letter-spacing: 1.4px;
  font-size: 9px;
}

.edition-label {
  position: absolute;
  z-index: 1;
  right: var(--gutter);
  bottom: var(--gutter);
  color: rgba(245, 240, 231, 0.5);
  writing-mode: vertical-rl;
}

@media (max-width: 920px) {
  .login-stage {
    grid-template-columns: 1fr 360px;
    gap: 32px;
  }
}

@media (max-width: 760px) {
  .ambient-video-backdrop {
    display: none;
  }

  /* 窄屏改成整幅出血，底部渐隐进页面背景，同样看不到视频边框 */
  .ambient-video {
    top: 0;
    left: 50%;
    width: 100%;
    height: min(760px, 92vh);
    object-fit: cover;
    object-position: center 26%;
    opacity: 1;
    transform: translateX(-50%);
    filter: none;
    -webkit-mask-image: linear-gradient(to bottom, #000 0%, #000 58%, rgba(0, 0, 0, 0.45) 82%, transparent 100%);
    mask-image: linear-gradient(to bottom, #000 0%, #000 58%, rgba(0, 0, 0, 0.45) 82%, transparent 100%);
  }

  .ambient-shade {
    background: linear-gradient(180deg, rgba(6, 10, 9, 0.6) 0%, rgba(6, 10, 9, 0.32) 34%, rgba(6, 10, 9, 0.86) 78%, rgba(6, 10, 9, 0.96) 100%);
  }

  .login-stage {
    min-height: auto;
    grid-template-columns: 1fr;
    gap: 36px;
    padding: var(--gutter) var(--gutter) 36px;
  }

  .login-story {
    min-height: 430px;
  }

  .story-kicker {
    margin: 54px 0 16px;
  }

  .login-story h1 {
    font-size: clamp(42px, 13vw, 60px);
  }

  .story-copy {
    max-width: 340px;
  }

  .portrait-strip {
    margin-top: 28px;
  }

  .portrait-card {
    width: 100px;
  }

  .login-box {
    max-width: 440px;
    margin: 0 auto;
    padding: 30px 22px 24px;
  }

  .edition-label {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .ambient-video,
  .ambient-video-backdrop {
    display: none;
  }

  .login-container {
    background: #111713 url('/editorial/night-portrait.jpg') center 28% / cover no-repeat;
  }
}
</style>
