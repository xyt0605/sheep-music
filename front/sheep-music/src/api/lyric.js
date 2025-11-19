import request from '@/utils/request'

/**
 * 获取歌曲歌词
 * @param {number} songId 歌曲ID
 * @returns {Promise}
 */
export function getSongLyric(songId) {
  return request({
    url: `/music/song/${songId}/lyric`,
    method: 'get'
  })
}

