<template>
  <div class="friends-page">
    <div class="page-header">
      <h2>我的好友</h2>
      <div class="header-actions">
        <el-button text @click="goToMoments">
          <el-icon><ChatDotRound /></el-icon>
          好友动态
        </el-button>
        <el-button text @click="goToShareSquare">
          <el-icon><MoreFilled /></el-icon>
          分享广场
        </el-button>
        <el-button type="primary" @click="showAddFriendDialog = true">
          <el-icon><Plus /></el-icon>
          添加好友
        </el-button>
      </div>
    </div>
    
    <!-- 好友请求标签页 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="好友列表" name="friends">
        <div v-loading="loading" class="friends-list">
          <div v-if="friends.length === 0 && !loading" class="empty-state">
            <el-empty description="暂无好友，快去添加好友吧~" />
          </div>
          
          <div v-else class="friends-grid">
            <div
              v-for="friendship in friends"
              :key="friendship.id"
              class="friend-card"
            >
              <el-avatar
                :src="friendship.friendAvatar || friendship.friend?.avatar"
                :size="60"
                @click="goToChat(friendship.friendId)"
              >
                <el-icon><User /></el-icon>
              </el-avatar>
              
              <div class="friend-info">
                <div class="friend-name" @click="goToChat(friendship.friendId)">
                  {{ friendship.remark || friendship.friendName || friendship.friend?.nickname }}
                </div>
                <div class="friend-username">
                  @{{ friendship.friend?.username }}
                </div>
              </div>
              
              <div class="friend-actions">
                <el-button
                  text
                  type="primary"
                  @click="goToChat(friendship.friendId)"
                >
                  <el-icon><ChatDotRound /></el-icon>
                  聊天
                </el-button>
                <el-dropdown @command="handleFriendAction">
                  <el-button text>
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        :command="{ action: 'remark', friendship }"
                      >
                        设置备注
                      </el-dropdown-item>
                      <el-dropdown-item
                        :command="{ action: 'delete', friendship }"
                        divided
                      >
                        删除好友
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
      
      <el-tab-pane name="requests">
        <template #label>
          <span>
            好友请求
            <el-badge
              v-if="friendRequestCount > 0"
              :value="friendRequestCount"
              type="danger"
              class="tab-badge"
            />
          </span>
        </template>
        <div v-loading="requestsLoading" class="requests-list">
          <div v-if="friendRequests.length === 0 && !requestsLoading" class="empty-state">
            <el-empty description="暂无好友请求" />
          </div>
          
          <div v-else class="request-items">
            <div
              v-for="request in friendRequests"
              :key="request.id"
              class="request-item"
            >
              <el-avatar
                :src="request.userAvatar"
                :size="50"
              >
                <el-icon><User /></el-icon>
              </el-avatar>
              
              <div class="request-info">
                <div class="request-name">{{ request.userName }}</div>
                <div class="request-time">{{ formatTime(request.createTime) }}</div>
                <div v-if="request.remark" class="request-remark">
                  备注：{{ request.remark }}
                </div>
              </div>
              
              <div class="request-actions">
                <el-button
                  type="primary"
                  @click="handleAcceptRequest(request.id)"
                  :loading="processingRequest === request.id"
                >
                  接受
                </el-button>
                <el-button
                  @click="handleRejectRequest(request.id)"
                  :loading="processingRequest === request.id"
                >
                  拒绝
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
    
    <!-- 添加好友对话框 -->
    <el-dialog
      v-model="showAddFriendDialog"
      title="添加好友"
      width="500px"
    >
      <el-form @submit.prevent="handleSearchUser">
        <el-form-item label="搜索用户">
          <el-input
            v-model="searchKeyword"
            placeholder="输入用户名或昵称"
            @keyup.enter="handleSearchUser"
          >
            <template #append>
              <el-button @click="handleSearchUser" :loading="searching">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      
      <div v-if="searchResults.length > 0" class="search-results">
        <div
          v-for="user in searchResults"
          :key="user.id"
          class="search-result-item"
        >
          <el-avatar :src="user.avatar" :size="40">
            <el-icon><User /></el-icon>
          </el-avatar>
          <div class="result-info">
            <div class="result-name">{{ user.nickname || user.username }}</div>
            <div class="result-username">@{{ user.username }}</div>
          </div>
          <el-button
            type="primary"
            size="small"
            @click="handleSendFriendRequest(user.id)"
            :loading="sendingRequest === user.id"
          >
            添加好友
          </el-button>
        </div>
      </div>
      
      <div v-else-if="searched && !searching" class="empty-results">
        <el-empty description="未找到相关用户" />
      </div>
      
      <template #footer>
        <el-button @click="showAddFriendDialog = false">取消</el-button>
      </template>
    </el-dialog>
    
    <!-- 设置备注对话框 -->
    <el-dialog
      v-model="showRemarkDialog"
      title="设置备注"
      width="400px"
    >
      <el-form>
        <el-form-item label="备注名">
          <el-input
            v-model="remarkText"
            placeholder="输入备注名"
            maxlength="20"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showRemarkDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleSetRemark"
          :loading="settingRemark"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User,
  Plus,
  ChatDotRound,
  MoreFilled,
  Search
} from '@element-plus/icons-vue'
import { useSocialStore } from '@/store/social'
import {
  searchUsers,
  sendFriendRequest,
  acceptFriendRequest,
  rejectFriendRequest,
  deleteFriend,
  setFriendRemark
} from '@/api/friend'

const router = useRouter()
const socialStore = useSocialStore()

const activeTab = ref('friends')
const loading = ref(false)
const requestsLoading = ref(false)
const friends = computed(() => socialStore.friends)
const friendRequests = computed(() => socialStore.friendRequests)
const friendRequestCount = computed(() => socialStore.friendRequestCount)

// 添加好友相关
const showAddFriendDialog = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const searching = ref(false)
const searched = ref(false)
const sendingRequest = ref(null)

// 设置备注相关
const showRemarkDialog = ref(false)
const currentFriendship = ref(null)
const remarkText = ref('')
const settingRemark = ref(false)
const processingRequest = ref(null)

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 标签页切换
const handleTabChange = (tab) => {
  if (tab === 'friends') {
    loadFriends()
  } else if (tab === 'requests') {
    loadFriendRequests()
  }
}

// 加载好友列表
const loadFriends = async () => {
  loading.value = true
  await socialStore.loadFriends()
  loading.value = false
}

// 加载好友请求
const loadFriendRequests = async () => {
  requestsLoading.value = true
  await socialStore.loadFriendRequests()
  requestsLoading.value = false
}

// 搜索用户
const handleSearchUser = async () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  
  searching.value = true
  searched.value = true
  try {
    const res = await searchUsers(searchKeyword.value.trim())
    if (res.code === 200) {
      searchResults.value = res.data || []
    }
  } catch (error) {
    console.error('搜索用户失败:', error)
  } finally {
    searching.value = false
  }
}

// 发送好友请求
const handleSendFriendRequest = async (friendId) => {
  sendingRequest.value = friendId
  try {
    const res = await sendFriendRequest({ friendId })
    if (res.code === 200) {
      ElMessage.success('好友请求已发送')
      showAddFriendDialog.value = false
      searchKeyword.value = ''
      searchResults.value = []
      searched.value = false
    }
  } catch (error) {
    console.error('发送好友请求失败:', error)
  } finally {
    sendingRequest.value = null
  }
}

// 接受好友请求
const handleAcceptRequest = async (friendshipId) => {
  processingRequest.value = friendshipId
  try {
    const res = await acceptFriendRequest(friendshipId)
    if (res.code === 200) {
      ElMessage.success('已接受好友请求')
      await socialStore.loadFriendRequests()
      await socialStore.loadFriends()
    }
  } catch (error) {
    console.error('接受好友请求失败:', error)
  } finally {
    processingRequest.value = null
  }
}

// 拒绝好友请求
const handleRejectRequest = async (friendshipId) => {
  processingRequest.value = friendshipId
  try {
    const res = await rejectFriendRequest(friendshipId)
    if (res.code === 200) {
      ElMessage.success('已拒绝好友请求')
      await socialStore.loadFriendRequests()
    }
  } catch (error) {
    console.error('拒绝好友请求失败:', error)
  } finally {
    processingRequest.value = null
  }
}

// 好友操作
const handleFriendAction = async ({ action, friendship }) => {
  if (action === 'remark') {
    currentFriendship.value = friendship
    remarkText.value = friendship.remark || ''
    showRemarkDialog.value = true
  } else if (action === 'delete') {
    try {
      await ElMessageBox.confirm('确定要删除该好友吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      
      const res = await deleteFriend(friendship.id)
      if (res.code === 200) {
        ElMessage.success('已删除好友')
        await socialStore.loadFriends()
      }
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除好友失败:', error)
      }
    }
  }
}

// 设置备注
const handleSetRemark = async () => {
  if (!currentFriendship.value) return
  
  settingRemark.value = true
  try {
    const res = await setFriendRemark(currentFriendship.value.id, remarkText.value)
    if (res.code === 200) {
      ElMessage.success('备注设置成功')
      showRemarkDialog.value = false
      await socialStore.loadFriends()
    }
  } catch (error) {
    console.error('设置备注失败:', error)
  } finally {
    settingRemark.value = false
  }
}

// 跳转到聊天
const goToChat = (friendId) => {
  router.push(`/chat/${friendId}`)
}

// 跳转到好友动态
const goToMoments = () => {
  router.push('/moments')
}

// 跳转到分享广场
const goToShareSquare = () => {
  router.push('/share-square')
}

onMounted(() => {
  loadFriends()
  loadFriendRequests()
})
</script>

<style scoped>
.friends-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.friends-list,
.requests-list {
  min-height: 400px;
}

.empty-state {
  padding: 60px 0;
}

.tab-badge {
  margin-left: 8px;
}

.friends-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.friend-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--card-bg, #fff);
  border-radius: 8px;
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(0,0,0,0.1));
  transition: all 0.3s;
}

.friend-card:hover {
  box-shadow: var(--shadow-md, 0 4px 12px rgba(0,0,0,0.15));
  transform: translateY(-2px);
}

.friend-info {
  flex: 1;
  min-width: 0;
}

.friend-name {
  font-weight: 500;
  color: var(--text-primary, #333);
  cursor: pointer;
  margin-bottom: 4px;
}

.friend-name:hover {
  color: var(--color-primary, #409eff);
}

.friend-username {
  font-size: 12px;
  color: var(--text-tertiary, #999);
}

.friend-actions {
  display: flex;
  gap: 8px;
}

.request-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.request-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--card-bg, #fff);
  border-radius: 8px;
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(0,0,0,0.1));
}

.request-info {
  flex: 1;
}

.request-name {
  font-weight: 500;
  color: var(--text-primary, #333);
  margin-bottom: 4px;
}

.request-time {
  font-size: 12px;
  color: var(--text-tertiary, #999);
  margin-bottom: 4px;
}

.request-remark {
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.request-actions {
  display: flex;
  gap: 8px;
}

.search-results {
  max-height: 400px;
  overflow-y: auto;
  margin-top: 16px;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-bottom: 1px solid var(--border-color, #eee);
}

.search-result-item:last-child {
  border-bottom: none;
}

.result-info {
  flex: 1;
}

.result-name {
  font-weight: 500;
  color: var(--text-primary, #333);
  margin-bottom: 4px;
}

.result-username {
  font-size: 12px;
  color: var(--text-tertiary, #999);
}

.empty-results {
  padding: 40px 0;
}
</style>

