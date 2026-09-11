import request from '@/utils/request'

/**
 * 婉婉小屋：展示页列表（按月分组 + 每日瞬间 + 我的星星数）
 */
export const getMemoryList = () => {
  return request({
    url: '/memory/list',
    method: 'get'
  })
}

/**
 * 婉婉小屋：点亮/取消星星
 */
export const toggleMemoryStar = (itemId) => {
  return request({
    url: `/memory/star/${itemId}`,
    method: 'post'
  })
}

/**
 * 婉婉小屋管理：全量素材列表（含下架）
 */
export const getMemoryAdminList = () => {
  return request({
    url: '/memory/admin/list',
    method: 'get'
  })
}

/**
 * 婉婉小屋管理：新建素材
 */
export const createMemoryItem = (data) => {
  return request({
    url: '/memory/admin',
    method: 'post',
    data
  })
}

/**
 * 婉婉小屋管理：编辑素材
 */
export const updateMemoryItem = (id, data) => {
  return request({
    url: `/memory/admin/${id}`,
    method: 'put',
    data
  })
}

/**
 * 婉婉小屋管理：发布/下架
 */
export const changeMemoryStatus = (id, status) => {
  return request({
    url: `/memory/admin/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 婉婉小屋管理：删除素材（DB + OSS）
 */
export const deleteMemoryItem = (id) => {
  return request({
    url: `/memory/admin/${id}`,
    method: 'delete'
  })
}

/**
 * 婉婉小屋管理：星星动态（近 14 天）
 */
export const getMemoryStarFeed = () => {
  return request({
    url: '/memory/admin/star-feed',
    method: 'get'
  })
}

/**
 * 婉婉小屋管理：上传素材文件到 OSS
 */
export const uploadMemoryFile = (file, onProgress) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/upload/memory',
    method: 'post',
    data: formData,
    timeout: 600000, // 200MB 视频给足 10 分钟
    onUploadProgress: onProgress
  })
}
