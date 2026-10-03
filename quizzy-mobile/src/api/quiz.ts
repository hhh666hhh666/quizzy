import request, { unwrap } from './request'
import type { AnswerResultVO, PaperRuleDTO, SessionResultVO, SessionVO } from '@/types'

export function startQuiz(payload: {
  sourceType: string
  paperId?: number
  rule?: PaperRuleDTO
  count?: number
}) {
  return unwrap<number>(request.post('/quiz/start', payload))
}

export function getSession(id: number) {
  return unwrap<SessionVO>(request.get(`/quiz/sessions/${id}`))
}

export function submitAnswer(sessionId: number, questionId: number, answer: string[]) {
  return unwrap<AnswerResultVO>(
    request.post(`/quiz/sessions/${sessionId}/answer`, { questionId, answer: answer.join(',') })
  )
}

export function finishSession(id: number) {
  return unwrap<SessionResultVO>(request.post(`/quiz/sessions/${id}/finish`))
}

export function abandonSession(id: number) {
  return unwrap<void>(request.post(`/quiz/sessions/${id}/abandon`))
}

export function getSessionResult(id: number) {
  return unwrap<SessionResultVO>(request.get(`/quiz/sessions/${id}/result`))
}
