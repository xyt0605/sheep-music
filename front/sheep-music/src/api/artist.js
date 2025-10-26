import request from '@/utils/request'

/**
 * 创建歌手
 */
export const createArtist = (data) => {
  return request({
    url: '/admin/artist',
    method: 'post',
    data
  })
}

/**
 * 更新歌手
 */
export const updateArtist = (id, data) => {
  return request({
    url: `/admin/artist/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除歌手
 */
export const deleteArtist = (id) => {
  return request({
    url: `/admin/artist/${id}`,
    method: 'delete'
  })
}

/**
 * 获取歌手列表（分页）
 */
export const getArtistList = (params) => {
  return request({
    url: '/admin/artist/list',
    method: 'get',
    params
  })
}

/**
 * 获取所有歌手（不分页）
 */
export const getAllArtists = () => {
  return request({
    url: '/admin/artist/all',
    method: 'get'
  })
}

/**
 * 获取歌手详情
 */
export const getArtist = (id) => {
  return request({
    url: `/admin/artist/${id}`,
    method: 'get'
  })
}

// ========== 用户端 API ==========

/**
 * 获取歌手列表（用户端）
 */
export const getArtists = (params) => {
  return request({
    url: '/music/artists',
    method: 'get',
    params
  })
}

/**
 * 获取歌手详情（用户端）
 */
export const getArtistDetail = (id) => {
  return request({
    url: `/music/artist/${id}`,
    method: 'get'
  })
}

