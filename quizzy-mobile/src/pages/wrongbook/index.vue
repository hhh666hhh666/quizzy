<template>
  <view class="page">
    <view class="summary" v-if="total">
      <text class="count">共 {{ total }} 题</text>
      <text class="rule">连续答对 2 次后自动移出</text>
    </view>

    <view class="qz-card item" v-for="q in list" :key="q.id">
      <view class="tags">
        <text class="qz-tag qz-tag--brand">{{ typeLabel(q.type) }}</text>
        <text class="qz-tag">{{ q.categoryName || '未分类' }}</text>
        <text class="qz-tag">{{ difficultyLabel(q.difficulty) }}</text>
      </view>
      <view class="stem">{{ q.stem }}</view>
      <view class="item-foot">
        <text class="score">{{ q.score }} 分</text>
        <text class="remove" @click="onRemove(q.id)">移出</text>
      </view>
    </view>

    <view class="qz-empty" v-if="!loading && !list.length">错题本是空的</view>
    <view class="more qz-muted" v-else-if="!hasMore && list.length">没有更多了</view>

    <view class="actionbar" v-if="total">
      <view class="action-inner">
        <view :class="['qz-btn', 'qz-btn--primary', 'qz-btn--block', practicing ? 'qz-btn--disabled' : '']" @click="onPractice">
          {{ practicing ? '正在抽题…' : '练错题' }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { pageWrongBook, practiceWrongBook, removeFromWrongBook } from '@/api/wrongbook'
import { startQuiz } from '@/api/quiz'
import { ensureLogin } from '@/utils/auth'
import type { Difficulty, QuestionListItemVO, QuestionType } from '@/types'

const SIZE = 20
const list = ref<QuestionListItemVO[]>([])
const total = ref(0)
const page = ref(1)
const hasMore = ref(true)
const loading = ref(false)
const practicing = ref(false)

// 每次进来都重拉：练完一轮回来，错题集合大概率已经变了（连续答对会自动移出）
onShow(() => {
  if (!ensureLogin()) return
  reset()
})

onReachBottom(() => {
  if (hasMore.value && !loading.value) load()
})

function reset() {
  list.value = []
  total.value = 0
  page.value = 1
  hasMore.value = true
  load()
}

async function load() {
  loading.value = true
  try {
    const res = await pageWrongBook(page.value, SIZE)
    list.value = [...list.value, ...res.list]
    total.value = res.total
    hasMore.value = list.value.length < res.total
    if (hasMore.value) page.value += 1
  } catch {
    // unwrap 已提示
  } finally {
    loading.value = false
  }
}

function onRemove(questionId: number) {
  uni.showModal({
    title: '提示',
    content: '移出后这题不再出现在错题本，确认移出？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await removeFromWrongBook(questionId)
        list.value = list.value.filter((q) => q.id !== questionId)
        total.value = Math.max(0, total.value - 1)
        uni.showToast({ title: '已移出', icon: 'none' })
      } catch {
        // unwrap 已提示
      }
    }
  })
}

async function onPractice() {
  if (practicing.value) return
  practicing.value = true
  try {
    // 错题本练习走后端自己的 practice（自带「练哪些题」的规则），不用前端拼 rule
    const sessionId = await practiceWrongBook(20)
    uni.navigateTo({ url: `/pages/quiz/index?id=${sessionId}` })
  } catch {
    // unwrap 已提示（如「错题本是空的」）
  } finally {
    practicing.value = false
  }
}

function typeLabel(type: QuestionType) {
  return ({ SINGLE: '单选', MULTI: '多选', JUDGE: '判断' } as Record<string, string>)[type] || type
}

function difficultyLabel(d: Difficulty) {
  return ({ EASY: '简单', MEDIUM: '中等', HARD: '困难' } as Record<string, string>)[d] || d
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 180rpx;
}
.summary {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 4rpx 8rpx 20rpx;
}
.count {
  font-size: $text-md;
  font-weight: 500;
  color: $app-ink;
}
.rule {
  font-size: $text-sm;
  color: $app-ink-muted;
}

.item {
  padding: 24rpx 28rpx;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 12rpx;
}
.stem {
  font-size: $text-md;
  line-height: 1.7;
  color: $app-ink;
}
.item-foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid $app-line-soft;
}
.score {
  font-size: $text-sm;
  color: $app-ink-muted;
}
.remove {
  font-size: $text-sm;
  color: $app-danger;
}

.more {
  padding: 24rpx 0 8rpx;
  text-align: center;
  font-size: $text-sm;
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
  padding: 16rpx 24rpx;
}
</style>
