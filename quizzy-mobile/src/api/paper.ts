import request, { unwrap } from './request'
import type { PageResult, PaperVO } from '@/types'

/**
 * 试卷列表。
 *
 * ⚠️ 移动端只做「消费」：这里只读，**不提供**新建 / 编辑 / 删除（ADR 0019）。
 * 建设类操作留在 PC 端。
 */
export function pagePapers(page: number, size: number) {
  return unwrap<PageResult<PaperVO>>(request.get('/papers', { params: { page, size } }))
}

export function getPaper(id: number) {
  return unwrap<PaperVO>(request.get(`/papers/${id}`))
}
