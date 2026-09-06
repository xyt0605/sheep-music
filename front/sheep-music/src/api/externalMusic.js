import request from '@/utils/request'

/**
 * 开放曲库（外源）搜索——曲库供应链 v1（docs/specs/曲库供应链v1/）
 */
export const searchExternalSongs = (params) => {
  return request({
    url: '/music/external/search',
    method: 'get',
    params
  })
}
