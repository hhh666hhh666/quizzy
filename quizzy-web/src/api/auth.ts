import request, { unwrap } from './request'
import type { LoginVO, UserVO } from '@/types'

/** 登录。`silent`：错误由登录页内联呈现，不弹全局 toast（见 request.ts 的 unwrap）。 */
export function login(username: string, password: string) {
  return unwrap<LoginVO>(request.post('/auth/login', { username, password }), { silent: true })
}

/** 注册。同上：错误由登录页内联呈现。 */
export function register(username: string, password: string, nickname: string) {
  return unwrap<LoginVO>(request.post('/auth/register', { username, password, nickname }), { silent: true })
}

export function fetchMe() {
  return unwrap<UserVO>(request.get('/auth/me'))
}

/** 改昵称 / 改头像。`avatar` 传 null 或空串表示**改回默认的生成头像**，不是「不改」。 */
export function updateProfile(nickname: string, avatar: string | null) {
  return unwrap<UserVO>(request.put('/auth/me', { nickname, avatar }))
}

/** 改密码。返回的新 token 只给**当前设备**用，其他设备的旧 token 立即失效（见 ADR 0027）。 */
export function changePassword(oldPassword: string, newPassword: string) {
  return unwrap<LoginVO>(request.put('/auth/password', { oldPassword, newPassword }))
}

/** 退出所有设备——含本机，调用方拿到成功之后要自己清 token 回登录页。 */
export function logoutAllDevices() {
  return unwrap<void>(request.post('/auth/logout-all'))
}

/** 注销账号（不可逆），要当前密码做二次确认。 */
export function deleteAccount(password: string) {
  return unwrap<void>(request.post('/auth/delete-account', { password }))
}
