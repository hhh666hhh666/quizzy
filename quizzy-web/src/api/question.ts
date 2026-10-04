import request, { unwrap } from './request'
import type { PageResult, QuestionListItemVO, QuestionQuery, QuestionVO } from '@/types'

export function listCategories() {
  return unwrap<any[]>(request.get('/categories'))
}

/**
 * 改分类名（**只迁移自己的题目**）。
 *
 * ⚠️ 这里**没有** create / delete：
 * - 分类随「保存题目」自动建（传 `categoryName`，同一个事务里完成）；
 * - 无人引用时由系统自动删。
 * 分类是全体共用的，把「主动删除共享分类」这个口子留给任何注册用户是不可接受的。
 */
export function moveCategory(id: number, name: string) {
  return unwrap<any>(request.put(`/categories/${id}`, { name }))
}

export function listTags() {
  return unwrap<any[]>(request.get('/tags'))
}

export function pageQuestions(query: QuestionQuery) {
  return unwrap<PageResult<QuestionListItemVO>>(request.get('/questions', { params: query }))
}

export function getQuestion(id: number) {
  return unwrap<QuestionVO>(request.get(`/questions/${id}`))
}

export function saveQuestion(payload: any) {
  const id = payload.id
  if (id) {
    return unwrap<number>(request.put(`/questions/${id}`, payload))
  }
  return unwrap<number>(request.post('/questions', payload))
}

export function deleteQuestion(id: number) {
  return unwrap<void>(request.delete(`/questions/${id}`))
}
