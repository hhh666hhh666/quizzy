<template>
  <view class="page">
    <template v-if="session">
      <view class="head">
        <view class="title">{{ session.title }}</view>
        <view class="meta">
          第 {{ current + 1 }} / {{ questions.length }} 题 · 已得 {{ session.obtainedScore }} 分
        </view>
      </view>
      <progress :percent="answeredPercent" stroke-width="6" activeColor="#409eff" />

      <view class="card" v-if="currentQuestion">
        <view class="tags">
          <wd-tag type="primary">{{ typeLabel(currentQuestion.type) }}</wd-tag>
          <wd-tag>{{ currentQuestion.score }} 分</wd-tag>
        </view>

        <view class="stem">
          <MarkdownRenderer :source="currentQuestion.stem" />
        </view>

        <!-- 选项自己排：整行可点、内容多行自适应。
             组件库的 radio / checkbox 会把块级内容排成居中文本（选项一多就参差不齐），
             而且点击落在行内元素上不一定触发选中。 -->
        <view class="options">
          <view
            v-for="option in currentQuestion.options"
            :key="option.label"
            :class="['option', { picked: isPicked(option.label), locked: submitted }]"
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

        <view class="feedback" v-if="feedback">
          <view :class="['fb-title', feedback.isCorrect ? 'ok' : 'bad']">
            {{ feedback.isCorrect ? '回答正确' : '回答错误' }}
          </view>
          <view class="fb-answer">正确答案：{{ feedback.correctAnswers.join(', ') }}</view>
          <view class="fb-analysis" v-if="feedback.analysis">
            <MarkdownRenderer :source="feedback.analysis" />
          </view>
        </view>
      </view>

      <view class="actions">
        <wd-button :disabled="current === 0" plain @click="go(current - 1)">上一题</wd-button>
        <wd-button v-if="!submitted" type="primary" :loading="submitting" @click="onSubmit">
          提交本题
        </wd-button>
        <wd-button v-else type="primary" @click="go(current + 1)">下一题</wd-button>
      </view>
      <view class="actions">
        <wd-button v-if="current === questions.length - 1" type="success" block @click="onFinish">
          结束并查看结果
        </wd-button>
      </view>
      <view class="actions">
        <wd-button type="error" plain block @click="onAbandon">放弃本次</wd-button>
      </view>
    </template>

    <view v-else class="loading">加载中…</view>
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
const answeredPercent = computed(() => {
  if (!questions.value.length) return 0
  return Math.round((questions.value.filter((q) => q.answered).length * 100) / questions.value.length)
})

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
  padding: 24rpx;
}
.head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12rpx;
}
.title {
  font-size: 32rpx;
  font-weight: 600;
}
.meta {
  font-size: 24rpx;
  color: $app-text-secondary;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-top: 24rpx;
}
.tags {
  display: flex;
  gap: 12rpx;
  margin-bottom: 16rpx;
}
.stem {
  margin-bottom: 16rpx;
}
.options {
  margin-top: 8rpx;
}
.option {
  display: flex;
  align-items: flex-start;
  padding: 20rpx 16rpx;
  margin: 12rpx 0;
  border-radius: 12rpx;
  background: #f7f8fa;
}
.option.picked {
  background: #ecf5ff;
}
.marker {
  flex: none;
  width: 40rpx;
  height: 40rpx;
  margin: 4rpx 16rpx 0 0;
  border: 2rpx solid #c8c9cc;
  border-radius: 50%;
  background: #fff;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
}
.marker--square {
  border-radius: 8rpx;
}
.option.picked .marker {
  background: $app-primary;
  border-color: $app-primary;
}
.marker-tick {
  color: #fff;
  font-size: 24rpx;
  line-height: 1;
}
.option-body {
  flex: 1;
  display: flex;
  align-items: flex-start;
}
.opt-label {
  flex: none;
  font-weight: 600;
  margin-right: 8rpx;
}
.option-content {
  flex: 1;
}
.feedback {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #ebeef5;
}
.fb-title {
  font-weight: 600;
  margin-bottom: 8rpx;
}
.fb-title.ok {
  color: #34d19d;
}
.fb-title.bad {
  color: #fa4350;
}
.fb-answer {
  font-size: 26rpx;
  color: $app-text-secondary;
}
.fb-analysis {
  margin-top: 12rpx;
}
.actions {
  margin-top: 24rpx;
  display: flex;
  gap: 16rpx;
}
.actions > * {
  flex: 1;
}
.loading {
  padding: 80rpx 0;
  text-align: center;
  color: $app-text-secondary;
}
</style>
