import axios from 'axios'
import type { Result } from '@/types'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('quizzy_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
      return Promise.reject(error)
    }
    return Promise.reject(error)
  }
)

export async function unwrap<T>(promise: Promise<{ data: Result<T> }>): Promise<T> {
  const response = await promise
  const body = response.data
  if (body.code === 0) {
    return body.data
  }
  if (body.code === 401) {
    localStorage.removeItem('quizzy_token')
    router.push('/login')
  }
  ElMessage.error(body.message || '请求失败')
  return Promise.reject(new Error(body.message || '请求失败'))
}

export default request
