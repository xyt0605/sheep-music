<template>
  <div class="register-container">
    <div class="register-box">
      <!-- 标题 -->
      <h2 class="title">Sheep Music</h2>
      <p class="subtitle">创建新账号</p>
      
      <!-- 注册表单 -->
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
          <router-link to="/login">立即登录</router-link>
        </div>
      </el-form>
    </div>
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
  display: flex;
  justify-content: center;
  align-items: center;
  background: radial-gradient(900px 450px at 80% 30%, rgba(245,87,108,0.25), transparent 65%),
              radial-gradient(700px 350px at 0% 0%, rgba(102,126,234,0.25), transparent 60%),
              linear-gradient(135deg, #0f172a 0%, #1f2937 100%);
}

.register-box {
  width: 460px;
  padding: 46px 42px;
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
  font-size: 14px;
  color: var(--text-secondary);
}

.login-link a {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 600;
}

.login-link a:hover {
  text-decoration: underline;
}

/* 响应式 - 全局样式已覆盖部分 */
@media (max-width: 768px) {
  .register-container {
    padding: 15px;
  }
  
  .register-box {
    width: 100%;
    max-width: 460px;
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
