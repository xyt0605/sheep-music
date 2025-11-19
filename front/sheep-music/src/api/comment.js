import request from '@/utils/request'

/**
 * 发表评论
 */
export const addComment = (data) => {
  return request({
    url: '/comment',
    method: 'post',
    data
  })
}

/**
 * 获取歌曲评论
 */
export const getSongComments = (songId, params) => {
  return request({
    url: `/comment/song/${songId}`,
    method: 'get',
    params
  })
}

/**
 * 获取评论的回复
 */
export const getCommentReplies = (commentId) => {
  return request({
    url: `/comment/${commentId}/replies`,
    method: 'get'
  })
}

/**
 * 删除评论
 */
export const deleteComment = (commentId) => {
  return request({
    url: `/comment/${commentId}`,
    method: 'delete'
  })
}

/**
 * 点赞/取消点赞评论
 */
export const toggleCommentLike = (commentId) => {
  return request({
    url: `/comment/${commentId}/like`,
    method: 'post'
  })
}

/**
 * 获取用户的评论
 */
export const getUserComments = (userId, params) => {
  return request({
    url: `/comment/user/${userId}`,
    method: 'get',
    params
  })
}

/**
 * 统计歌曲评论数
 */
export const getCommentCount = (songId) => {
  return request({
    url: `/comment/count/${songId}`,
    method: 'get'
  })
}

/**
 * 检查是否点赞
 */
export const hasLikedComment = (commentId) => {
  return request({
    url: `/comment/${commentId}/liked`,
    method: 'get'
  })
}

