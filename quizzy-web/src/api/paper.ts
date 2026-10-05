import request, { unwrap } from './request'
import type { ImportResultVO, PageResult, PaperRuleDTO, PaperVO, QuestionListItemVO } from '@/types'

export function pagePapers(page: number, size: number) {
  return unwrap<PageResult<PaperVO>>(request.get('/papers', { params: { page, size } }))
}

export function getPaper(id: number) {
  return unwrap<PaperVO>(request.get(`/papers/${id}`))
}

export function savePaper(payload: any) {
  if (payload.id) {
    return unwrap<number>(request.put(`/papers/${payload.id}`, payload))
  }
  return unwrap<number>(request.post('/papers', payload))
}

export function deletePaper(id: number) {
  return unwrap<void>(request.delete(`/papers/${id}`))
}

export function previewRule(rule: PaperRuleDTO) {
  return unwrap<QuestionListItemVO[]>(request.post('/papers/preview', rule))
}

export function importExcel(file: File) {
  const form = new FormData()
  form.append('file', file)
  return unwrap<ImportResultVO>(request.post('/questions/import/excel', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }))
}

export function importJson(items: any[]) {
  return unwrap<ImportResultVO>(request.post('/questions/import/json', items))
}

/**
 * 导出与模板下载走 axios（带 Authorization 头），所以这里返回不含 /api 前缀的路径。
 *
 * ⚠️ 导出**跟随筛选条件**（跟列表页共用 `toQueryParams` 那一份参数），但**不跟随分页**：
 * 界面筛完再导出，拿到的是「筛出来的那一批全部」，不是当前这一页。所以调用方传进来的
 * 参数里通常会**不含** page / size。
 */
export function exportPath(format: string, params?: URLSearchParams) {
  const search = new URLSearchParams(params)
  search.set('format', format)
  return `/questions/export?${search.toString()}`
}

export function templatePath() {
  return '/questions/template/excel'
}
