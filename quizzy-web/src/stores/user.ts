import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchMe, login as loginApi, register as registerApi } from '@/api/auth'
import type { UserVO } from '@/types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('quizzy_token') || '')
  const user = ref<UserVO | null>(null)

  async function login(username: string, password: string) {
    const result = await loginApi(username, password)
    token.value = result.token
    localStorage.setItem('quizzy_token', result.token)
    user.value = result.user
  }

  async function register(username: string, password: string, nickname: string) {
    const result = await registerApi(username, password, nickname)
    token.value = result.token
    localStorage.setItem('quizzy_token', result.token)
    user.value = result.user
  }

  async function loadUser() {
    if (!token.value) return
    user.value = await fetchMe()
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('quizzy_token')
  }

  /** 保存资料后把服务端返回的最新用户信息换上，免得再发一次 /me。 */
  function setUser(next: UserVO) {
    user.value = next
  }

  /**
   * 改密码后，后端只给**当前设备**换发新 token（见 ADR 0027），
   * 本地凭据要跟着换，否则下一次请求就 401 了。
   */
  function applyToken(next: string) {
    token.value = next
    localStorage.setItem('quizzy_token', next)
  }

  return { token, user, login, register, loadUser, logout, setUser, applyToken }
})
