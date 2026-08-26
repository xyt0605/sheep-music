<template>
  <div class="profile-page">
    <div class="page-header">
      <h2>👤 个人中心</h2>
      <p class="subtitle">
        管理你的个人信息
      </p>
    </div>
    
    <div class="user-info-card">
      <el-card shadow="hover">
        <div class="user-basic-info">
          <div class="avatar-container">
            <el-avatar
              :size="100"
              :src="userStore.userInfo?.avatar"
            >
              {{ userStore.userInfo?.nickname?.charAt(0) }}
            </el-avatar>
            <el-upload
              class="avatar-uploader"
              :action="uploadAction"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload"
              :on-progress="handleUploadProgress"
              :on-error="handleUploadError"
            >
              <el-button
                size="small"
                type="primary"
                :loading="uploadLoading"
              >
                {{ uploadLoading ? '上传中...' : '更换头像' }}
              </el-button>
            </el-upload>
          </div>
          <div class="user-details">
            <h3>{{ userStore.userInfo?.nickname }}</h3>
            <p class="username">
              @{{ userStore.userInfo?.username }}
            </p>
            <el-tag
              v-if="userStore.isAdmin"
              type="danger"
              size="small"
            >
              管理员
            </el-tag>
            <el-tag
              v-else
              type="success"
              size="small"
            >
              普通用户
            </el-tag>
          </div>
        </div>
        
        <el-divider />
        
        <div class="info-list">
          <div class="info-item">
            <span class="label">邮箱：</span>
            <span class="value">{{ userStore.userInfo?.email || '未设置' }}</span>
          </div>
          <div class="info-item">
            <span class="label">注册时间：</span>
            <span class="value">{{ formatDate(userStore.userInfo?.createdAt) }}</span>
          </div>
        </div>
        
        <el-divider />
        
        <div class="actions">
          <el-button
            type="primary"
            @click="showEditDialog"
          >
            编辑资料
          </el-button>
          <el-button @click="showPasswordDialog">
            修改密码
          </el-button>
        </div>
      </el-card>
    </div>
    
    <!-- 编辑资料对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑资料"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="80px"
      >
        <el-form-item
          label="昵称"
          prop="nickname"
        >
          <el-input
            v-model="editForm.nickname"
            placeholder="请输入昵称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item
          label="邮箱"
          prop="email"
        >
          <el-input
            v-model="editForm.email"
            placeholder="请输入邮箱"
            type="email"
          />
        </el-form-item>
        
        <el-form-item
          label="头像"
          prop="avatar"
        >
          <div class="avatar-edit">
            <el-avatar
              :size="80"
              :src="editForm.avatar"
            >
              {{ editForm.nickname?.charAt(0) }}
            </el-avatar>
            <div class="avatar-actions">
              <el-input
                v-model="editForm.avatar"
                placeholder="头像URL（可选，建议使用上传功能）"
                type="url"
                style="margin-bottom: 10px;"
              />
              <el-text
                type="info"
                size="small"
              >
                提示：关闭对话框后，可点击个人中心的"更换头像"按钮上传图片
              </el-text>
            </div>
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="editLoading"
            @click="handleEditSubmit"
          >
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item
          label="旧密码"
          prop="oldPassword"
        >
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入旧密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item
          label="新密码"
          prop="newPassword"
        >
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码（6-20个字符）"
            show-password
          />
        </el-form-item>
        
        <el-form-item
          label="确认新密码"
          prop="confirmPassword"
        >
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="passwordLoading"
            @click="handlePasswordSubmit"
          >
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { updateUser, updatePassword } from '@/api/user'

export default {
  name: 'Profile',
  setup() {
    const userStore = useUserStore()
    const editFormRef = ref(null)
    const passwordFormRef = ref(null)
    const editDialogVisible = ref(false)
    const passwordDialogVisible = ref(false)
    const editLoading = ref(false)
    const passwordLoading = ref(false)
    const uploadLoading = ref(false)
    
    // 上传配置
    const uploadAction = '/api/upload/avatar'
    const uploadHeaders = computed(() => ({
      Authorization: `Bearer ${localStorage.getItem('token')}`
    }))
    
    // 编辑表单数据
    const editForm = reactive({
      nickname: '',
      email: '',
      avatar: ''
    })
    
    // 密码表单数据
    const passwordForm = reactive({
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
    
    // 日期格式化
    const formatDate = (dateString) => {
      if (!dateString) return '未知'
      const date = new Date(dateString)
      return date.toLocaleDateString('zh-CN')
    }
    
    // 验证邮箱
    const validateEmail = (rule, value, callback) => {
      if (value && !/^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/.test(value)) {
        callback(new Error('邮箱格式不正确'))
      } else {
        callback()
      }
    }
    
    // 验证确认密码
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== passwordForm.newPassword) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    
    // 编辑表单验证规则
    const editRules = {
      nickname: [
        { required: true, message: '请输入昵称', trigger: 'blur' },
        { min: 1, max: 50, message: '昵称长度为1-50个字符', trigger: 'blur' }
      ],
      email: [
        { validator: validateEmail, trigger: 'blur' }
      ]
    }
    
    // 密码表单验证规则
    const passwordRules = {
      oldPassword: [
        { required: true, message: '请输入旧密码', trigger: 'blur' }
      ],
      newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请再次输入新密码', trigger: 'blur' },
        { validator: validateConfirmPassword, trigger: 'blur' }
      ]
    }
    
    // 显示编辑对话框
    const showEditDialog = () => {
      // 填充当前用户信息
      editForm.nickname = userStore.userInfo?.nickname || ''
      editForm.email = userStore.userInfo?.email || ''
      editForm.avatar = userStore.userInfo?.avatar || ''
      editDialogVisible.value = true
    }
    
    // 显示修改密码对话框
    const showPasswordDialog = () => {
      // 清空表单
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      passwordDialogVisible.value = true
    }
    
    // 提交编辑表单
    const handleEditSubmit = async () => {
      if (!editFormRef.value) return
      
      await editFormRef.value.validate(async (valid) => {
        if (!valid) return
        
        try {
          editLoading.value = true
          
          // 调用更新 API
          const res = await updateUser({
            nickname: editForm.nickname,
            email: editForm.email || null,
            avatar: editForm.avatar || null
          })
          
          // 更新 store 中的用户信息
          userStore.setUserInfo(res.data)
          
          ElMessage.success('资料更新成功')
          editDialogVisible.value = false
        } catch (error) {
          ElMessage.error(error.message || '更新失败')
        } finally {
          editLoading.value = false
        }
      })
    }
    
    // 提交密码表单
    const handlePasswordSubmit = async () => {
      if (!passwordFormRef.value) return
      
      await passwordFormRef.value.validate(async (valid) => {
        if (!valid) return
        
        try {
          passwordLoading.value = true
          
          // 调用修改密码 API
          await updatePassword({
            oldPassword: passwordForm.oldPassword,
            newPassword: passwordForm.newPassword
          })
          
          ElMessage.success('密码修改成功')
          passwordDialogVisible.value = false
          
          // 清空表单
          passwordForm.oldPassword = ''
          passwordForm.newPassword = ''
          passwordForm.confirmPassword = ''
        } catch (error) {
          ElMessage.error(error.message || '修改失败')
        } finally {
          passwordLoading.value = false
        }
      })
    }
    
    // 上传前验证
    const beforeAvatarUpload = (file) => {
      const isImage = file.type.startsWith('image/')
      const isLt2M = file.size / 1024 / 1024 < 2
      
      if (!isImage) {
        ElMessage.error('只能上传图片文件!')
        return false
      }
      if (!isLt2M) {
        ElMessage.error('图片大小不能超过 2MB!')
        return false
      }
      return true
    }
    
    // 上传进度
    const handleUploadProgress = () => {
      uploadLoading.value = true
    }
    
    // 上传成功
    const handleAvatarSuccess = (response) => {
      uploadLoading.value = false
      
      // 兼容后端返回 Result 包装的数据结构
      const url = (response && response.data && response.data.url) || response && response.url
      
      if (url) {
        // 更新用户头像
        const newUserInfo = { ...userStore.userInfo, avatar: url }
        userStore.setUserInfo(newUserInfo)
        
        // 同步更新到后端
        updateUser({ avatar: url })
          .then(() => {
            ElMessage.success('头像更新成功')
          })
          .catch(() => {
            ElMessage.warning('头像显示成功，但同步失败，请刷新页面')
          })
      } else {
        ElMessage.error((response && response.message) || '上传失败，请重试')
      }
    }
    
    const handleUploadError = (err) => {
      uploadLoading.value = false
      try {
        const res = JSON.parse(err.message)
        ElMessage.error(res.message || '上传失败')
      } catch (e) {
        ElMessage.error('上传失败')
      }
    }
    
    return {
      userStore,
      editFormRef,
      passwordFormRef,
      editDialogVisible,
      passwordDialogVisible,
      editLoading,
      passwordLoading,
      uploadLoading,
      uploadAction,
      uploadHeaders,
      editForm,
      passwordForm,
      editRules,
      passwordRules,
      formatDate,
      showEditDialog,
      showPasswordDialog,
      handleEditSubmit,
      handlePasswordSubmit,
      beforeAvatarUpload,
      handleUploadProgress,
      handleAvatarSuccess,
      handleUploadError
    }
  }
}
</script>

<style scoped>
.profile-page {
  padding: 30px;
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h2 {
  font-size: 28px;
  color: #333;
  margin-bottom: 10px;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

.user-basic-info {
  display: flex;
  align-items: center;
  gap: 30px;
}

.user-details h3 {
  font-size: 24px;
  margin-bottom: 8px;
  color: #333;
}

.username {
  color: #999;
  font-size: 14px;
  margin-bottom: 10px;
}

.info-list {
  padding: 10px 0;
}

.info-item {
  padding: 10px 0;
  display: flex;
}

.info-item .label {
  width: 100px;
  color: #666;
  font-weight: 500;
}

.info-item .value {
  color: #333;
}

.actions {
  display: flex;
  gap: 15px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.avatar-edit {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.avatar-actions {
  flex: 1;
}

/* 响应式 - 部分已由全局样式覆盖 */
@media (max-width: 768px) {
  .profile-page {
    padding: 15px 10px;
  }
  
  .avatar-edit {
    flex-direction: column;
    align-items: center;
  }
  
  .avatar-edit .el-avatar {
    margin-bottom: 15px;
  }
  
  .info-item .label {
    width: 80px;
    font-size: 13px;
  }
  
  .info-item .value {
    font-size: 13px;
  }
}
</style>
