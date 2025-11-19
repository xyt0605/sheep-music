import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getFriendList, getFriendRequests } from '@/api/friend'
import { getConversations, getUnreadCount as getChatUnreadCount } from '@/api/chat'
import { getUnreadNotificationCount } from '@/api/notification'

export const useSocialStore = defineStore('social', () => {
  // 好友相关
  const friends = ref([])
  const friendRequests = ref([])
  
  // 聊天相关
  const conversations = ref([])
  const currentChatFriend = ref(null)
  const unreadMessageCount = ref(0)
  
  // 通知相关
  const unreadNotificationCount = ref(0)
  
  // 计算属性
  const friendRequestCount = computed(() => friendRequests.value.length)
  
  // 加载好友列表
  const loadFriends = async () => {
    try {
      const res = await getFriendList()
      if (res.code === 200) {
        friends.value = res.data || []
      }
    } catch (error) {
      console.error('加载好友列表失败:', error)
    }
  }
  
  // 加载好友请求
  const loadFriendRequests = async () => {
    try {
      const res = await getFriendRequests()
      if (res.code === 200) {
        friendRequests.value = res.data || []
      }
    } catch (error) {
      console.error('加载好友请求失败:', error)
    }
  }
  
  // 加载会话列表
  const loadConversations = async () => {
    try {
      const res = await getConversations()
      if (res.code === 200) {
        conversations.value = res.data || []
      }
    } catch (error) {
      console.error('加载会话列表失败:', error)
    }
  }
  
  // 更新未读消息数
  const updateUnreadMessageCount = async () => {
    try {
      const res = await getChatUnreadCount()
      if (res.code === 200) {
        unreadMessageCount.value = res.data || 0
      }
    } catch (error) {
      console.error('获取未读消息数失败:', error)
    }
  }
  
  // 更新未读通知数
  const updateUnreadNotificationCount = async () => {
    try {
      const res = await getUnreadNotificationCount()
      if (res.code === 200) {
        unreadNotificationCount.value = res.data || 0
      }
    } catch (error) {
      console.error('获取未读通知数失败:', error)
    }
  }
  
  // 初始化社交数据
  const initSocialData = async () => {
    // 使用 Promise.allSettled 确保即使某个API失败也不影响其他API
    await Promise.allSettled([
      loadFriends(),
      loadFriendRequests(),
      loadConversations(),
      updateUnreadMessageCount(),
      updateUnreadNotificationCount()
    ])
  }
  
  // 设置当前聊天对象
  const setCurrentChatFriend = (friend) => {
    currentChatFriend.value = friend
  }
  
  // 添加好友到列表
  const addFriend = (friend) => {
    if (!friends.value.find(f => f.id === friend.id)) {
      friends.value.push(friend)
    }
  }
  
  // 移除好友
  const removeFriend = (friendshipId) => {
    friends.value = friends.value.filter(f => f.id !== friendshipId)
  }
  
  // 添加好友请求
  const addFriendRequest = (request) => {
    if (!friendRequests.value.find(r => r.id === request.id)) {
      friendRequests.value.push(request)
    }
  }
  
  // 移除好友请求
  const removeFriendRequest = (friendshipId) => {
    friendRequests.value = friendRequests.value.filter(r => r.id !== friendshipId)
  }
  
  return {
    // 状态
    friends,
    friendRequests,
    conversations,
    currentChatFriend,
    unreadMessageCount,
    unreadNotificationCount,
    
    // 计算属性
    friendRequestCount,
    
    // 方法
    loadFriends,
    loadFriendRequests,
    loadConversations,
    updateUnreadMessageCount,
    updateUnreadNotificationCount,
    initSocialData,
    setCurrentChatFriend,
    addFriend,
    removeFriend,
    addFriendRequest,
    removeFriendRequest
  }
})

