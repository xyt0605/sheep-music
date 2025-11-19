<template>
  <div class="login-container">
    <div class="login-box">
      <!-- 标题 -->
      <h2 class="title">Sheep Music</h2>
      <p class="subtitle">欢迎回来，请登录</p>

      <!-- 登录表单 -->
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        autocomplete="off"
      >
        <!-- 隐藏的假输入框，用于欺骗浏览器自动填充 -->
        <input type="text" name="fake-username" style="display:none" autocomplete="off">
        <input type="password" name="fake-password" style="display:none" autocomplete="new-password">
        
        <!-- 用户名输入框 -->
        <el-form-item prop="username">
          <GalaxyFormInput
            v-model="loginForm.username"
            label="用户名"
            name="username"
            autocomplete="off"
            @blur="noop"
          />
        </el-form-item>

        <!-- 密码输入框 -->
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

        <!-- 登录按钮 -->
        <el-form-item>
          <GalaxyButton
            :loading="loading"
            size="lg"
            variant="primary"
            class="login-button"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </GalaxyButton>
        </el-form-item>

        <!-- 注册链接 -->
        <div class="register-link">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </div>
      </el-form>
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
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: radial-gradient(800px 400px at 20% 20%, rgba(118,75,162,0.25), transparent 60%),
              radial-gradient(600px 300px at 80% 0%, rgba(102,126,234,0.25), transparent 60%),
              linear-gradient(135deg, #0f172a 0%, #1f2937 100%);
}

.login-box {
  width: 420px;
  padding: 44px 40px;
  background: var(--card-bg);
  border-radius: 16px;
  border: 1px solid var(--border-color-light);
  backdrop-filter: blur(18px);
  box-shadow: var(--shadow-xl);
}

.title {
  text-align: center;
  font-size: 30px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.subtitle {
  text-align: center;
  color: var(--text-tertiary);
  margin-bottom: 26px;
  font-size: 15px;
}

.login-form {
  margin-top: 18px;
  display: grid;
  gap: 6px;
}

.login-button {
  width: 100%;
  margin-top: 6px;
}

.register-link {
  text-align: center;
  margin-top: 18px;
  font-size: 14px;
  color: var(--text-secondary);
}

.register-link a {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 600;
}

.register-link a:hover {
  text-decoration: underline;
}

/* 响应式 - 全局样式已覆盖部分 */
@media (max-width: 768px) {
  .login-container {
    padding: 15px;
  }
  
  .login-box {
    width: 100%;
    max-width: 420px;
    padding: 28px 18px;
  }
  
  .title {
    font-size: 24px;
  }
  
  .subtitle {
    font-size: 13px;
  }
}
</style>
