export type QuestionType = 'SINGLE' | 'MULTI' | 'JUDGE'
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type PaperMode = 'FIXED' | 'RULE'
export type SourceType = 'PAPER' | 'QUICK' | 'WRONG_BOOK'
export type SessionStatus = 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED'

export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface UserVO {
  id: number
  username: string
  nickname: string
}

export interface LoginVO {
  token: string
  expireMillis: number
  user: UserVO
}

export interface TagVO {
  id: number
  name: string
}

export interface OptionVO {
  label: string
  content: string
}

export interface QuestionListItemVO {
  id: number
  type: QuestionType
  stem: string
  difficulty: Difficulty
  score: number
  categoryName: string
  ownerId: number | null
  editable: boolean
  inWrongBook: boolean
  tags: TagVO[]
}

export interface QuestionVO extends QuestionListItemVO {
  analysis: string
  answers: string[]
  categoryId: number | null
  answerCount: number
  correctCount: number
  options: OptionVO[]
}

export interface QuestionSaveDTO {
  id?: number | null
  type: QuestionType
  stem: string
  analysis: string
  difficulty: Difficulty
  answers: string[]
  score: number
  categoryId: number | null
  options: OptionVO[]
  tags: string[]
}

export interface QuestionQuery {
  page?: number
  size?: number
  keyword?: string
  type?: QuestionType
  difficulty?: Difficulty
  categoryId?: number
  tagIds?: number[]
  scope?: string
  onlyWrong?: boolean
}

export interface PaperRuleDTO {
  categoryId: number | null
  tagIds: number[]
  types: QuestionType[]
  difficulties: Difficulty[]
  count: number
  excludeRecentDays: number | null
}

export interface PaperVO {
  id: number
  title: string
  description: string
  mode: PaperMode
  rule: PaperRuleDTO | null
  questionCount: number
  questionIds: number[]
}

export interface QuizQuestionVO {
  questionId: number
  index: number
  type: QuestionType
  stem: string
  score: number
  options: OptionVO[]
  answered: boolean
  userAnswers: string[]
  correctAnswers: string[]
  analysis: string
  isCorrect: boolean
}

export interface SessionVO {
  id: number
  title: string
  sourceType: SourceType
  status: SessionStatus
  paperId: number | null
  questionCount: number
  currentIndex: number
  totalScore: number
  obtainedScore: number
  startTime: string
  finishTime: string | null
  questions: QuizQuestionVO[]
}

export interface AnswerResultVO {
  questionId: number
  isCorrect: boolean
  correctAnswers: string[]
  analysis: string
  score: number
  obtainedScore: number
  answeredCount: number
  questionCount: number
}

export interface QuizResultItemVO {
  questionId: number
  index: number
  type: QuestionType
  stem: string
  score: number
  options: OptionVO[]
  userAnswers: string[]
  correctAnswers: string[]
  answered: boolean
  correct: boolean
  analysis: string
}

export interface SessionResultVO {
  id: number
  title: string
  sourceType: SourceType
  status: SessionStatus
  questionCount: number
  answeredCount: number
  correctCount: number
  unansweredCount: number
  totalScore: number
  obtainedScore: number
  accuracy: number
  startTime: string
  finishTime: string | null
  items: QuizResultItemVO[]
}

export interface ImportErrorVO {
  row: number
  stem: string
  reason: string
}

export interface ImportResultVO {
  total: number
  successCount: number
  failed: ImportErrorVO[]
}
