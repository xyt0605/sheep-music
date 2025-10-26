import request from '@/utils/request'

/**
 * 创建歌曲
 */
export const createSong = (data) => {
  return request({
    url: '/admin/song',
    method: 'post',
    data
  })
}

/**
 * 更新歌曲
 */
export const updateSong = (id, data) => {
  return request({
    url: `/admin/song/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除歌曲
 */
export const deleteSong = (id) => {
  return request({
    url: `/admin/song/${id}`,
    method: 'delete'
  })
}

/**
 * 获取歌曲列表（分页）
 */
export const getSongList = (params) => {
  return request({
    url: '/admin/song/list',
    method: 'get',
    params
  })
}

/**
 * 获取歌曲详情
 */
export const getSong = (id) => {
  return request({
    url: `/admin/song/${id}`,
    method: 'get'
  })
}

/**
 * 批量更新歌曲类型和语言
 */
export const batchUpdateGenreAndLanguage = (data) => {
  return request({
    url: '/admin/song/batch-update-genre-language',
    method: 'put',
    data
  })
}

/**
 * 上传封面
 */
export const uploadCover = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/upload/cover',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 上传音乐文件
 */
export const uploadMusic = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/upload/music',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// ========== 用户端 API ==========

/**
 * 获取热门歌曲
 */
export const getHotSongs = (params) => {
  return request({
    url: '/music/hot',
    method: 'get',
    params
  })
}

/**
 * 获取最新歌曲
 */
export const getNewSongs = (params) => {
  return request({
    url: '/music/new',
    method: 'get',
    params
  })
}

/**
 * 搜索歌曲
 */
export const searchSongs = (params) => {
  return request({
    url: '/music/search',
    method: 'get',
    params
  })
}

/**
 * 获取歌曲详情（用户端）
 */
export const getMusicDetail = (id) => {
  return request({
    url: `/music/song/${id}`,
    method: 'get'
  })
}

/**
 * 播放歌曲（增加播放次数）
 */
export const playSong = (id) => {
  return request({
    url: `/music/play/${id}`,
    method: 'post'
  })
}

/**
 * 根据歌手获取歌曲
 */
export const getSongsByArtist = (artistId) => {
  return request({
    url: `/music/artist/${artistId}/songs`,
    method: 'get'
  })
}

