import request from '@/utils/request'

/**
 * 分享歌单
 */
export const sharePlaylist = (data) => {
  return request({
    url: '/share/playlist',
    method: 'post',
    data
  })
}

/**
 * 获取分享广场
 */
export const getShareSquare = (params) => {
  return request({
    url: '/share/square',
    method: 'get',
    params
  })
}

/**
 * 获取热门分享
 */
export const getHotShares = (params) => {
  return request({
    url: '/share/hot',
    method: 'get',
    params
  })
}

/**
 * 获取好友分享
 */
export const getFriendsShares = () => {
  return request({
    url: '/share/friends',
    method: 'get'
  })
}

/**
 * 获取用户的分享
 */
export const getUserShares = (userId) => {
  return request({
    url: `/share/user/${userId}`,
    method: 'get'
  })
}

/**
 * 增加浏览次数
 */
export const incrementShareView = (shareId) => {
  return request({
    url: `/share/${shareId}/view`,
    method: 'post'
  })
}

/**
 * 收藏分享的歌单
 */
export const collectSharedPlaylist = (shareId) => {
  return request({
    url: `/share/${shareId}/collect`,
    method: 'post'
  })
}

/**
 * 删除分享
 */
export const deleteShare = (shareId) => {
  return request({
    url: `/share/${shareId}`,
    method: 'delete'
  })
}

// ==================== 歌曲分享相关接口 ====================

/**
 * 分享歌曲
 */
export const shareSong = (data) => {
  return request({
    url: '/share/song',
    method: 'post',
    data
  })
}

/**
 * 获取歌曲分享广场
 */
export const getSongShareSquare = (params) => {
  return request({
    url: '/share/songs',
    method: 'get',
    params
  })
}

/**
 * 获取热门歌曲分享
 */
export const getHotSongShares = (params) => {
  return request({
    url: '/share/songs/hot',
    method: 'get',
    params
  })
}

/**
 * 获取好友的歌曲分享
 */
export const getFriendsSongShares = () => {
  return request({
    url: '/share/songs/friends',
    method: 'get'
  })
}

/**
 * 获取用户的歌曲分享
 */
export const getUserSongShares = (userId) => {
  return request({
    url: `/share/songs/user/${userId}`,
    method: 'get'
  })
}

/**
 * 增加歌曲分享浏览次数
 */
export const incrementSongShareView = (shareId) => {
  return request({
    url: `/share/song/${shareId}/view`,
    method: 'post'
  })
}

/**
 * 点赞歌曲分享
 */
export const likeSongShare = (shareId) => {
  return request({
    url: `/share/song/${shareId}/like`,
    method: 'post'
  })
}

/**
 * 删除歌曲分享
 */
export const deleteSongShare = (shareId) => {
  return request({
    url: `/share/song/${shareId}`,
    method: 'delete'
  })
}

