<template>
  <div class="font-sans text-base text-ink">
    <div v-if="session" class="rounded-xl bg-surface p-6 shadow-sm" :class="loading ? 'pointer-events-none opacity-60' : ''">
      <!-- 头部：标题 + 进度文字 + 放弃；e2e 钩子 .quiz-header（02 号 spec 断言「第 X / Y 题」） -->
      <div class="quiz-header flex flex-wrap items-center justify-between gap-3">
        <h1 class="text-base font-medium">{{ session.title }}</h1>
        <span class="text-sm text-ink-muted">第 {{ current + 1 }} / {{ questions.length }} 题 · 已得 {{ session.obtainedScore }} 分</span>
        <button type="button" class="text-sm text-danger hover:underline" @click="onAbandon">放弃本次</button>
      </div>

      <!-- 进度条：细条 + 品牌填充 -->
      <div class="quiz-progress mt-3 h-2 overflow-hidden rounded-full bg-surface-2">
        <div class="h-full rounded-full bg-brand transition-all" :style="{ width: answeredPercent + '%' }" />
      </div>

      <div v-if="currentQuestion" class="mt-5">
        <div class="flex items-center gap-2">
          <TypeTag :type="currentQuestion.type" />
          <span class="inline-flex items-center rounded-sm bg-surface-2 px-2 py-0.5 text-xs text-ink-muted">{{ currentQuestion.score }} 分</span>
        </div>

        <!-- 题干是「读」的内容：近白阅读面 -->
        <div class="reader mt-3 rounded-lg border border-line-soft bg-reader p-5">
          <MarkdownRenderer :source="currentQuestion.stem" />
        </div>

        <p
          v-if="reviewing"
          class="locked-tip mt-3 rounded-md bg-surface-2 px-3 py-2 text-sm text-ink-muted"
        >
          本题已作答，答案不可修改；想重做可以另开一次练习。
        </p>

        <!--
          选项：一行一张可点卡片。原生 radio / checkbox 藏在 label 里（label 就是点击目标，
          e2e 沿用 `.option → label` 的点法），选中态用 has-[:checked] 上底色——
          无 preflight 过渡期里这比 reka 控件更可控，键盘 / 读屏行为也由原生元素兜底。
        -->
        <div class="options mt-4 flex flex-col gap-3">
          <div v-for="option in currentQuestion.options" :key="option.label" class="option">
            <label
              class="flex cursor-pointer items-start gap-3 rounded-lg border p-4 transition-colors has-[:checked]:border-brand has-[:checked]:bg-tint-blue/40"
              :class="submitted ? 'cursor-default' : ''"
            >
              <input
                v-if="currentQuestion.type === 'MULTI'"
                v-model="picked"
                type="checkbox"
                :value="option.label"
                :disabled="submitted"
                class="mt-1 size-4 shrink-0 accent-brand"
              />
              <input
                v-else
                v-model="pickedRadio"
                type="radio"
                name="quiz-option"
                :value="option.label"
                :disabled="submitted"
                class="mt-1 size-4 shrink-0 accent-brand"
              />
              <span class="font-semibold">{{ option.label }}.</span>
              <span class="min-w-0 flex-1"><MarkdownRenderer :source="option.content" /></span>
            </label>
          </div>
        </div>

        <div class="actions mt-4 flex gap-2">
          <Button variant="outline" :disabled="current === 0" @click="go(current - 1)">上一题</Button>
          <Button v-if="!submitted" :disabled="submitting" @click="onSubmit">提交本题</Button>
          <Button v-else variant="outline" @click="go(current + 1)">下一题</Button>
          <Button v-if="current === questions.length - 1" class="bg-ok text-white hover:opacity-90" @click="onFinish">
            结束并查看结果
          </Button>
        </div>

        <!-- 判分反馈：绿底对 / 红底错（tint 底 + 深字色，状态色不做大色块） -->
        <div
          v-if="showVerdict"
          class="feedback mt-4 rounded-md p-4"
          role="alert"
          :class="verdictCorrect ? 'bg-tint-green/60' : 'bg-danger-soft/60'"
        >
          <p class="text-sm font-medium" :class="verdictCorrect ? 'text-ink-green' : 'text-ink-red'">
            {{ verdictCorrect ? '回答正确' : '回答错误' }}
          </p>
          <p class="mt-1 text-sm">正确答案：{{ verdictAnswers.join(', ') }}</p>
          <div v-if="verdictAnalysis" class="mt-1 text-sm">
            <MarkdownRenderer :source="verdictAnalysis" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { confirmBox } from '@/lib/box'
import { toast } from '@/lib/toast'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import TypeTag from '@/components/TypeTag.vue'
import { Button } from '@/components/ui/button'
import { abandonSession, finishSession, getSession, submitAnswer } from '@/api/quiz'
import type { AnswerResultVO, QuizQuestionVO, SessionVO } from '@/types'

const route = useRoute()
const router = useRouter()
const sessionId = Number(route.params.id)

const session = ref<SessionVO | null>(null)
const current = ref(0)
const picked = ref<string[]>([])
const pickedRadio = ref('')
const feedback = ref<AnswerResultVO | null>(null)
const loading = ref(false)
const submitting = ref(false)

const questions = computed<QuizQuestionVO[]>(() => session.value?.questions || [])
const currentQuestion = computed<QuizQuestionVO | null>(() => questions.value[current.value] || null)
const submitted = computed(() => currentQuestion.value?.answered || feedback.value !== null)
const answeredPercent = computed(() => {
  if (!questions.value.length) return 0
  return Math.round((questions.value.filter((q) => q.answered).length * 100) / questions.value.length)
})

// 「刚提交那一刻」的反馈来自接口的返回体；「回看一道已作答的题」时则要读题目本身——
// detail 接口在已作答时才会下发 correctAnswers / analysis（未作答时不下发，
// 见后端 QuizService#detail 与 QuizApiIT#unansweredQuestionsDoNotLeakAnswers）。
// 两个来源在这里合流，模板只认下面这几个名字，免得同一块反馈分两处渲染。
const reviewing = computed(() => currentQuestion.value?.answered === true && feedback.value === null)
const verdictAnswers = computed<string[]>(() =>
  feedback.value ? feedback.value.correctAnswers : currentQuestion.value?.correctAnswers || [])
const verdictAnalysis = computed(() =>
  (feedback.value ? feedback.value.analysis : currentQuestion.value?.analysis) || '')
const verdictCorrect = computed(() =>
  feedback.value ? feedback.value.isCorrect : currentQuestion.value?.isCorrect === true)
// 有答案就显示：未作答时两者皆空，于是自然不显示
const showVerdict = computed(() => verdictAnswers.value.length > 0 || verdictAnalysis.value !== '')

async function load() {
  loading.value = true
  try {
    session.value = await getSession(sessionId)
    const firstUnanswered = session.value.questions.findIndex((q) => !q.answered)
    current.value = firstUnanswered === -1 ? 0 : firstUnanswered
    syncPicked()
  } finally {
    loading.value = false
  }
}

function syncPicked() {
  const question = currentQuestion.value
  feedback.value = null
  if (!question) return
  if (question.type === 'MULTI') {
    picked.value = question.answered ? [...question.userAnswers] : []
  } else {
    pickedRadio.value = question.answered ? question.userAnswers[0] || '' : ''
  }
}

function go(index: number) {
  if (index < 0 || index >= questions.value.length) return
  current.value = index
  syncPicked()
}

async function onSubmit() {
  const question = currentQuestion.value
  if (!question) return
  const answer = question.type === 'MULTI' ? picked.value : pickedRadio.value ? [pickedRadio.value] : []
  if (!answer.length) {
    toast.warning('请先选择答案')
    return
  }
  submitting.value = true
  try {
    const result = await submitAnswer(sessionId, question.questionId, answer)
    // 刷新会话（拿 answered / userAnswers / obtainedScore），但**停在刚提交的这题**上。
    // 不能让 load() 决定当前题：它会把位置挪到「第一道未答题」，并在 syncPicked() 里把
    // feedback 清成 null —— 原先那句 `feedback.value = feedback.value` 是自赋值，等于
    // 判分反馈永远不显示；而且按钮变成「下一题」后一点就跳过一道未答题。
    const index = current.value
    await load()
    current.value = index
    syncPicked()
    feedback.value = result
  } finally {
    submitting.value = false
  }
}

async function onFinish() {
  await finishSession(sessionId)
  router.push(`/quiz/${sessionId}/result`)
}

async function onAbandon() {
  const ok = await confirmBox({ title: '提示', message: '放弃后本次答题不会再出现在未完成列表，确认放弃？', confirmText: '放弃', danger: true })
  if (!ok) return
  await abandonSession(sessionId)
  router.push('/history')
}

onMounted(load)
</script>
