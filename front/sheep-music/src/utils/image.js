// OSS 图片工具：列表/卡片场景给阿里云 OSS 图片追加缩放参数，
// 避免 500KB 原图在缩略图位置反复下载（手机端流量与加载时长都明显受益）。
// 注意：仅对自家 OSS 生效；外站图片（百度头像、QQ 音乐封面等）原样返回。

const OSS_HOSTS = [
  'sheepmusic.oss-cn-hangzhou.aliyuncs.com',
  '/api/oss/', // 开发/生产代理形式（vite 与 Nginx 都把 /api/oss/* 透传到 OSS）
  '/oss/'
]

/**
 * 给 OSS 图片 URL 追加缩放处理参数。
 * @param {string} url 原图 URL
 * @param {number} width 目标宽度（px），等比缩放，只缩不放（m_lfit）
 * @returns {string} 处理后的 URL
 */
export function ossThumb (url, width) {
  if (!url || !width) return url || ''
  if (!OSS_HOSTS.some((host) => url.includes(host))) return url
  if (url.includes('x-oss-process')) return url // 已带处理参数的不重复追加
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}x-oss-process=image/resize,w_${width},m_lfit/quality,q_85`
}
