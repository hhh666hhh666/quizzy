import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchMe, login as loginApi, register as registerApi } from '@/api/auth'
import type { UserVO } from '@/types'

// 只持久化 token（key 与 PC 端完全相同）；user 刷新时靠 /auth/me 重拉，与 quizzy-web 一致。
export const useUserStore = defineStore('user', () => {
  const token = ref<string>((uni.getStorageSync('quizzy_token') as string) || '')
  const user = ref<UserVO | null>(null)

  async function login(username: string, password: string) {
    const result = await loginApi(username, password)
    token.value = result.token
    uni.setStorageSync('quizzy_token', result.token)
    user.value = result.user
  }

  async function register(username: string, password: string, nickname: string) {
    const result = await registerApi(username, password, nickname)
    token.value = result.token
    uni.setStorageSync('quizzy_token', result.token)
    user.value = result.user
  }

  async function loadUser() {
    if (!token.value) return
    user.value = await fetchMe()
  }

  function logout() {
    token.value = ''
    user.value = null
    uni.removeStorageSync('quizzy_token')
  }

  return { token, user, login, register, loadUser, logout }
})
