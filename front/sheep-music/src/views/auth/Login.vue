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
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
            autocomplete="off"
            name="username"
            readonly
            @focus="handleUsernameFocus"
          />
        </el-form-item>

        <!-- 密码输入框 -->
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            autocomplete="new-password"
            name="password"
            readonly
            @focus="handlePasswordFocus"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <!-- 登录按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="login-button"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
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
import { login } from '@/api/user'

export default {
  // eslint-disable-next-line vue/multi-word-component-names
  name: 'Login',
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
      handlePasswordFocus
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.title {
  text-align: center;
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

.subtitle {
  text-align: center;
  color: #999;
  margin-bottom: 30px;
  font-size: 14px;
}

.login-form {
  margin-top: 20px;
}

.login-button {
  width: 100%;
  margin-top: 10px;
}

.register-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.register-link a {
  color: #667eea;
  text-decoration: none;
  font-weight: bold;
}

.register-link a:hover {
  text-decoration: underline;
}
</style>
