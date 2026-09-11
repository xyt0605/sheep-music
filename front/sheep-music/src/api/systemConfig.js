import request from '@/utils/request'

/**
 * 系统设置：OSS 配置状态（密钥脱敏）
 */
export const getOssConfig = () => {
  return request({
    url: '/system/config/oss',
    method: 'get'
  })
}

/**
 * 系统设置：保存 OSS 配置（Secret 留空沿用旧值，运行时生效）
 */
export const saveOssConfig = (data) => {
  return request({
    url: '/system/config/oss',
    method: 'put',
    data
  })
}

/**
 * 系统设置：测试 OSS 连接（可传覆盖值，不落库）
 */
export const testOssConfig = (data) => {
  return request({
    url: '/system/config/oss/test',
    method: 'post',
    data,
    timeout: 30000
  })
}

/**
 * 系统设置：恢复默认（删除面板配置，回落 application.yml）
 */
export const resetOssConfig = () => {
  return request({
    url: '/system/config/oss',
    method: 'delete'
  })
}
