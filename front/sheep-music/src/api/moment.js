import request from '@/utils/request'

/**
 * 发布动态
 */
export const publishMoment = (data) => {
  return request({
    url: '/moment',
    method: 'post',
    data
  })
}

/**
 * 获取好友动态
 */
export const getFriendsMoments = (params) => {
  return request({
    url: '/moment/friends',
    method: 'get',
    params
  })
}

/**
 * 获取公开动态
 */
export const getPublicMoments = (params) => {
  return request({
    url: '/moment/public',
    method: 'get',
    params
  })
}

/**
 * 获取用户的动态
 */
export const getUserMoments = (userId, params) => {
  return request({
    url: `/moment/user/${userId}`,
    method: 'get',
    params
  })
}

/**
 * 点赞/取消点赞动态
 */
export const toggleMomentLike = (momentId) => {
  return request({
    url: `/moment/${momentId}/like`,
    method: 'post'
  })
}

/**
 * 评论动态
 */
export const commentMoment = (data) => {
  return request({
    url: '/moment/comment',
    method: 'post',
    data
  })
}

/**
 * 获取动态评论
 */
export const getMomentComments = (momentId, params) => {
  return request({
    url: `/moment/${momentId}/comments`,
    method: 'get',
    params
  })
}

/**
 * 删除动态
 */
export const deleteMoment = (momentId) => {
  return request({
    url: `/moment/${momentId}`,
    method: 'delete'
  })
}

/**
 * 检查是否点赞
 */
export const hasLikedMoment = (momentId) => {
  return request({
    url: `/moment/${momentId}/liked`,
    method: 'get'
  })
}

