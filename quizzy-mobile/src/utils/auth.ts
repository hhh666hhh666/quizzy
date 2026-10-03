import { useUserStore } from '@/stores/user'

/**
 * 登录态守卫：受保护页面在 onShow 里调用。
 * 无 token 直接 reLaunch 回登录页，返回 false。
 */
export function ensureLogin(): boolean {
  const store = useUserStore()
  if (!store.token) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

export function goHome() {
  uni.reLaunch({ url: '/pages/home/index' })
}
