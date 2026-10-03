<template>
  <view class="page">
    <template v-if="result">
      <view class="card summary">
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

      <view class="card item" v-for="item in result.items" :key="item.questionId">
        <view class="item-head">
          <text class="index">第 {{ item.index + 1 }} 题</text>
          <wd-tag type="primary">{{ typeLabel(item.type) }}</wd-tag>
          <wd-tag :type="item.correct ? 'success' : item.answered ? 'danger' : 'default'">
            {{ item.correct ? '正确' : item.answered ? '错误' : '未作答' }}
          </wd-tag>
          <text class="score">{{ item.correct ? item.score : 0 }} / {{ item.score }} 分</text>
        </view>

        <MarkdownRenderer :source="item.stem" />

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

        <view class="analysis" v-if="item.analysis">
          <view class="analysis-title">解析</view>
          <MarkdownRenderer :source="item.analysis" />
        </view>
      </view>

      <view class="actions">
        <wd-button type="primary" block @click="goHome">返回首页</wd-button>
      </view>
    </template>

    <view v-else class="loading">加载中…</view>
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
  padding: 24rpx;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
}
.summary {
  display: flex;
  margin-bottom: 16rpx;
}
.stat {
  flex: 1;
  text-align: center;
}
.num {
  font-size: 32rpx;
  font-weight: 600;
}
.lbl {
  margin-top: 4rpx;
  font-size: 24rpx;
  color: $app-text-secondary;
}
.tip {
  padding: 20rpx 24rpx;
  margin-bottom: 24rpx;
  background: #fdf6ec;
  color: #e6a23c;
  border-radius: 12rpx;
  font-size: 26rpx;
}
.item-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 12rpx;
}
.index {
  font-weight: 600;
}
.score {
  font-size: 24rpx;
  color: $app-text-secondary;
}
.option-list {
  margin: 16rpx 0;
}
.option {
  padding: 8rpx 12rpx;
  border-radius: 8rpx;
  margin: 8rpx 0;
}
.option.correct {
  background: #f0f9eb;
  color: #67c23a;
}
.option.wrong {
  background: #fef0f0;
  color: #f56c6c;
}
.opt-label {
  font-weight: 600;
  margin-right: 8rpx;
}
.answers {
  font-size: 26rpx;
  color: $app-text-secondary;
}
.analysis {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #ebeef5;
}
.analysis-title {
  font-weight: 600;
  margin-bottom: 8rpx;
}
.actions {
  margin: 32rpx 0 48rpx;
}
.loading {
  padding: 80rpx 0;
  text-align: center;
  color: $app-text-secondary;
}
</style>
