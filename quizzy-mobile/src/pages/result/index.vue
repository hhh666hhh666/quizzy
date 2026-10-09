<template>
  <view class="page">
    <template v-if="result">
      <view class="qz-card summary">
        <view class="stat">
          <view class="num">{{ result.obtainedScore }} / {{ result.totalScore }}</view>
          <view class="lbl">得分</view>
        </view>
        <view class="stat">
          <view class="num">{{ result.accuracy }}%</view>
          <view class="lbl">正确率</view>
        </view>
        <view class="stat">
          <view class="num">{{ result.correctCount }} / {{ result.answeredCount }}</view>
          <view class="lbl">答对</view>
        </view>
        <view class="stat">
          <view class="num">{{ result.unansweredCount }}</view>
          <view class="lbl">未作答</view>
        </view>
      </view>

      <view class="tip" v-if="result.unansweredCount">
        还有 {{ result.unansweredCount }} 道题没有作答，正确率只统计已作答的题目。
      </view>

      <view class="qz-card item" v-for="item in result.items" :key="item.questionId">
        <view class="item-head">
          <text class="index">第 {{ item.index + 1 }} 题</text>
          <text class="qz-tag">{{ typeLabel(item.type) }}</text>
          <text
            :class="['qz-tag', item.correct ? 'qz-tag--ok' : item.answered ? 'qz-tag--danger' : 'qz-tag--warn']"
          >
            {{ item.correct ? '正确' : item.answered ? '错误' : '未作答' }}
          </text>
          <text class="score">{{ item.correct ? item.score : 0 }} / {{ item.score }} 分</text>
        </view>

        <view class="qz-reader stem-box">
          <MarkdownRenderer :source="item.stem" />
        </view>

        <view class="option-list">
          <view
            v-for="option in item.options"
            :key="option.label"
            :class="['option', optionClass(item, option.label)]"
          >
            <text class="opt-label">{{ option.label }}.</text>
            <MarkdownRenderer :source="option.content" />
          </view>
        </view>

        <view class="answers">
          你的答案：{{ item.userAnswers.length ? item.userAnswers.join(', ') : '未作答' }} · 正确答案：{{
            item.correctAnswers.join(', ')
          }}
        </view>

        <view class="qz-reader analysis" v-if="item.analysis">
          <view class="analysis-title">解析</view>
          <MarkdownRenderer :source="item.analysis" />
        </view>
      </view>

      <view class="qz-btn qz-btn--primary qz-btn--block" @click="goHome">返回首页</view>
    </template>

    <view v-else class="qz-loading">加载中…</view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { getSessionResult } from '@/api/quiz'
import { ensureLogin } from '@/utils/auth'
import type { QuizResultItemVO, SessionResultVO } from '@/types'

const result = ref<SessionResultVO | null>(null)

onShow(() => {
  ensureLogin()
})

onLoad(async (query) => {
  try {
    result.value = await getSessionResult(Number(query?.id || 0))
  } catch {
    // unwrap 已提示
  }
})

/**
 * 与答题页同一套口径：正确答案是绿的，用户选错的那项是红的。
 * ⚠️ 颜色从不单独表意——同一屏还有「正确 / 错误 / 未作答」文字标签与「正确答案」一行。
 */
function optionClass(item: QuizResultItemVO, label: string) {
  if (item.correctAnswers.includes(label)) return 'correct'
  if (item.userAnswers.includes(label)) return 'wrong'
  return ''
}

function goHome() {
  uni.reLaunch({ url: '/pages/home/index' })
}

function typeLabel(type: string) {
  return ({ SINGLE: '单选', MULTI: '多选', JUDGE: '判断' } as Record<string, string>)[type] || type
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 48rpx;
}

.summary {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24rpx 16rpx;
  padding: 32rpx 28rpx;
}
.stat {
  text-align: center;
}
.num {
  font-size: $text-xl;
  font-weight: 500;
  color: $app-ink;
}
.lbl {
  margin-top: 4rpx;
  font-size: $text-sm;
  color: $app-ink-muted;
}

.tip {
  padding: 20rpx 24rpx;
  margin-bottom: 24rpx;
  background: $app-warn-soft;
  color: $app-ink;
  border-radius: $radius-md;
  font-size: $text-base;
}

.item {
  padding: 24rpx 28rpx;
}
.item-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 16rpx;
}
.index {
  font-weight: 500;
  color: $app-ink;
}
.score {
  font-size: $text-sm;
  color: $app-ink-muted;
}

.stem-box {
  padding: 20rpx 24rpx;
  margin-bottom: 16rpx;
  font-size: $text-md;
  line-height: 1.7;
}

.option-list {
  margin: 16rpx 0;
}
.option {
  padding: 12rpx 16rpx;
  border-radius: $radius-sm;
  margin: 8rpx 0;
  color: $app-ink;
}
.option.correct {
  background: $app-ok-soft;
  color: $app-ink-green;
}
.option.wrong {
  background: $app-danger-soft;
  color: $app-danger;
}
.opt-label {
  font-weight: 500;
  margin-right: 8rpx;
}

.answers {
  font-size: $text-base;
  color: $app-ink-muted;
}

.analysis {
  margin-top: 20rpx;
  padding: 20rpx 24rpx;
}
.analysis-title {
  font-weight: 500;
  margin-bottom: 8rpx;
  color: $app-ink;
}
</style>
