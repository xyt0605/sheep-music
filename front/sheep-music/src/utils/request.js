import axios from 'axios'
import { ElMessage } from 'element-plus'

// 根据环境自动选择 baseURL
// 开发环境：/api（通过 vite 代理到本地后端）
// 生产环境：从环境变量读取（Vercel 会注入）
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

// 创建 axios 实例
const request = axios.create({
  baseURL: baseURL,
  timeout: 30000 // 增加超时时间到30秒（云端可能较慢）
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 在发送请求之前做些什么，例如添加 token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    // 对请求错误做些什么
    console.log(error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 对响应数据做点什么
    const res = response.data
    
    // 后端返回格式：{ code: 200, message: "成功", data: {...} }
    if (res.code === 200) {
      // 返回完整的响应对象（包含 code, message, data）
      return res
    } else {
      // 业务错误
      ElMessage.error(res.message || '操作失败')
      return Promise.reject(new Error(res.message || '操作失败'))
    }
  },
  error => {
    // 对响应错误做点什么（网络错误、超时等）
    const message = error.response?.data?.message || error.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
