import request from '@/utils/request'

/**
 * 播放历史 API
 */

/**
 * 添加播放历史记录
 * @param {Object} data - { songId: 123, playDuration: 180 }
 */
export const addPlayHistory = (data) => {
  return request({
    url: '/api/user/play-history',
    method: 'post',
    data
  })
}

/**
 * 获取播放历史列表（分页）
 * @param {Object} params - { page: 0, size: 20 }
 */
export const getPlayHistoryList = (params) => {
  return request({
    url: '/api/user/play-history/list',
    method: 'get',
    params
  })
}

/**
 * 统计播放历史数量
 */
export const getPlayHistoryCount = () => {
  return request({
    url: '/api/user/play-history/count',
    method: 'get'
  })
}

/**
 * 清空播放历史
 */
export const clearPlayHistory = () => {
  return request({
    url: '/api/user/play-history/clear',
    method: 'delete'
  })
}

/**
 * 删除单条播放历史
 * @param {Number} id - 历史记录ID
 */
export const deletePlayHistory = (id) => {
  return request({
    url: `/api/user/play-history/${id}`,
    method: 'delete'
  })
}

/**
 * 获取最近播放的歌曲
 * @param {Number} limit - 数量限制
 */
export const getRecentPlayHistory = (limit = 10) => {
  return request({
    url: '/api/user/play-history/recent',
    method: 'get',
    params: { limit }
  })
}

