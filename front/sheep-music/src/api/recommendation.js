import request from '@/utils/request'

/**
 * 获取个性化推荐歌曲（基于用户协同过滤）
 * @param {Object} params - { limit: 推荐数量 }
 */
export const getPersonalizedSongs = (params) => {
  return request({
    url: '/music/recommend/personalized',
    method: 'get',
    params
  })
}

/**
 * 猜你喜欢（混合推荐策略）
 * @param {Object} params - { limit: 推荐数量 }
 */
export const getGuessYouLike = (params) => {
  return request({
    url: '/music/recommend/guess-you-like',
    method: 'get',
    params
  })
}

/**
 * 获取相似歌曲（基于物品协同过滤）
 * @param {Number} songId - 歌曲ID
 * @param {Object} params - { limit: 推荐数量 }
 */
export const getSimilarSongs = (songId, params) => {
  return request({
    url: `/music/recommend/similar-songs/${songId}`,
    method: 'get',
    params
  })
}

/**
 * 推荐歌单
 * @param {Object} params - { limit: 推荐数量 }
 */
export const getRecommendedPlaylists = (params) => {
  return request({
    url: '/music/recommend/playlists',
    method: 'get',
    params
  })
}

