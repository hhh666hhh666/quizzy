export type QuestionType = 'SINGLE' | 'MULTI' | 'JUDGE'
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type PaperMode = 'FIXED' | 'RULE'
export type SourceType = 'PAPER' | 'QUICK' | 'WRONG_BOOK' | 'FAVORITE'
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

/**
 * 收藏夹。⚠️「收藏」= 题目在至少一个夹里，**没有「未分组」这个状态**；
 * 默认收藏夹不可删、可改名，所以它的名字只能从接口拿，界面里不许写死。
 */
export interface FavoriteFolderVO {
  id: number
  name: string
  /** 简介，最多 200 字；没填时后端不返回这个字段 */
  intro?: string
  /** 默认收藏夹不可删除（但可改名），界面据此决定要不要显示「删除」 */
  isDefault: boolean
  /** ⚠️ 目前只回显：公开的夹别人看不到，界面文案不许暗示「公开后别人能看」（ADR 0032） */
  isPublic: boolean
  questionCount: number
  /** 该夹最近一次有新题进来的时间；空夹时后端不返回这个字段 */
  lastAddedTime?: string
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
  /** 收藏 = 在至少一个收藏夹里（ADR 0030） */
  favorited: boolean
  /**
   * 什么时候收藏的（最近一次进夹的时间）。
   * ⚠️ **只有收藏夹页面会拿到它**——题库列表没有「夹」这个概念，后端不返回这个字段。
   */
  favoritedAt?: string
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
  /** 按收藏夹筛（可多选）。⚠️ anyFavorite 为真时它被忽略——两者是包含关系，不是并列 */
  favoriteFolderIds?: number[]
  /** 只看收藏（在任意一个收藏夹里）。界面上的「全部收藏」就是它 */
  anyFavorite?: boolean
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
  /** 收藏 = 在至少一个收藏夹里（ADR 0030） */
  favorited: boolean
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
