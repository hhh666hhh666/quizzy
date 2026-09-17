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
 */
export function exportPath(format: string, ids?: number[]) {
  const params = new URLSearchParams()
  params.set('format', format)
  if (ids && ids.length) {
    params.set('ids', ids.join(','))
  }
  return `/questions/export?${params.toString()}`
}

export function templatePath() {
  return '/questions/template/excel'
}
