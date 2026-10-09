<template>
  <view class="page">
    <template v-if="session">
      <view class="head">
        <view class="head-main">
          <text class="counter">第 {{ current + 1 }} / {{ questions.length }} 题</text>
          <text class="head-score">已得 {{ session.obtainedScore }} 分</text>
        </view>
        <view class="head-right">
          <text class="percent">{{ percent }}%</text>
          <text class="abandon" @click="onAbandon">放弃本次</text>
        </view>
      </view>

      <view class="progress">
        <view class="progress-fill" :style="{ width: percent + '%' }" />
      </view>

      <template v-if="currentQuestion">
        <view class="qz-reader stem-card">
          <view class="tags">
            <text class="qz-tag qz-tag--brand">{{ typeLabel(currentQuestion.type) }}</text>
            <text class="qz-tag">{{ currentQuestion.score }} 分</text>
          </view>
          <view class="stem">
            <MarkdownRenderer :source="currentQuestion.stem" />
          </view>
        </view>

        <view class="options">
          <view class="locked-tip" v-if="reviewing">
            本题已作答，答案不可修改；想重做可以另开一次练习。
          </view>

          <!-- 选项自己排：整行可点、内容多行自适应。
               组件库的 radio / checkbox 会把块级内容排成居中文本（选项一多就参差不齐），
               而且点击落在行内元素上不一定触发选中。 -->
          <view
            v-for="option in currentQuestion.options"
            :key="option.label"
            :class="['option', optionState(option.label)]"
            @click="onPick(option.label)"
          >
            <view :class="['marker', currentQuestion.type === 'MULTI' ? 'marker--square' : '']">
              <text v-if="isPicked(option.label)" class="marker-tick">✓</text>
            </view>
            <view class="option-body">
              <text class="opt-label">{{ option.label }}.</text>
              <view class="option-content">
                <MarkdownRenderer :source="option.content" />
              </view>
            </view>
          </view>
        </view>

        <view class="feedback" v-if="showVerdict">
          <view :class="['fb-title', verdictCorrect ? 'ok' : 'bad']">
            {{ verdictCorrect ? '回答正确' : '回答错误' }}
          </view>
          <view class="fb-answer">正确答案：{{ verdictAnswers.join(', ') }}</view>
        </view>

        <view class="qz-reader analysis" v-if="verdictAnalysis">
          <view class="analysis-title">解析</view>
          <MarkdownRenderer :source="verdictAnalysis" />
        </view>
      </template>
    </template>

    <view v-else class="qz-loading">加载中…</view>

    <!-- 底部操作条：一屏只留一个主按钮，「放弃」已降级为顶部文字链 -->
    <view class="actionbar" v-if="session">
      <view class="action-inner">
        <view
          :class="['qz-btn', 'qz-btn--ghost', 'btn-prev', current === 0 ? 'qz-btn--disabled' : '']"
          @click="go(current - 1)"
        >
          上一题
        </view>
        <view v-if="!submitted" class="qz-btn qz-btn--primary btn-main" @click="onSubmit">
          {{ submitting ? '提交中…' : '提交本题' }}
        </view>
        <view v-else class="qz-btn qz-btn--primary btn-main" @click="onNext">
          {{ current === questions.length - 1 ? '结束并查看结果' : '下一题' }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { abandonSession, finishSession, getSession, submitAnswer } from '@/api/quiz'
import { ensureLogin } from '@/utils/auth'
import type { AnswerResultVO, QuizQuestionVO, SessionVO } from '@/types'

const sessionId = ref(0)
const session = ref<SessionVO | null>(null)
const current = ref(0)
const picked = ref<string[]>([])
const pickedRadio = ref('')
const feedback = ref<AnswerResultVO | null>(null)
const submitting = ref(false)

const questions = computed<QuizQuestionVO[]>(() => session.value?.questions || [])
const currentQuestion = computed<QuizQuestionVO | null>(() => questions.value[current.value] || null)
const submitted = computed(() => Boolean(currentQuestion.value?.answered) || feedback.value !== null)
const percent = computed(() => {
  if (!questions.value.length) return 0
  return Math.round(((current.value + 1) * 100) / questions.value.length)
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

/**
 * 选项的三种结果态（主人 2026-10-09 定）：
 *   picked  未作答时的选中——主色
 *   correct 已作答，且该选项是正确答案——绿
 *   wrong   已作答，且该选项是用户选错的——红
 * ⚠️ 颜色从不单独表意：绿 / 红同时配有「回答正确 / 回答错误」标题与「正确答案」一行，
 *    色盲与高对比模式下照样读得出来。
 */
function optionState(label: string): string {
  if (!submitted.value) return isPicked(label) ? 'picked' : ''
  const isRightAnswer = verdictAnswers.value.includes(label)
  if (isPicked(label)) return isRightAnswer ? 'correct' : 'wrong'
  // 用户没选、但确实是正确答案——一并染绿，指明「该选的是这个」
  return isRightAnswer ? 'correct' : ''
}

onShow(() => {
  ensureLogin()
})

onLoad(async (query) => {
  sessionId.value = Number(query?.id || 0)
  try {
    await load()
  } catch {
    // unwrap 已提示
  }
})

async function load() {
  session.value = await getSession(sessionId.value)
  const firstUnanswered = session.value.questions.findIndex((q) => !q.answered)
  current.value = firstUnanswered === -1 ? 0 : firstUnanswered
  syncPicked()
}

function syncPicked() {
  const q = currentQuestion.value
  feedback.value = null
  if (!q) return
  if (q.type === 'MULTI') {
    picked.value = q.answered ? [...q.userAnswers] : []
  } else {
    pickedRadio.value = q.answered ? q.userAnswers[0] || '' : ''
  }
}

function isPicked(label: string): boolean {
  const q = currentQuestion.value
  if (!q) return false
  return q.type === 'MULTI' ? picked.value.includes(label) : pickedRadio.value === label
}

function onPick(label: string) {
  if (submitted.value) return
  const q = currentQuestion.value
  if (!q) return
  if (q.type === 'MULTI') {
    picked.value = picked.value.includes(label)
      ? picked.value.filter((item) => item !== label)
      : [...picked.value, label]
  } else {
    pickedRadio.value = label
  }
}

function go(index: number) {
  if (index < 0 || index >= questions.value.length) return
  current.value = index
  syncPicked()
}

function onNext() {
  if (current.value === questions.value.length - 1) {
    onFinish()
    return
  }
  go(current.value + 1)
}

async function onSubmit() {
  const q = currentQuestion.value
  if (!q) return
  const answer = q.type === 'MULTI' ? picked.value : pickedRadio.value ? [pickedRadio.value] : []
  if (!answer.length) {
    uni.showToast({ title: '请先选择答案', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const result = await submitAnswer(sessionId.value, q.questionId, answer)
    // 刷新会话（answered / userAnswers / obtainedScore），但**停在刚提交的这题**上。
    // 不能让 load() 决定当前题：它会把位置挪到「第一道未答题」，于是反馈张冠李戴
    // （题干是下一题、解析还是这题），而且按钮变成「下一题」，一点就跳过一道未答题。
    const index = current.value
    await load()
    current.value = index
    syncPicked()
    feedback.value = result
  } catch {
    // unwrap 已提示
  } finally {
    submitting.value = false
  }
}

async function onFinish() {
  try {
    await finishSession(sessionId.value)
    uni.redirectTo({ url: `/pages/result/index?id=${sessionId.value}` })
  } catch {
    // unwrap 已提示
  }
}

function onAbandon() {
  uni.showModal({
    title: '提示',
    content: '放弃后本次答题不会再出现在未完成列表，确认放弃？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await abandonSession(sessionId.value)
        uni.reLaunch({ url: '/pages/home/index' })
      } catch {
        // unwrap 已提示
      }
    }
  })
}

function typeLabel(type: string) {
  return ({ SINGLE: '单选', MULTI: '多选', JUDGE: '判断' } as Record<string, string>)[type] || type
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 200rpx;
}

.head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16rpx;
}
.head-main {
  display: flex;
  align-items: baseline;
}
.counter {
  font-size: $text-md;
  font-weight: 500;
  color: $app-ink;
}
.head-score {
  margin-left: 16rpx;
  font-size: $text-sm;
  color: $app-ink-muted;
}
.head-right {
  display: flex;
  align-items: baseline;
}
.percent {
  font-size: $text-sm;
  color: $app-ink-muted;
}
.abandon {
  margin-left: 20rpx;
  font-size: $text-sm;
  color: $app-ink-subtle;
}

.progress {
  height: 8rpx;
  border-radius: 4rpx;
  background: $app-line-soft;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: 4rpx;
  background: $app-brand;
}

.stem-card {
  padding: 28rpx;
  margin-top: 24rpx;
}
.tags {
  display: flex;
  gap: 12rpx;
  margin-bottom: 16rpx;
}
.stem {
  font-size: $text-lg;
  line-height: 1.75;
  color: $app-ink;
}

.options {
  margin-top: 24rpx;
}
.locked-tip {
  padding: 16rpx 20rpx;
  border-radius: $radius-md;
  background: $app-surface-2;
  color: $app-ink-muted;
  font-size: $text-sm;
  line-height: 1.5;
}

.option {
  display: flex;
  align-items: flex-start;
  padding: 20rpx;
  margin: 12rpx 0;
  border-radius: $radius-md;
  background: $app-surface;
  border: 1rpx solid $app-line-soft;
}
.option.picked {
  background: $app-brand-soft;
  border-color: $app-brand;
}
.option.correct {
  background: $app-ok-soft;
  border-color: $app-ok;
}
.option.wrong {
  background: $app-danger-soft;
  border-color: $app-danger;
}

.marker {
  flex: none;
  width: 36rpx;
  height: 36rpx;
  margin: 6rpx 16rpx 0 0;
  border: 2rpx solid $app-line;
  border-radius: 50%;
  background: $app-reader;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
}
.marker--square {
  border-radius: 8rpx;
}
.option.picked .marker {
  background: $app-brand;
  border-color: $app-brand;
}
.option.correct .marker {
  background: $app-ok;
  border-color: $app-ok;
}
.option.wrong .marker {
  background: $app-danger;
  border-color: $app-danger;
}
.marker-tick {
  color: #fff;
  font-size: 22rpx;
  line-height: 1;
}

.option-body {
  flex: 1;
  display: flex;
  align-items: flex-start;
}
.opt-label {
  flex: none;
  font-weight: 500;
  margin-right: 8rpx;
  color: $app-ink;
}
.option.correct .opt-label {
  color: $app-ink-green;
}
.option.wrong .opt-label {
  color: $app-danger;
}
.option-content {
  flex: 1;
}

.feedback {
  margin-top: 24rpx;
  padding: 24rpx 28rpx;
  border-radius: $radius-lg;
  background: $app-surface;
}
.fb-title {
  font-size: $text-lg;
  font-weight: 500;
  margin-bottom: 8rpx;
}
.fb-title.ok {
  color: $app-ok;
}
.fb-title.bad {
  color: $app-danger;
}
.fb-answer {
  font-size: $text-base;
  color: $app-ink-muted;
}

.analysis {
  margin-top: 24rpx;
  padding: 24rpx 28rpx;
}
.analysis-title {
  font-size: $text-md;
  font-weight: 500;
  color: $app-ink;
  margin-bottom: 8rpx;
}

.actionbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: $app-reader;
  border-top: 1rpx solid $app-line;
  padding-bottom: env(safe-area-inset-bottom);
}
.action-inner {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx;
}
.btn-prev {
  flex: none;
  width: 200rpx;
}
.btn-main {
  flex: 1;
}
</style>
