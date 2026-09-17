import request, { unwrap } from './request'
import type { AnswerResultVO, PageResult, PaperRuleDTO, SessionResultVO, SessionVO } from '@/types'

export function startQuiz(payload: { sourceType: string; paperId?: number; rule?: PaperRuleDTO; count?: number }) {
  return unwrap<number>(request.post('/quiz/start', payload))
}

export function getSession(id: number) {
  return unwrap<SessionVO>(request.get(`/quiz/sessions/${id}`))
}

export function submitAnswer(sessionId: number, questionId: number, answer: string[]) {
  return unwrap<AnswerResultVO>(request.post(`/quiz/sessions/${sessionId}/answer`, {
    questionId,
    answer: answer.join(',')
  }))
}

export function finishSession(sessionId: number) {
  return unwrap<SessionResultVO>(request.post(`/quiz/sessions/${sessionId}/finish`))
}

export function abandonSession(sessionId: number) {
  return unwrap<void>(request.post(`/quiz/sessions/${sessionId}/abandon`))
}

export function listSessions(page: number, size: number, status?: string) {
  return unwrap<PageResult<SessionVO>>(request.get('/quiz/sessions', { params: { page, size, status } }))
}

export function getSessionResult(sessionId: number) {
  return unwrap<SessionResultVO>(request.get(`/quiz/sessions/${sessionId}/result`))
}

export function pageWrongBook(page: number, size: number) {
  return unwrap<PageResult<any>>(request.get('/wrong-book', { params: { page, size } }))
}

export function removeFromWrongBook(questionId: number) {
  return unwrap<void>(request.delete(`/wrong-book/${questionId}`))
}

export function practiceWrongBook(count: number) {
  return unwrap<number>(request.post('/wrong-book/practice', null, { params: { count } }))
}
