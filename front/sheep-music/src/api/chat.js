import request from '@/utils/request'

/**
 * 发送消息
 */
export const sendMessage = (data) => {
  return request({
    url: '/chat/send',
    method: 'post',
    data
  })
}

/**
 * 获取聊天记录
 */
export const getChatHistory = (friendId, params) => {
  return request({
    url: `/chat/history/${friendId}`,
    method: 'get',
    params
  })
}

/**
 * 获取会话列表
 */
export const getConversations = () => {
  return request({
    url: '/chat/conversations',
    method: 'get'
  })
}

/**
 * 标记消息为已读
 */
export const markMessageAsRead = (messageId) => {
  return request({
    url: `/chat/read/${messageId}`,
    method: 'put'
  })
}

/**
 * 批量标记消息为已读
 */
export const markBatchAsRead = (messageIds) => {
  return request({
    url: '/chat/read/batch',
    method: 'put',
    data: messageIds
  })
}

/**
 * 标记与某人的所有未读消息为已读
 */
export const markAllAsReadFrom = (friendId) => {
  return request({
    url: `/chat/read/all/${friendId}`,
    method: 'put'
  })
}

/**
 * 撤回消息
 */
export const recallMessage = (messageId) => {
  return request({
    url: `/chat/recall/${messageId}`,
    method: 'post'
  })
}

/**
 * 获取未读消息数
 */
export const getUnreadCount = () => {
  return request({
    url: '/chat/unread-count',
    method: 'get'
  })
}

/**
 * 获取来自某人的未读消息数
 */
export const getUnreadCountFrom = (friendId) => {
  return request({
    url: `/chat/unread-count/${friendId}`,
    method: 'get'
  })
}

