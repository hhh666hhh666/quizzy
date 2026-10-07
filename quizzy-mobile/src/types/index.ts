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
  /**
   * 头像 data URL。**没设头像时整个字段不出现**——后端配了 `non_null`，
   * 所以判断「有没有自定义头像」要用 `!user.avatar`，别指望它是 `null`。
   */
  avatar?: string
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
  /** 分类多选；与 uncategorized 是「或」的关系。空数组＝不按分类筛（即「全部分类」） */
  categoryIds?: number[]
  /** 是否包含「未分类」的题目 */
  uncategorized?: boolean
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
  /**
   * 导入时按需创建的固定卷 id；**没建卷时整个字段不出现**（后端配了 `non_null`，
   * 所以判断「有没有建卷」要用 `paperId != null`，别指望它是 `null`）。
   */
  paperId?: number
}
