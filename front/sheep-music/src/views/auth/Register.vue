<template>
  <div class="register-container">
    <section class="register-visual">
      <div class="register-brand">
        <span class="brand-glyph">S</span>
        <strong>Sheep Music</strong>
      </div>

      <div class="register-story">
        <p class="story-kicker">CREATE YOUR LISTENING ROOM · 08/26</p>
        <h1>给下一首歌，<br>留一个位置。</h1>
        <p class="story-copy">
          收藏喜欢的旋律，记录播放过的夜晚，再和朋友分享那些值得循环的片段。
        </p>
      </div>

      <figure class="register-photo register-photo-back">
        <img
          src="/editorial/night-portrait.jpg"
          alt="夜晚人像"
        >
        <figcaption>AFTER DARK · 01</figcaption>
      </figure>
      <figure class="register-photo register-photo-front">
        <img
          src="/editorial/bamboo-portrait.jpg"
          alt="日光人像"
        >
        <figcaption>DAYLIGHT · 02</figcaption>
      </figure>
    </section>

    <section class="register-panel">
      <div class="register-box">
        <div class="form-header">
          <span>NEW MEMBER</span>
          <h2 class="title">
            创建音乐空间
          </h2>
          <p class="subtitle">
            几步完成注册，开始整理你的私人播放列表。
          </p>
        </div>
      
        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
        >
        <!-- 用户名输入框 -->
        <el-form-item prop="username">
          <GalaxyFormInput
            v-model="registerForm.username"
            label="用户名"
            name="reg-username"
            autocomplete="off"
          />
        </el-form-item>
        
        <!-- 密码输入框 -->
        <el-form-item prop="password">
          <GalaxyFormInput
            v-model="registerForm.password"
            type="password"
            label="密码"
            name="reg-password"
            autocomplete="new-password"
          />
        </el-form-item>
        
        <!-- 确认密码输入框 -->
        <el-form-item prop="confirmPassword">
          <GalaxyFormInput
            v-model="registerForm.confirmPassword"
            type="password"
            label="确认密码"
            name="reg-confirm"
            autocomplete="new-password"
          />
        </el-form-item>
        
        <!-- 昵称输入框（可选） -->
        <el-form-item prop="nickname">
          <GalaxyFormInput
            v-model="registerForm.nickname"
            label="昵称（可选）"
            name="reg-nickname"
            autocomplete="off"
          />
        </el-form-item>
        
        <!-- 邮箱输入框（可选） -->
        <el-form-item prop="email">
          <GalaxyFormInput
            v-model="registerForm.email"
            label="邮箱（可选）"
            name="reg-email"
            autocomplete="email"
          />
        </el-form-item>
        
        <!-- 注册按钮 -->
        <el-form-item>
          <GalaxyButton
            :loading="loading"
            size="lg"
            variant="primary"
            class="register-button"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '注册' }}
          </GalaxyButton>
        </el-form-item>
        
        <!-- 登录链接 -->
        <div class="login-link">
          已有账号？
          <router-link to="/login">
            立即登录
          </router-link>
        </div>
        </el-form>
      </div>
    </section>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/user'
import GalaxyFormInput from '@/components/GalaxyFormInput.vue'
import GalaxyButton from '@/components/GalaxyButton.vue'

export default {
  name: 'Register',
  components: {
    GalaxyFormInput,
    GalaxyButton
  },
  setup() {
    const router = useRouter()
    const registerFormRef = ref(null)
    const loading = ref(false)
    
    // 表单数据
    const registerForm = reactive({
      username: '',
      password: '',
      confirmPassword: '',
      nickname: '',
      email: ''
    })
    
    // 自定义验证：确认密码
    const validateConfirmPassword = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请再次输入密码'))
      } else if (value !== registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    
    // 自定义验证：邮箱格式（可选）
    const validateEmail = (rule, value, callback) => {
      if (value === '') {
        callback()  // 邮箱是可选的，可以为空
      } else {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        if (!emailRegex.test(value)) {
          callback(new Error('请输入正确的邮箱格式'))
        } else {
          callback()
        }
      }
    }
    
    // 表单验证规则
    const registerRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' },
        { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, validator: validateConfirmPassword, trigger: 'blur' }
      ],
      email: [
        { validator: validateEmail, trigger: 'blur' }
      ]
    }
    
    // 注册处理
    const handleRegister = async () => {
      // 1. 表单验证
      if (!registerFormRef.value) return
      
      await registerFormRef.value.validate(async (valid) => {
        if (!valid) return
        
        try {
          // 2. 显示加载状态
          loading.value = true
          
          // 3. 调用注册API
          await register({
            username: registerForm.username,
            password: registerForm.password,
            nickname: registerForm.nickname || registerForm.username,  // 昵称为空则用用户名
            email: registerForm.email
          })
          
          // 4. 注册成功
          ElMessage.success('注册成功，请登录')
          
          // 5. 跳转到登录页
          setTimeout(() => {
            router.push('/login')
          }, 1000)
          
        } catch (error) {
          // 6. 注册失败
          ElMessage.error(error.message || '注册失败')
        } finally {
          // 7. 隐藏加载状态
          loading.value = false
        }
      })
    }
    
    return {
      registerFormRef,
      registerForm,
      registerRules,
      loading,
      handleRegister
    }
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(460px, 0.95fr);
  background: #0b0d0b;
}

.register-visual {
  position: relative;
  min-height: 100vh;
  padding: clamp(36px, 5vw, 72px);
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(8, 10, 8, 0.78), rgba(8, 10, 8, 0.18)),
    url('/editorial/night-portrait.jpg') center / cover;
  color: #f7f5ee;
}

.register-visual::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(0deg, rgba(8, 10, 8, 0.88), transparent 56%);
  pointer-events: none;
}

.register-brand,
.register-story,
.register-photo {
  position: relative;
  z-index: 1;
}

.register-brand {
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

.register-story {
  max-width: 640px;
  margin-top: clamp(90px, 17vh, 190px);
}

.story-kicker,
.form-header > span,
.register-photo figcaption {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 0.12em;
}

.story-kicker {
  color: var(--signal-lime);
}

.register-story h1 {
  margin: 24px 0 18px;
  font-family: var(--font-display);
  font-size: clamp(50px, 6vw, 86px);
  line-height: 1.04;
  letter-spacing: 0;
}

.story-copy {
  max-width: 500px;
  color: #d1d4ca;
  font-size: 14px;
  line-height: 1.9;
}

.register-photo {
  position: absolute;
  right: 7%;
  bottom: 7%;
  width: clamp(120px, 12vw, 180px);
  margin: 0;
  padding: 8px 8px 26px;
  background: #f4f1e7;
  color: #171914;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.32);
}

.register-photo img {
  display: block;
  width: 100%;
  aspect-ratio: 0.82;
  object-fit: cover;
}

.register-photo figcaption {
  margin-top: 8px;
  letter-spacing: 0;
}

.register-photo-back {
  right: 22%;
  bottom: 10%;
  transform: rotate(-6deg);
}

.register-photo-front {
  transform: rotate(6deg);
}

.register-panel {
  display: grid;
  min-height: 100vh;
  padding: clamp(28px, 5vw, 76px);
  place-items: center;
  background: var(--bg-secondary);
}

.register-box {
  width: min(100%, 520px);
  padding: clamp(30px, 4vw, 52px);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--surface-raised);
  box-shadow: var(--shadow-lg);
}

.form-header > span {
  color: var(--text-tertiary);
}

.title {
  margin: 16px 0 8px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 38px;
  line-height: 1.08;
  letter-spacing: 0;
}

.subtitle {
  margin: 0 0 24px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.register-form {
  margin-top: 18px;
  display: grid;
  gap: 6px;
}

.register-button {
  width: 100%;
  margin-top: 6px;
}

.login-link {
  text-align: center;
  margin-top: 18px;
  font-size: 13px;
  color: var(--text-secondary);
}

.login-link a {
  color: var(--color-primary-light);
  text-decoration: none;
  font-weight: 600;
}

.login-link a:hover {
  text-decoration: underline;
}

/* 响应式 - 全局样式已覆盖部分 */
@media (max-width: 768px) {
  .register-container {
    grid-template-columns: 1fr;
  }

  .register-visual {
    min-height: 370px;
    padding: 28px 24px;
  }

  .register-story {
    margin-top: 64px;
  }

  .register-story h1 {
    font-size: 44px;
  }

  .story-copy {
    max-width: 80%;
    font-size: 13px;
  }

  .register-photo {
    display: none;
  }

  .register-photo-back {
    display: none;
  }

  .register-panel {
    min-height: auto;
    padding: 22px 14px 34px;
  }
  
  .register-box {
    width: 100%;
    padding: 30px 20px;
  }
  
  .title {
    font-size: 32px;
  }
  
  .subtitle {
    font-size: 13px;
  }
}
</style>
