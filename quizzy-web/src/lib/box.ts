import { ref } from 'vue'

/**
 * 确认框 / 输入框（替代 ElMessageBox 的 confirm、prompt）。
 *
 * 用法是从调用点像函数一样发起、await 结果；界面由一个常驻的 MessageBoxHost 渲染（reka Dialog）：
 *
 * ```ts
 * if (!(await confirmBox({ title: '提示', message: '删除后不可恢复，确认删除？', confirmText: '删除', danger: true }))) return
 *
 * const name = await promptBox({ title: '新建收藏夹', message: '给新收藏夹起个名字', placeholder: '最多 20 个字', validator: (v) => (v.trim() ? true : '名字不能为空') })
 * if (name === null) return  // 取消
 * ```
 *
 * 与 ElMessageBox 的关键差异（迁移时特意选的）：**取消走返回值（false / null），不再靠 reject**——
 * 原来每个调用点都得包一层 `catch { return }` 把「取消」和「真失败」混在一起，现在一眼看清。
 */
export interface ConfirmBoxOptions {
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  /** 危险操作（删除 / 注销）：确认按钮用危险色 */
  danger?: boolean
}

export interface PromptBoxOptions extends ConfirmBoxOptions {
  defaultValue?: string
  placeholder?: string
  inputType?: 'text' | 'password'
  /** 返回 true 通过；返回字符串 = 报错文案（不关闭，显示在输入框下面） */
  validator?: (value: string) => string | true
}

export type BoxRequest =
  | { kind: 'confirm'; opts: ConfirmBoxOptions; resolve: (v: boolean) => void }
  | { kind: 'prompt'; opts: PromptBoxOptions; resolve: (v: string | null) => void }

export const boxRequest = ref<BoxRequest | null>(null)

export function confirmBox(opts: ConfirmBoxOptions): Promise<boolean> {
  return new Promise((resolve) => {
    boxRequest.value = { kind: 'confirm', opts, resolve }
  })
}

export function promptBox(opts: PromptBoxOptions): Promise<string | null> {
  return new Promise((resolve) => {
    boxRequest.value = { kind: 'prompt', opts, resolve }
  })
}
