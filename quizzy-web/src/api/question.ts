import request, { unwrap } from './request'
import type { PageResult, QuestionListItemVO, QuestionQuery, QuestionVO } from '@/types'

export function listCategories() {
  return unwrap<any[]>(request.get('/categories'))
}

export function createCategory(name: string, sort = 0) {
  return unwrap<any>(request.post('/categories', { name, sort }))
}

export function deleteCategory(id: number) {
  return unwrap<void>(request.delete(`/categories/${id}`))
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
