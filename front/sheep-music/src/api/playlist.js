import request from '@/utils/request'

/**
 * 歌单API封装
 */

// ==================== 我的歌单管理 ====================

/**
 * 创建歌单
 */
export function createPlaylist(data) {
  return request({
    url: '/api/playlist/create',
    method: 'post',
    data
  })
}

/**
 * 更新歌单信息
 */
export function updatePlaylist(id, data) {
  return request({
    url: `/api/playlist/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除歌单
 */
export function deletePlaylist(id) {
  return request({
    url: `/api/playlist/${id}`,
    method: 'delete'
  })
}

/**
 * 获取我的歌单列表（分页）
 */
export function getMyPlaylists(params) {
  return request({
    url: '/api/playlist/my',
    method: 'get',
    params
  })
}

/**
 * 获取我的所有歌单（不分页）
 */
export function getMyAllPlaylists() {
  return request({
    url: '/api/playlist/my/all',
    method: 'get'
  })
}

/**
 * 统计我的歌单数量
 */
export function countMyPlaylists() {
  return request({
    url: '/api/playlist/my/count',
    method: 'get'
  })
}

// ==================== 歌单内容管理 ====================

/**
 * 添加歌曲到歌单
 */
export function addSongToPlaylist(playlistId, songId) {
  return request({
    url: `/api/playlist/${playlistId}/songs/${songId}`,
    method: 'post'
  })
}

/**
 * 批量添加歌曲到歌单
 */
export function addSongsToPlaylist(playlistId, songIds) {
  return request({
    url: `/api/playlist/${playlistId}/songs/batch`,
    method: 'post',
    data: songIds
  })
}

/**
 * 从歌单移除歌曲
 */
export function removeSongFromPlaylist(playlistId, songId) {
  return request({
    url: `/api/playlist/${playlistId}/songs/${songId}`,
    method: 'delete'
  })
}

/**
 * 获取歌单的歌曲列表
 */
export function getPlaylistSongs(playlistId, params) {
  return request({
    url: `/api/playlist/${playlistId}/songs`,
    method: 'get',
    params
  })
}

// ==================== 歌单详情 ====================

/**
 * 获取歌单详情
 */
export function getPlaylistDetail(id) {
  return request({
    url: `/api/playlist/${id}`,
    method: 'get'
  })
}

/**
 * 增加播放次数
 */
export function incrementPlayCount(id) {
  return request({
    url: `/api/playlist/${id}/play`,
    method: 'post'
  })
}

/**
 * 设置歌单公开/私有
 */
export function setPlaylistVisibility(id, isPublic) {
  return request({
    url: `/api/playlist/${id}/visibility`,
    method: 'post',
    params: { isPublic }
  })
}

// ==================== 歌单广场 ====================

/**
 * 获取公开歌单列表
 */
export function getPublicPlaylists(params) {
  return request({
    url: '/api/playlist/square',
    method: 'get',
    params
  })
}

/**
 * 获取热门公开歌单
 */
export function getHotPlaylists(params) {
  return request({
    url: '/api/playlist/square/hot',
    method: 'get',
    params
  })
}

/**
 * 搜索公开歌单
 */
export function searchPublicPlaylists(params) {
  return request({
    url: '/api/playlist/square/search',
    method: 'get',
    params
  })
}

