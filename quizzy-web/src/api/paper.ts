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

/**
 * 批量导入。
 *
 * ⚠️ `paperTitle` 这类元信息走**查询参数**，不进 body——JSON 那条的 body 必须是**裸数组**
 * （见 docs/adr/0006-bare-array-import-contract.md，其 Amendment 1 记了为什么不用包装对象）。
 * 给了卷名，后端会把**本次成功导入的题**装进一张新的固定卷，并在结果里回 `paperId`。
 */
export function importExcel(file: File, paperTitle?: string) {
  const form = new FormData()
  form.append('file', file)
  return unwrap<ImportResultVO>(request.post('/questions/import/excel', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    params: paperTitle ? { paperTitle } : undefined
  }))
}

export function importJson(items: any[], paperTitle?: string) {
  return unwrap<ImportResultVO>(request.post('/questions/import/json', items, {
    params: paperTitle ? { paperTitle } : undefined
  }))
}

/**
 * 往一张**固定卷**追加题目。
 *
 * 语义是**并入**不是替换：已在卷里的题会被忽略（后端靠 `paper_question` 的唯一键去重），
 * 所以重复调用不会越加越多。返回本次真正新增的题量与追加后的总题量。
 */
export function appendPaperQuestions(paperId: number, questionIds: number[]) {
  return unwrap<{ added: number; total: number }>(
    request.post(`/papers/${paperId}/questions`, { questionIds })
  )
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
