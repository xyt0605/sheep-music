import request from '@/utils/request'

/**
 * 开放曲库（外源）——曲库供应链 v1（docs/specs/曲库供应链v1/）
 */

// 音源列表（含启用状态，前端据此生成搜索分区）
export const getExternalSources = () => {
  return request({
    url: '/music/external/sources',
    method: 'get'
  })
}

// 搜索外源开放曲库歌曲
export const searchExternalSongs = (params) => {
  return request({
    url: '/music/external/search',
    method: 'get',
    params
  })
}

// 外源歌词（LRC 文本，无歌词返回空串）
export const getExternalLyric = (params) => {
  return request({
    url: '/music/external/lyric',
    method: 'get',
    params
  })
}

// 外源歌曲封面 URL（无则返回空串）
export const getExternalCover = (params) => {
  return request({
    url: '/music/external/cover',
    method: 'get',
    params
  })
}
