<template>
  <el-card v-if="session" v-loading="loading">
    <template #header>
      <div class="header">
        <span>{{ session.title }}</span>
        <span>第 {{ current + 1 }} / {{ questions.length }} 题 · 已得 {{ session.obtainedScore }} 分</span>
        <el-button link type="danger" @click="onAbandon">放弃本次</el-button>
      </div>
      <el-progress :percentage="answeredPercent" :stroke-width="8" />
    </template>

    <div v-if="currentQuestion">
      <div class="stem">
        <el-tag size="small">{{ typeLabel(currentQuestion.type) }}</el-tag>
        <el-tag size="small" type="info">{{ currentQuestion.score }} 分</el-tag>
        <MarkdownRenderer :source="currentQuestion.stem" />
      </div>

      <el-alert
        v-if="reviewing"
        class="locked-tip"
        type="info"
        :closable="false"
        title="本题已作答，答案不可修改；想重做可以另开一次练习。"
      />

      <div class="options">
        <el-checkbox-group v-if="currentQuestion.type === 'MULTI'" v-model="picked" :disabled="submitted">
          <div v-for="option in currentQuestion.options" :key="option.label" class="option">
            <el-checkbox :value="option.label">
              <span class="label">{{ option.label }}.</span>
              <MarkdownRenderer :source="option.content" />
            </el-checkbox>
          </div>
        </el-checkbox-group>
        <el-radio-group v-else v-model="pickedRadio" :disabled="submitted">
          <div v-for="option in currentQuestion.options" :key="option.label" class="option">
            <el-radio :value="option.label">
              <span class="label">{{ option.label }}.</span>
              <MarkdownRenderer :source="option.content" />
            </el-radio>
          </div>
        </el-radio-group>
      </div>

      <div class="actions">
        <el-button :disabled="current === 0" @click="go(current - 1)">上一题</el-button>
        <el-button v-if="!submitted" type="primary" :loading="submitting" @click="onSubmit">提交本题</el-button>
        <el-button v-else @click="go(current + 1)">下一题</el-button>
        <el-button v-if="current === questions.length - 1" type="success" @click="onFinish">结束并查看结果</el-button>
      </div>

      <el-alert
        v-if="showVerdict"
        class="feedback"
        :type="verdictCorrect ? 'success' : 'error'"
        :title="verdictCorrect ? '回答正确' : '回答错误'"
        :closable="false"
      >
        <div>正确答案：{{ verdictAnswers.join(', ') }}</div>
        <div v-if="verdictAnalysis">
          <MarkdownRenderer :source="verdictAnalysis" />
        </div>
      </el-alert>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
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
    ElMessage.warning('请先选择答案')
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
  await ElMessageBox.confirm('放弃后本次答题不会再出现在未完成列表，确认放弃？', '提示', { type: 'warning' })
  await abandonSession(sessionId)
  router.push('/history')
}

function typeLabel(type: string) {
  return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[type] || type
}

onMounted(load)
</script>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between; }
.stem { font-size: 15px; margin: 12px 0; }
.stem :deep(.el-tag) { margin-right: 6px; }
.locked-tip { margin-bottom: 12px; }
.options { margin: 16px 0; }
.option { display: flex; align-items: flex-start; margin: 10px 0; }
.label { font-weight: 600; margin-right: 4px; }
.actions { display: flex; gap: 10px; }
.feedback { margin-top: 16px; }
</style>
