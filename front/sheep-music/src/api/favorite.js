import request from '@/utils/request'

/**
 * 添加收藏
 */
export const addFavorite = (songId) => {
  return request({
    url: `/api/user/favorite/${songId}`,
    method: 'post'
  })
}

/**
 * 取消收藏
 */
export const removeFavorite = (songId) => {
  return request({
    url: `/api/user/favorite/${songId}`,
    method: 'delete'
  })
}

/**
 * 切换收藏状态
 */
export const toggleFavorite = (songId) => {
  return request({
    url: `/api/user/favorite/toggle/${songId}`,
    method: 'post'
  })
}

/**
 * 检查是否已收藏
 */
export const checkFavorite = (songId) => {
  return request({
    url: `/api/user/favorite/check/${songId}`,
    method: 'get'
  })
}

/**
 * 批量检查收藏状态
 */
export const batchCheckFavorites = (songIds) => {
  return request({
    url: '/api/user/favorite/batch-check',
    method: 'post',
    data: songIds
  })
}

/**
 * 获取我的收藏列表
 */
export const getMyFavorites = (params) => {
  return request({
    url: '/api/user/favorite/list',
    method: 'get',
    params
  })
}

/**
 * 统计我的收藏数量
 */
export const countMyFavorites = () => {
  return request({
    url: '/api/user/favorite/count',
    method: 'get'
  })
}
