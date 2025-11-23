<template>
  <div class="chat-page">
    <!-- 左侧：会话列表 -->
    <div class="conversation-list">
      <div class="list-header">
        <h3>聊天</h3>
        <el-button text @click="refreshConversations">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
      
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索好友"
          prefix-icon="Search"
          clearable
        />
      </div>
      
      <div v-loading="loading" class="conversations">
        <div v-if="filteredConversations.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无会话" />
        </div>
        
        <div
          v-for="conv in filteredConversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ active: currentFriendId === conv.friendId }"
          @click="selectConversation(conv)"
        >
          <el-badge 
            :value="conv.unreadCount" 
            :hidden="!conv.unreadCount"
            :max="99"
            class="conversation-badge"
          >
            <el-avatar :src="conv.friendAvatar" :size="45">
              <el-icon><User /></el-icon>
            </el-avatar>
          </el-badge>
          
          <div class="conv-info">
            <div class="conv-name">{{ conv.friendName }}</div>
            <div class="conv-message">{{ formatConversationMessage(conv) }}</div>
          </div>
          
          <div class="conv-time">
            {{ formatTime(conv.lastMessageTime) }}
          </div>
        </div>
      </div>
    </div>
    
    <!-- 右侧：聊天窗口 -->
    <div class="chat-window">
      <div v-if="currentFriend" class="chat-container">
        <!-- 聊天头部 -->
        <div class="chat-header">
          <el-avatar :src="currentFriend.avatar" :size="40">
            <el-icon><User /></el-icon>
          </el-avatar>
          <div class="header-info">
            <div class="friend-name">{{ currentFriend.nickname || currentFriend.username }}</div>
            <div class="friend-status">在线</div>
          </div>
        </div>
        
        <!-- 消息列表 -->
        <div ref="messageListRef" class="message-list" v-loading="messagesLoading">
          <div v-if="messages.length === 0 && !messagesLoading" class="empty-messages">
            <el-empty description="开始聊天吧~" />
          </div>
          
          <div
            v-for="message in messages"
            :key="message.id || message.clientId"
            class="message-item"
            :class="{ 'is-self': message.senderId === userStore.userInfo?.id }"
          >
            <el-avatar
              v-if="message.senderId !== userStore.userInfo?.id"
              :src="currentFriend?.avatar"
              :size="35"
            >
              <el-icon><User /></el-icon>
            </el-avatar>
            
            <div class="message-content">
              <div class="message-bubble" :class="{ 'is-self': message.senderId === userStore.userInfo?.id }">
                <div v-if="message.type === 'text' && !message.isRecalled" class="message-text">
                  {{ message.content }}
                </div>
                <div v-else-if="message.type === 'song' && !message.isRecalled" class="message-share" @click="handleViewSharedSong(message)">
                  <el-image 
                    v-if="message.shareData?.cover"
                    :src="message.shareData.cover" 
                    fit="cover"
                    class="share-cover"
                  >
                    <template #error>
                      <div class="share-cover-error">
                        <el-icon><VideoPlay /></el-icon>
                      </div>
                    </template>
                  </el-image>
                  <div v-else class="share-cover-error">
                    <el-icon><VideoPlay /></el-icon>
                  </div>
                  <div class="share-content">
                    <div class="share-title">{{ message.shareData?.name || '歌曲' }}</div>
                    <div class="share-subtitle">{{ message.shareData?.artist || '' }}</div>
                  </div>
                </div>
                <div v-else-if="message.type === 'playlist' && !message.isRecalled" class="message-share" @click="handleViewSharedPlaylist(message)">
                  <el-image 
                    v-if="message.shareData?.cover"
                    :src="message.shareData.cover" 
                    fit="cover"
                    class="share-cover"
                  >
                    <template #error>
                      <div class="share-cover-error">
                        <el-icon><List /></el-icon>
                      </div>
                    </template>
                  </el-image>
                  <div v-else class="share-cover-error">
                    <el-icon><List /></el-icon>
                  </div>
                  <div class="share-content">
                    <div class="share-title">{{ message.shareData?.name || '歌单' }}</div>
                    <div class="share-subtitle">{{ message.shareData?.songCount || 0 }} 首歌曲</div>
                  </div>
                </div>
                <div v-if="message.isRecalled" class="message-recalled">
                  [消息已撤回]
                </div>
              </div>
              <div class="message-time">
                {{ formatMessageTime(message.createTime) }}
                <el-button
                  v-if="message.senderId === userStore.userInfo?.id && !message.isRecalled && message.id"
                  text
                  size="small"
                  class="recall-btn"
                  @click="handleRecallMessage(message)"
                >撤回</el-button>
              </div>
            </div>
            
            <el-avatar
              v-if="message.senderId === userStore.userInfo?.id"
              :src="userStore.userInfo?.avatar"
              :size="35"
            >
              <el-icon><User /></el-icon>
            </el-avatar>
          </div>
        </div>
        
        <!-- 输入框 -->
        <div class="chat-input">
          <el-input
            v-model="messageContent"
            type="textarea"
            :rows="3"
            placeholder="输入消息..."
            maxlength="2000"
            show-word-limit
            resize="none"
            @keyup.ctrl.enter="handleSendMessage"
          />
          <div class="input-actions">
            <el-button-group>
              <el-button text @click="handleShareSong">
                <el-icon><VideoPlay /></el-icon>
                分享歌曲
              </el-button>
              <el-button text @click="handleSharePlaylist">
                <el-icon><List /></el-icon>
                分享歌单
              </el-button>
            </el-button-group>
            <el-button
              type="primary"
              @click="handleSendMessage"
              :loading="sending"
              :disabled="!messageContent.trim()"
            >
              发送
            </el-button>
          </div>
        </div>
      </div>
      
      <div v-else class="empty-chat">
        <el-empty description="选择一个好友开始聊天" />
      </div>
    </div>
    
    <!-- 歌曲选择器 -->
    <SongSelector v-model="showSongSelector" @select="handleSongSelected" />
    
    <!-- 歌单选择器 -->
    <PlaylistShareSelector v-model="showPlaylistSelector" @select="handlePlaylistSelected" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User,
  Refresh,
  Search,
  VideoPlay,
  List
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useSocialStore } from '@/store/social'
import { usePlayerStore } from '@/store/player'
import SongSelector from '@/components/SongSelector.vue'
import PlaylistShareSelector from '@/components/PlaylistShareSelector.vue'
import {
  getChatHistory,
  markAllAsReadFrom,
  recallMessage
} from '@/api/chat'
import { getConversations } from '@/api/chat'
import { getFriendList } from '@/api/friend'
import wsClient from '@/ws/client'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const socialStore = useSocialStore()
const playerStore = usePlayerStore()

const loading = ref(false)
const messagesLoading = ref(false)
const sending = ref(false)
const searchKeyword = ref('')
const conversations = ref([])
const currentFriendId = ref(null)
const currentFriend = ref(null)
const messages = ref([])
const messageContent = ref('')
const messageListRef = ref(null)
const page = ref(0)
const hasMore = ref(true)
const showSongSelector = ref(false)
const showPlaylistSelector = ref(false)

// 过滤会话
const filteredConversations = computed(() => {
  if (!searchKeyword.value) {
    return conversations.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return conversations.value.filter(conv =>
    conv.friendName.toLowerCase().includes(keyword)
  )
})

// 格式化会话消息显示
const formatConversationMessage = (conv) => {
  if (!conv.lastMessage) return '暂无消息'
  
  let msg = conv.lastMessage
  
  // 如果已经是友好格式（后端已处理），直接返回
  if (msg === '[分享了歌曲]' || msg === '[分享了歌单]') {
    return msg
  }
  
  // 兼容旧数据：尝试判断是否是 JSON 格式的分享消息
  if (msg.startsWith('{') || msg.startsWith('[')) {
    try {
      const data = JSON.parse(msg)
      // 判断是歌曲分享还是歌单分享
      if (data.id && data.title) {
        return '[分享了歌曲]'
      } else if (data.id && data.name) {
        return '[分享了歌单]'
      }
    } catch (e) {
      // 解析失败，继续正常显示
    }
  }
  
  // 普通文本消息，如果太长则截断
  if (msg.length > 30) {
    return msg.substring(0, 30) + '...'
  }
  
  return msg
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) {
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    })
  } else if (days === 1) {
    return '昨天'
  } else if (days < 7) {
    return `${days}天前`
  } else {
    return date.toLocaleDateString('zh-CN', {
      month: '2-digit',
      day: '2-digit'
    })
  }
}

// 格式化消息时间
const formatMessageTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 更新会话列表中的好友信息（同步最新头像和昵称）
const updateFriendInfoInConversations = async () => {
  try {
    const friendsRes = await getFriendList()
    if (friendsRes.code === 200 && friendsRes.data) {
      const friendMap = new Map()
      friendsRes.data.forEach(f => {
        if (f.friend) {
          friendMap.set(f.friendId, {
            avatar: f.friend.avatar,
            nickname: f.friend.nickname || f.friend.username
          })
        }
      })
      
      // 更新会话列表中的好友信息
      conversations.value = conversations.value.map(conv => {
        const friendInfo = friendMap.get(conv.friendId)
        if (friendInfo) {
          return {
            ...conv,
            friendAvatar: friendInfo.avatar,
            friendName: friendInfo.nickname
          }
        }
        return conv
      })
      
      // 如果当前正在聊天，也更新当前好友信息
      if (currentFriendId.value) {
        const friendInfo = friendMap.get(currentFriendId.value)
        if (friendInfo && currentFriend.value) {
          currentFriend.value = {
            ...currentFriend.value,
            avatar: friendInfo.avatar,
            nickname: friendInfo.nickname
          }
        }
      }
    }
  } catch (error) {
    console.error('更新好友信息失败:', error)
  }
}

// 加载会话列表
const loadConversations = async () => {
  loading.value = true
  try {
    const res = await getConversations()
    if (res.code === 200) {
      conversations.value = res.data || []
      // 加载完会话列表后，立即同步最新的好友信息
      await updateFriendInfoInConversations()
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 刷新会话列表
const refreshConversations = () => {
  loadConversations()
  socialStore.loadConversations()
}

// 选择会话
const selectConversation = (conv) => {
  // 仅进行路由跳转，数据加载交给 watch 处理
  if (Number(route.params.id) !== conv.friendId) {
    router.push(`/chat/${conv.friendId}`)
  }
}

// 加载聊天记录
const loadChatHistory = async (friendId, reset = true) => {
  if (reset) {
    page.value = 0
    messages.value = []
    hasMore.value = true
  }
  
  messagesLoading.value = true
  try {
    const res = await getChatHistory(friendId, {
      page: page.value,
      size: 50
    })
    
    if (res.code === 200) {
      const data = res.data
      const historyMessages = (data.content || []).map(msg => {
        // 解析分享数据
        if ((msg.type === 'song' || msg.type === 'playlist') && msg.content && !msg.shareData) {
          try {
            msg.shareData = JSON.parse(msg.content)
          } catch (e) {
            console.error('解析分享数据失败:', e)
          }
        }
        return msg
      })
      
      if (reset) {
        messages.value = historyMessages.reverse()
      } else {
        messages.value = [...historyMessages.reverse(), ...messages.value]
      }
      
      hasMore.value = !data.first
      
      // 滚动到底部
      await nextTick()
      scrollToBottom()
    }
  } catch (error) {
    console.error('加载聊天记录失败:', error)
  } finally {
    messagesLoading.value = false
  }
}

// 撤回消息
const handleRecallMessage = async (message) => {
  if (!message || !message.id) return
  try {
    await ElMessageBox.confirm('确定撤回这条消息吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (_) {
    return
  }

  try {
    const res = await recallMessage(message.id)
    if (res.code === 200) {
      message.isRecalled = true
      if (message.type === 'text') message.content = ''
      ElMessage.success('已撤回')
    }
  } catch (error) {
    console.error('撤回消息失败:', error)
    ElMessage.error('撤回失败')
  }
}

// 发送消息
const handleSendMessage = async () => {
  if (!messageContent.value.trim() || !currentFriendId.value) return
  
  sending.value = true
  try {
    const payload = {
      receiverId: currentFriendId.value,
      type: 'text',
      content: messageContent.value.trim()
    }
    // 通过 WebSocket 发布消息
    wsClient.sendChatMessage(payload)

    // 乐观更新本地消息列表
    const tempId = `tmp-${Date.now()}`
    messages.value.push({
      clientId: tempId,
      local: true,
      senderId: userStore.userInfo?.id,
      receiverId: currentFriendId.value,
      type: 'text',
      content: payload.content,
      createTime: new Date().toISOString(),
      isRecalled: false
    })
    messageContent.value = ''
    
    // 滚动到底部
    await nextTick()
    scrollToBottom()
    
    // 刷新会话列表
    await loadConversations()
  } catch (error) {
    console.error('发送消息失败:', error)
  } finally {
    sending.value = false
  }
}

// 分享歌曲
const handleShareSong = () => {
  if (!currentFriendId.value) {
    ElMessage.warning('请先选择聊天对象')
    return
  }
  showSongSelector.value = true
}

// 分享歌单
const handleSharePlaylist = () => {
  if (!currentFriendId.value) {
    ElMessage.warning('请先选择聊天对象')
    return
  }
  showPlaylistSelector.value = true
}

// 处理选择的歌曲
const handleSongSelected = async (song) => {
  if (!song || !currentFriendId.value) return
  
  sending.value = true
  try {
    const payload = {
      receiverId: currentFriendId.value,
      type: 'song',
      content: JSON.stringify({
        id: song.id,
        name: song.name,
        artist: song.artist,
        cover: song.cover
      })
    }
    
    // 通过 WebSocket 发送分享消息
    wsClient.sendChatMessage(payload)
    
    // 乐观更新本地消息列表
    const tempId = `tmp-${Date.now()}`
    messages.value.push({
      clientId: tempId,
      local: true,
      senderId: userStore.userInfo?.id,
      receiverId: currentFriendId.value,
      type: 'song',
      content: payload.content,
      shareData: {
        id: song.id,
        name: song.name,
        artist: song.artist,
        cover: song.cover
      },
      createTime: new Date().toISOString(),
      isRecalled: false
    })
    
    // 滚动到底部
    await nextTick()
    scrollToBottom()
    
    // 刷新会话列表
    await loadConversations()
    
    ElMessage.success('分享成功')
  } catch (error) {
    console.error('分享歌曲失败:', error)
    ElMessage.error('分享失败')
  } finally {
    sending.value = false
  }
}

// 处理选择的歌单
const handlePlaylistSelected = async (playlist) => {
  if (!playlist || !currentFriendId.value) return
  
  sending.value = true
  try {
    const payload = {
      receiverId: currentFriendId.value,
      type: 'playlist',
      content: JSON.stringify({
        id: playlist.id,
        name: playlist.name,
        cover: playlist.cover,
        songCount: playlist.songCount
      })
    }
    
    // 通过 WebSocket 发送分享消息
    wsClient.sendChatMessage(payload)
    
    // 乐观更新本地消息列表
    const tempId = `tmp-${Date.now()}`
    messages.value.push({
      clientId: tempId,
      local: true,
      senderId: userStore.userInfo?.id,
      receiverId: currentFriendId.value,
      type: 'playlist',
      content: payload.content,
      shareData: {
        id: playlist.id,
        name: playlist.name,
        cover: playlist.cover,
        songCount: playlist.songCount
      },
      createTime: new Date().toISOString(),
      isRecalled: false
    })
    
    // 滚动到底部
    await nextTick()
    scrollToBottom()
    
    // 刷新会话列表
    await loadConversations()
    
    ElMessage.success('分享成功')
  } catch (error) {
    console.error('分享歌单失败:', error)
    ElMessage.error('分享失败')
  } finally {
    sending.value = false
  }
}

// 查看分享的歌曲
const handleViewSharedSong = async (message) => {
  if (!message.shareData && message.content) {
    try {
      message.shareData = JSON.parse(message.content)
    } catch (e) {
      console.error('解析歌曲数据失败:', e)
      ElMessage.error('歌曲数据解析失败')
      return
    }
  }
  
  console.log('点击分享的歌曲:', message.shareData)
  
  if (!message.shareData?.id) {
    ElMessage.warning('歌曲信息不完整')
    return
  }
  
  try {
    // 如果歌曲数据没有 url，需要先获取完整信息
    if (!message.shareData.url) {
      ElMessage.info('正在加载歌曲...')
      const { getMusicDetail } = await import('@/api/song')
      const res = await getMusicDetail(message.shareData.id)
      if (res.code === 200 && res.data) {
        // 合并分享数据和完整数据
        const fullSongData = {
          ...message.shareData,
          ...res.data
        }
        playerStore.play(fullSongData)
        ElMessage.success('开始播放')
      } else {
        ElMessage.error('获取歌曲信息失败')
      }
    } else {
      // 已有完整数据，直接播放
      playerStore.play(message.shareData)
      ElMessage.success('开始播放')
    }
  } catch (error) {
    console.error('播放歌曲失败:', error)
    ElMessage.error('播放失败')
  }
}

// 查看分享的歌单
const handleViewSharedPlaylist = (message) => {
  if (!message.shareData && message.content) {
    try {
      message.shareData = JSON.parse(message.content)
    } catch (e) {
      console.error('解析歌单数据失败:', e)
    }
  }
  
  if (message.shareData?.id) {
    // 跳转到歌单详情页
    router.push(`/playlist/${message.shareData.id}`)
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// 监听路由变化
watch(() => route.params.id, async (id) => {
  if (id) {
    const fid = Number(id)
    
    // 1. 立即更新当前ID
    currentFriendId.value = fid
    
    // 2. 尝试从当前会话列表中立即获取好友信息（实现无缝切换头部）
    const conv = conversations.value.find(c => c.friendId === fid)
    if (conv) {
      currentFriend.value = {
        id: fid,
        nickname: conv.friendName,
        avatar: conv.friendAvatar
      }
    }
    
    // 2.1 异步获取最新的好友信息
    try {
      const friendsRes = await getFriendList()
      if (friendsRes.code === 200) {
        const friend = friendsRes.data.find(f => f.friendId === fid)
        if (friend && friend.friend) {
          currentFriend.value = {
            id: fid,
            nickname: friend.friend.nickname || friend.friend.username,
            avatar: friend.friend.avatar,
            username: friend.friend.username
          }
        }
      }
    } catch (e) {
      console.error('获取最新好友信息失败:', e)
    }
    
    // 3. 加载聊天记录
    await loadChatHistory(fid, true)
    
    // 4. 标记已读等副作用
    try {
      await markAllAsReadFrom(fid)
      await socialStore.updateUnreadMessageCount()
      // 如果列表里没有这个会话（比如新聊天），或者需要更新未读数，则刷新列表
      if (!conv || conv.unreadCount > 0) {
        await loadConversations()
      }
    } catch (_) {}
    
    // 5. 如果刚才没在列表里找到好友信息（比如通过URL直接访问），现在尝试获取详情
    if (!currentFriend.value || currentFriend.value.id !== fid) {
      try {
        const friendsRes = await getFriendList()
        if (friendsRes.code === 200) {
          const friend = friendsRes.data.find(f => f.friendId === fid)
          if (friend) {
            currentFriend.value = friend.friend || {
              id: fid,
              nickname: friend.friendName,
              avatar: friend.friendAvatar
            }
          }
        }
      } catch (e) {}
    }
  } else {
    // 没有ID时才置空
    currentFriendId.value = null
    currentFriend.value = null
    messages.value = []
  }
}, { immediate: true })

onMounted(async () => {
  // 先加载会话列表，这样 watch 里的同步查找才能命中
  await loadConversations()
  
  // 如果有参数但还没加载出好友信息（因为watch先于onMounted执行，当时conversations可能为空）
  if (route.params.id && !currentFriend.value) {
    const fid = Number(route.params.id)
    const conv = conversations.value.find(c => c.friendId === fid)
    if (conv) {
      currentFriend.value = {
        id: fid,
        nickname: conv.friendName,
        avatar: conv.friendAvatar
      }
    }
  }
  // 订阅聊天消息 - 用于更新会话列表
  const offChatForConversations = wsClient.onChatMessage(async (msg) => {
    try {
      if (!msg) return
      
      // 无论是否在当前聊天窗口，都更新会话列表
      // 这样会话列表的最后消息和时间会实时更新
      await loadConversations()
    } catch (error) {
      console.error('更新会话列表失败:', error)
    }
  })
  
  // 订阅聊天消息，匹配当前会话则追加显示
  const offChat = wsClient.onChatMessage(async (msg) => {
    try {
      if (!msg || !currentFriendId.value) return
      const fid = currentFriendId.value
      const isCurrentChat = msg.senderId === fid || msg.receiverId === fid
      
      if (isCurrentChat) {
        // 解析分享数据
        if ((msg.type === 'song' || msg.type === 'playlist') && msg.content && !msg.shareData) {
          try {
            msg.shareData = JSON.parse(msg.content)
          } catch (e) {
            console.error('解析分享数据失败:', e)
          }
        }
        
        // 如果是自己发出的消息，尝试用服务端消息替换本地临时消息，避免重复
        const isSelf = msg.senderId === userStore.userInfo?.id
        if (isSelf) {
          const isNear = (a, b) => {
            const ta = new Date(a).getTime()
            const tb = new Date(b).getTime()
            return Math.abs(ta - tb) < 10000 // 10秒内视为同一条
          }
          const idx = messages.value.findIndex(m =>
            m.local === true &&
            m.senderId === msg.senderId &&
            m.receiverId === msg.receiverId &&
            m.type === msg.type &&
            m.content === msg.content &&
            m.createTime && msg.createTime && isNear(m.createTime, msg.createTime)
          )
          if (idx !== -1) {
            messages.value[idx] = { ...msg }
          } else {
            messages.value.push(msg)
          }
        } else {
          // 收到对方的消息
          messages.value.push(msg)
          
          // 立即标记为已读（因为用户正在查看此聊天窗口）
          if (msg.id && msg.senderId === fid) {
            try {
              await markAllAsReadFrom(fid)
              // 通知 social store 更新未读数
              await socialStore.updateUnreadMessageCount()
              // 更新会话列表
              await loadConversations()
            } catch (error) {
              console.error('标记已读失败:', error)
            }
          }
        }
        
        await nextTick()
        scrollToBottom()
      }
    } catch (_) {}
  })

  // 组件卸载时移除订阅
  onUnmounted(() => {
    if (typeof offChat === 'function') offChat()
    if (typeof offChatForConversations === 'function') offChatForConversations()
  })
})
</script>

<style scoped>
.chat-page {
  display: flex;
  height: calc(100vh - 120px);
  max-width: 1400px;
  margin: 0 auto;
  background: var(--card-bg, #fff);
  border-radius: 8px;
  overflow: hidden;
}

.conversation-list {
  width: 300px;
  border-right: 1px solid var(--border-color, #eee);
  display: flex;
  flex-direction: column;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid var(--border-color, #eee);
}

.list-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.search-box {
  padding: 12px;
  border-bottom: 1px solid var(--border-color, #eee);
}

.conversations {
  flex: 1;
  overflow-y: auto;
}

.empty-state {
  padding: 40px 20px;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.conversation-item:hover {
  background: var(--bg-hover, #f5f5f5);
}

.conversation-item.active {
  background: var(--color-primary-light, #ecf5ff);
}

.conversation-badge :deep(.el-badge__content) {
  background-color: #f56c6c;
  border: 2px solid #fff;
  font-size: 12px;
  height: 20px;
  line-height: 16px;
  padding: 0 6px;
  min-width: 20px;
}

.conversation-badge :deep(.el-badge__content.is-fixed) {
  top: 2px;
  right: 8px;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-name {
  font-weight: 500;
  color: var(--text-primary, #333);
  margin-bottom: 4px;
}

.conv-message {
  font-size: 12px;
  color: var(--text-tertiary, #999);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-time {
  font-size: 12px;
  color: var(--text-tertiary, #999);
}

.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--border-color, #eee);
}

.header-info {
  flex: 1;
}

.friend-name {
  font-weight: 500;
  color: var(--text-primary, #333);
  margin-bottom: 4px;
}

.friend-status {
  font-size: 12px;
  color: var(--text-tertiary, #999);
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: var(--bg-secondary, #f5f5f5);
}

.empty-messages {
  padding: 60px 0;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: flex-start;
}

.message-item.is-self {
  flex-direction: row-reverse;
}

.message-content {
  max-width: 60%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-item.is-self .message-content {
  align-items: flex-end;
}

.message-bubble {
  padding: 10px 14px;
  background: var(--card-bg, #fff);
  border-radius: 8px;
  word-wrap: break-word;
}

.message-bubble.is-self {
  background: var(--color-primary, #409eff);
  color: #fff;
}

.message-text {
  line-height: 1.5;
}

.message-share {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 200px;
}

.message-share:hover {
  background: rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.message-bubble.is-self .message-share {
  background: rgba(255, 255, 255, 0.2);
}

.message-bubble.is-self .message-share:hover {
  background: rgba(255, 255, 255, 0.3);
}

.message-share .el-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.share-cover {
  width: 50px;
  height: 50px;
  border-radius: 6px;
  flex-shrink: 0;
}

.share-cover-error {
  width: 50px;
  height: 50px;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-bubble.is-self .share-cover-error {
  background: rgba(255, 255, 255, 0.2);
}

.share-content {
  flex: 1;
  min-width: 0;
}

.share-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.share-subtitle {
  font-size: 12px;
  opacity: 0.8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-recalled {
  font-style: italic;
  opacity: 0.7;
}

.message-time {
  font-size: 12px;
  color: var(--text-tertiary, #999);
  padding: 0 4px;
}

.chat-input {
  padding: 16px;
  border-top: 1px solid var(--border-color, #eee);
  background: var(--card-bg, #fff);
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.empty-chat {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
</style>



