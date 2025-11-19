import request from '@/utils/request'

/**
 * 获取通知列表
 */
export const getNotifications = (params) => {
  return request({
    url: '/notification/list',
    method: 'get',
    params
  })
}

/**
 * 获取未读通知
 */
export const getUnreadNotifications = () => {
  return request({
    url: '/notification/unread',
    method: 'get'
  })
}

/**
 * 获取未读通知数
 */
export const getUnreadNotificationCount = () => {
  return request({
    url: '/notification/unread-count',
    method: 'get'
  })
}

/**
 * 标记通知为已读
 */
export const markNotificationAsRead = (notificationId) => {
  return request({
    url: `/notification/read/${notificationId}`,
    method: 'put'
  })
}

/**
 * 批量标记通知为已读
 */
export const markBatchNotificationsAsRead = (notificationIds) => {
  return request({
    url: '/notification/read/batch',
    method: 'put',
    data: notificationIds
  })
}

/**
 * 标记所有通知为已读
 */
export const markAllNotificationsAsRead = () => {
  return request({
    url: '/notification/read/all',
    method: 'put'
  })
}

