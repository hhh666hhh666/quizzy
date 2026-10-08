import { reactive } from 'vue'

/**
 * 站内轻提示（toast）。
 *
 * 自建的原因：Element Plus 退场后不引第二个 UI 库，而 ElMessage/ElNotification 的用法
 * 又只用到「右下角一条会自动消失的短条」这一种形态——八十行就能覆盖，还顺手把
 * 「带一个动作按钮的横幅」（收藏/撤销）也做进来了。
 *
 * ⚠️ e2e 以 `.toast--<type>` 类名做断言（helpers.lastMessage），改类名要同步改那边。
 */
export type ToastType = 'success' | 'info' | 'warning' | 'error'

export interface ToastAction {
  label: string
  /** 点击动作；`close` 用来手动收起本提示（撤销成功后收，失败了留着） */
  onClick: (close: () => void) => void
}

export interface ToastItem {
  id: number
  type: ToastType
  message: string
  action?: ToastAction
  duration: number
}

export const toasts = reactive<ToastItem[]>([])

let seq = 0

export function dismissToast(id: number) {
  const index = toasts.findIndex((item) => item.id === id)
  if (index >= 0) {
    toasts.splice(index, 1)
  }
}

function push(type: ToastType, message: string, opts: { duration?: number; action?: ToastAction } = {}) {
  const id = ++seq
  const duration = opts.duration ?? (opts.action ? 6000 : type === 'error' ? 5000 : 3000)
  toasts.push({ id, type, message, action: opts.action, duration })
  if (duration > 0) {
    window.setTimeout(() => dismissToast(id), duration)
  }
  return id
}

export const toast = {
  success: (message: string, opts?: { duration?: number; action?: ToastAction }) => push('success', message, opts),
  info: (message: string, opts?: { duration?: number; action?: ToastAction }) => push('info', message, opts),
  warning: (message: string, opts?: { duration?: number; action?: ToastAction }) => push('warning', message, opts),
  error: (message: string, opts?: { duration?: number; action?: ToastAction }) => push('error', message, opts)
}
