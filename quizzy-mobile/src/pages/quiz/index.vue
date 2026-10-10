<template>
  <view class="page">
    <template v-if="session">
      <!-- 顶栏自绘（本页 navigationStyle: custom，没有系统导航栏）：浅色品牌头，对齐 Stitch V1 -->
      <view class="qz-nav">
        <text class="brand">quizzy</text>
        <text class="quit" @click="onAbandon">放弃本次</text>
      </view>

      <view class="head">
        <view class="head-left">
          <text class="counter">第 {{ current + 1 }} / {{ questions.length }} 题</text>
          <text class="percent-chip">{{ percent }}%</text>
        </view>
        <text class="head-score">已得 {{ session.obtainedScore }} 分</text>
      </view>

      <view class="progress">
        <view class="progress-fill" :style="{ width: percent + '%' }" />
      </view>

      <template v-if="currentQuestion">
        <!-- 题干与选项同卡（V1：一张问卷面，选项间细分割线；选中 / 对 / 错整行染色） -->
        <view class="qcard">
          <view class="stem-block">
            <view class="tags">
              <text class="qz-tag qz-tag--brand">{{ typeLabel(currentQuestion.type) }}</text>
              <text class="qz-tag">{{ currentQuestion.score }} 分</text>
            </view>
            <view class="stem">
              <MarkdownRenderer :source="currentQuestion.stem" />
            </view>
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
              <view v-if="showPickDot(option.label)" class="marker-dot" />
              <text v-else-if="markerGlyph(option.label)" class="marker-glyph">
                {{ markerGlyph(option.label) }}
              </text>
            </view>
            <view class="option-body">
              <text class="opt-label">{{ option.label }}.</text>
              <view class="option-content">
                <MarkdownRenderer :source="option.content" />
              </view>
            </view>
          </view>
        </view>

        <!-- 作答提示在选项卡下方（V1 位置）；提交那一刻起就显示 -->
        <view class="locked-tip" v-if="submitted">
          本题已作答，答案不可修改；想重做可以另开一次练习。
        </view>

        <view class="feedback" v-if="showVerdict">
          <view class="fb-row">
            <text :class="['fb-badge', verdictCorrect ? 'ok' : 'bad']">
              {{ verdictCorrect ? '回答正确' : '回答错误' }}
            </text>
            <text class="fb-answer">正确答案：{{ verdictAnswers.join(', ') }}</text>
          </view>
          <view class="fb-analysis" v-if="verdictAnalysis">
            <MarkdownRenderer :source="analysisSource" />
          </view>
        </view>
      </template>
    </template>

    <view v-else class="qz-loading">加载中…</view>

    <!-- 底部操作条：一屏只留一个主按钮，「放弃」已降级为顶部文字链 -->
    <view class="actionbar" v-if="session">
      <view class="action-inner">
        <view
          :class="['qz-btn', 'btn-prev', current === 0 ? 'qz-btn--disabled' : '']"
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
const verdictAnswers = computed<string[]>(() =>
  feedback.value ? feedback.value.correctAnswers : currentQuestion.value?.correctAnswers || [])
const verdictAnalysis = computed(() =>
  (feedback.value ? feedback.value.analysis : currentQuestion.value?.analysis) || '')
const verdictCorrect = computed(() =>
  feedback.value ? feedback.value.isCorrect : currentQuestion.value?.isCorrect === true)
// 有答案就显示：未作答时两者皆空，于是自然不显示
const showVerdict = computed(() => verdictAnswers.value.length > 0 || verdictAnalysis.value !== '')

// V1 设计稿里解析以「解析：」直接起段（没有单独的小标题），这里补上前缀；
// 题库数据若已自带前缀则不重复加。
const analysisSource = computed(() => {
  const text = verdictAnalysis.value
  if (!text) return ''
  return text.startsWith('解析') ? text : `解析：${text}`
})

/**
 * 选项的三种结果态（2026-10-09 定口径，2026-10-10 按 V1 设计稿改样式）：
 *   picked  未作答时的选中——浅蓝洗底 + 蓝底白点（**选中不画勾**，勾只表「对」）
 *   correct 已作答，且该选项是正确答案——绿底 + 白勾
 *   wrong   已作答，且该选项是用户选错的——红底 + 白叉
 * ⚠️ 颜色从不单独表意：绿 / 红同时配有「回答正确 / 回答错误」徽章与「正确答案」一行，
 *    色盲与高对比模式下照样读得出来。
 */
function optionState(label: string): string {
  if (!submitted.value) return isPicked(label) ? 'picked' : ''
  const isRightAnswer = verdictAnswers.value.includes(label)
  if (isPicked(label)) return isRightAnswer ? 'correct' : 'wrong'
  // 用户没选、但确实是正确答案——一并染绿，指明「该选的是这个」
  return isRightAnswer ? 'correct' : ''
}

// 未判分时的「已选」标记：白点；判分后才换成勾 / 叉
function showPickDot(label: string): boolean {
  return !submitted.value && isPicked(label)
}

// 勾 / 叉只表达对错：正确答案画勾，选错的画叉（主人 2026-10-10 明示）
function markerGlyph(label: string): string {
  if (!submitted.value) return ''
  if (verdictAnswers.value.includes(label)) return '✓'
  return isPicked(label) ? '✗' : ''
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
  padding: 0 32rpx 220rpx;
}

/* 浅色品牌顶栏（本页 navigationStyle: custom，没有系统导航栏；
   小程序端状态栏高度由 --status-bar-height 提供，H5 端为 0） */
.qz-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: calc(var(--status-bar-height, 0px) + 24rpx);
  /* #ifdef MP-WEIXIN */
  /* 小程序右上角有胶囊按钮，给「放弃本次」让出位置 */
  padding-right: 200rpx;
  /* #endif */
}
.brand {
  font-size: 36rpx;
  font-weight: 600;
  color: $app-ink-strong;
}
.quit {
  font-size: $text-sm;
  color: $app-ink-muted;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24rpx;
}
.head-left {
  display: flex;
  align-items: center;
}
.counter {
  font-size: $text-md;
  font-weight: 500;
  color: $app-ink;
}
.percent-chip {
  margin-left: 12rpx;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  background: #ffffff;
  font-size: $text-xs;
  font-weight: 500;
  line-height: 1.4;
  color: $app-brand;
}
.head-score {
  font-size: $text-base;
  color: $app-ink-muted;
}

.progress {
  margin-top: 14rpx;
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

/* 题干 + 选项的统一问卷卡（V1）：选项行不出卡、整行染色 */
.qcard {
  margin-top: 32rpx;
  background: $app-reader;
  border: 1rpx solid $app-line-soft;
  border-radius: $radius-xl;
  box-shadow: $shadow-sm;
  overflow: hidden;
}
.stem-block {
  padding: 32rpx 28rpx;
  border-bottom: 1rpx solid $app-divider;
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

.option {
  display: flex;
  align-items: flex-start;
  padding: 22rpx 28rpx;
}
.option + .option {
  border-top: 1rpx solid $app-divider;
}
.option.picked {
  background: $app-brand-wash;
}
.option.correct {
  background: $app-ok-soft;
}
.option.wrong {
  background: $app-danger-soft;
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
/* 已选（未判分）：蓝底白点 */
.marker-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #ffffff;
}
/* 判分后：对勾 / 错叉 */
.marker-glyph {
  color: #ffffff;
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

/* 作答提示条（V1）：卡下方浅蓝面 + 细边 */
.locked-tip {
  margin-top: 28rpx;
  padding: 18rpx 28rpx;
  border-radius: $radius-md;
  background: $app-surface-2;
  border: 1rpx solid $app-line;
  color: $app-ink-muted;
  font-size: $text-sm;
  line-height: 1.5;
}

/* 判分反馈卡（V1）：徽章 + 正确答案同行，解析同卡、上分割线 */
.feedback {
  margin-top: 28rpx;
  padding: 28rpx;
  border-radius: $radius-lg;
  background: $app-reader;
  border: 1rpx solid $app-line-soft;
  box-shadow: $shadow-sm;
}
.fb-row {
  display: flex;
  align-items: center;
}
.fb-badge {
  flex: none;
  padding: 4rpx 16rpx;
  border-radius: 12rpx;
  font-size: $text-base;
  font-weight: 600;
  line-height: 1.5;
}
.fb-badge.bad {
  background: $app-danger-soft;
  color: $app-danger;
}
.fb-badge.ok {
  background: $app-ok-soft;
  color: $app-ink-green;
}
.fb-answer {
  margin-left: 16rpx;
  font-size: $text-md;
  font-weight: 500;
  color: $app-ink;
}
.fb-analysis {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid $app-divider;
  font-size: $text-base;
  color: $app-ink-muted;
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
  gap: 24rpx;
  padding: 16rpx 32rpx;
}
.action-inner .qz-btn {
  height: 96rpx;
  border-radius: 24rpx;
}
.btn-prev {
  flex: none;
  width: 192rpx;
}
.btn-main {
  flex: 1;
}
</style>
