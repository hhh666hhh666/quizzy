import axios from 'axios'
import type { Result } from '@/types'
import { toast } from '@/lib/toast'
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
      toast.error('请求超时，请稍后重试')
      return Promise.reject(error)
    }
    return Promise.reject(error)
  }
)

export async function unwrap<T>(promise: Promise<{ data: Result<T> }>, opts?: { silent?: boolean }): Promise<T> {
  const response = await promise
  const body = response.data
  if (body.code === 0) {
    return body.data
  }
  if (body.code === 401) {
    localStorage.removeItem('quizzy_token')
    router.push('/login')
  }
  // ⚠️ `silent`：错误由调用方自己呈现（如登录页的内联提示条），不弹全局提示。
  if (!opts?.silent) {
    toast.error(body.message || '请求失败')
  }
  return Promise.reject(new Error(body.message || '请求失败'))
}

export default request
