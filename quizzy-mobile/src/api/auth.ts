import request, { unwrap } from './request'
import type { LoginVO, UserVO } from '@/types'

export function login(username: string, password: string) {
  return unwrap<LoginVO>(request.post('/auth/login', { username, password }))
}

export function register(username: string, password: string, nickname: string) {
  return unwrap<LoginVO>(request.post('/auth/register', { username, password, nickname }))
}

export function fetchMe() {
  return unwrap<UserVO>(request.get('/auth/me'))
}
