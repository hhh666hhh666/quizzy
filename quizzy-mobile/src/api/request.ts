import type { Result } from '@/types'

// 分平台 base 地址。
// 本轮只跑 H5：与后端同源（dev 由 vite 代理 /api，将来上线走同域 /m/），所以用相对路径。
// ⚠️ 下一轮接微信小程序时必须改成绝对地址（如 https://<域名>/api），届时在此切换。
const BASE_URL = '/api'

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE'

function buildUrl(url: string, params?: Record<string, unknown>): string {
  if (!params) return BASE_URL + url
  const qs = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join('&')
  return BASE_URL + url + (qs ? `?${qs}` : '')
}

// 把 uni.request 套成 { data: Result<T> } 的形状，好让 unwrap 与 quizzy-web 逐字一致
function send<T>(
  method: Method,
  url: string,
  data?: unknown,
  params?: Record<string, unknown>
): Promise<{ data: Result<T> }> {
  return new Promise((resolve, reject) => {
    const token = (uni.getStorageSync('quizzy_token') as string) || ''
    uni.request({
      url: buildUrl(url, params),
      method,
      data: data as Record<string, unknown> | undefined,
      timeout: 30000,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => resolve({ data: res.data as Result<T> }),
      fail: () => {
        uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
        reject(new Error('network error'))
      }
    })
  })
}

const request = {
  get: <T>(url: string, params?: Record<string, unknown>) => send<T>('GET', url, undefined, params),
  post: <T>(url: string, data?: unknown, params?: Record<string, unknown>) =>
    send<T>('POST', url, data, params),
  put: <T>(url: string, data?: unknown) => send<T>('PUT', url, data),
  delete: <T>(url: string) => send<T>('DELETE', url)
}

/**
 * 后端是「HTTP 200 + 业务码」封装：成功码 0，401 也在 body 里。
 * 这段语义与 quizzy-web/src/api/request.ts 的 unwrap 保持逐字一致，便于两端对照维护。
 */
export async function unwrap<T>(promise: Promise<{ data: Result<T> }>): Promise<T> {
  const response = await promise
  const body = response.data
  if (body.code === 0) {
    return body.data
  }
  if (body.code === 401) {
    uni.removeStorageSync('quizzy_token')
    uni.reLaunch({ url: '/pages/login/index' })
  }
  const message = body.message || '请求失败'
  uni.showToast({ title: message, icon: 'none' })
  return Promise.reject(new Error(message))
}

export default request
