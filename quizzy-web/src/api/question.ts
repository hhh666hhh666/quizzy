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

/**
 * 把查询条件转成 URL 参数。列表与导出都走这一个函数，口径只有一个。
 *
 * ⚠️ 数组**必须**在这里手工摊成**重复的参数名**（`tagIds=1&tagIds=2`）。
 * 直接把数组交给 axios 会序列化成 `tagIds[]=1&tagIds[]=2`——参数名多了两个方括号，
 * 后端不认识，于是筛选题干**静默失效**：不报错、不提示，列表看起来就像「筛了跟没筛一样」。
 * 这类错误没有任何日志可查，所以宁可在这里多写三行，也不图省事。
 */
export function toQueryParams(query: QuestionQuery): URLSearchParams {
  const params = new URLSearchParams()
  if (query.keyword) params.set('keyword', query.keyword)
  if (query.type) params.set('type', query.type)
  if (query.difficulty) params.set('difficulty', query.difficulty)
  if (query.scope) params.set('scope', query.scope)
  query.categoryIds?.forEach((id) => params.append('categoryIds', String(id)))
  // 只在真的要筛「未分类」时才带这个参数：带 false 与不带，后端行为一致，那就别带
  if (query.uncategorized) params.set('uncategorized', 'true')
  query.tagIds?.forEach((id) => params.append('tagIds', String(id)))
  if (query.onlyWrong) params.set('onlyWrong', 'true')
  if (query.page) params.set('page', String(query.page))
  if (query.size) params.set('size', String(query.size))
  return params
}

export function pageQuestions(query: QuestionQuery) {
  return unwrap<PageResult<QuestionListItemVO>>(request.get('/questions', { params: toQueryParams(query) }))
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
