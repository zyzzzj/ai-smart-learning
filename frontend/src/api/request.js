import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

/**
 * 统一请求封装:
 * - 自动携带 JWT(Authorization: Bearer xxx)
 * - 统一解包后端 Result{code,message,data};code!==200 时提示并抛出
 * - 401 自动清理登录态并跳转登录页
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 120000 // AI 出题/评分可能较慢
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('ai_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 文件流等非 Result 结构直接返回
    if (body === null || body === undefined) {
      return body
    }
    if (typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        return body.data
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      localStorage.removeItem('ai_token')
      localStorage.removeItem('ai_user')
      ElMessage.error('登录已过期,请重新登录')
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
    } else if (status === 403) {
      ElMessage.error('没有权限或链接已失效')
    } else if (status === 404) {
      ElMessage.error('资源不存在')
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
