import request, { unwrap } from './request'
import type { PageResult, QuestionListItemVO } from '@/types'

export function pageWrongBook(page: number, size: number) {
  return unwrap<PageResult<QuestionListItemVO>>(request.get('/wrong-book', { params: { page, size } }))
}

/** 手动移出。正常路径是「连续答对 N 次自动移出」，这个是给「这题我早会了」用的。 */
export function removeFromWrongBook(questionId: number) {
  return unwrap<void>(request.delete(`/wrong-book/${questionId}`))
}

/** 用错题本开一次练习，返回会话 id。 */
export function practiceWrongBook(count: number) {
  return unwrap<number>(request.post('/wrong-book/practice', undefined, { count }))
}
