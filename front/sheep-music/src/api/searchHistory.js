import request from '@/utils/request'

/**
 * 获取热门搜索关键词（公开接口）
 */
export const getHotSearchKeywords = (limit = 10) => {
  return request({
    url: '/api/user/search-history/hot',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取搜索历史
 */
export const getSearchHistory = () => {
  return request({
    url: '/api/user/search-history',
    method: 'get'
  })
}

/**
 * 添加搜索记录
 */
export const addSearchHistory = (keyword) => {
  return request({
    url: '/api/user/search-history',
    method: 'post',
    data: {
      keyword: keyword
    }
  })
}

/**
 * 删除单条搜索历史
 */
export const deleteSearchHistory = (id) => {
  return request({
    url: `/api/user/search-history/${id}`,
    method: 'delete'
  })
}

/**
 * 清空所有搜索历史
 */
export const clearSearchHistory = () => {
  return request({
    url: '/api/user/search-history/clear',
    method: 'delete'
  })
}

