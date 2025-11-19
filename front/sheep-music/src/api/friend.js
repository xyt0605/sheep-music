import request from '@/utils/request'

/**
 * 发送好友请求
 */
export const sendFriendRequest = (data) => {
  return request({
    url: '/friend/request',
    method: 'post',
    data
  })
}

/**
 * 接受好友请求
 */
export const acceptFriendRequest = (friendshipId) => {
  return request({
    url: `/friend/accept/${friendshipId}`,
    method: 'post'
  })
}

/**
 * 拒绝好友请求
 */
export const rejectFriendRequest = (friendshipId) => {
  return request({
    url: `/friend/reject/${friendshipId}`,
    method: 'post'
  })
}

/**
 * 删除好友
 */
export const deleteFriend = (friendshipId) => {
  return request({
    url: `/friend/${friendshipId}`,
    method: 'delete'
  })
}

/**
 * 获取好友列表
 */
export const getFriendList = () => {
  return request({
    url: '/friend/list',
    method: 'get'
  })
}

/**
 * 获取好友请求列表
 */
export const getFriendRequests = () => {
  return request({
    url: '/friend/requests',
    method: 'get'
  })
}

/**
 * 搜索用户
 */
export const searchUsers = (keyword) => {
  return request({
    url: '/friend/search',
    method: 'get',
    params: { keyword }
  })
}

/**
 * 设置备注名
 */
export const setFriendRemark = (friendshipId, remark) => {
  return request({
    url: `/friend/${friendshipId}/remark`,
    method: 'put',
    data: { remark }
  })
}

/**
 * 检查是否为好友
 */
export const areFriends = (friendId) => {
  return request({
    url: `/friend/check/${friendId}`,
    method: 'get'
  })
}

