<template>
  <view class="page">
    <view class="hint qz-muted">试卷在 PC 端维护，这里只能看和开始答题。</view>

    <view class="qz-card item" v-for="p in list" :key="p.id">
      <view class="item-head">
        <text class="title">{{ p.title }}</text>
        <text class="qz-tag">{{ p.mode === 'FIXED' ? '固定卷' : '规则卷' }}</text>
      </view>
      <view class="desc" v-if="p.description">{{ p.description }}</view>
      <view class="foot">
        <text class="count">{{ p.questionCount }} 题</text>
        <view :class="['start', startingId === p.id ? 'start--busy' : '']" @click="onStart(p)">
          {{ startingId === p.id ? '正在开始…' : '开始答题' }}
        </view>
      </view>
    </view>

    <view class="qz-empty" v-if="!loading && !list.length">还没有试卷</view>
    <view class="more qz-muted" v-else-if="!hasMore && list.length">没有更多了</view>
    <view class="qz-loading" v-else-if="loading">加载中…</view>
  </view>
</template>

<script setup lang="ts">
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { pagePapers } from '@/api/paper'
import { startQuiz } from '@/api/quiz'
import { ensureLogin } from '@/utils/auth'
import type { PaperVO } from '@/types'

const SIZE = 20
const list = ref<PaperVO[]>([])
const page = ref(1)
const hasMore = ref(true)
const loading = ref(false)
const startingId = ref(0)

onShow(() => {
  if (!ensureLogin()) return
  reset()
})

onReachBottom(() => {
  if (hasMore.value && !loading.value) load()
})

function reset() {
  list.value = []
  page.value = 1
  hasMore.value = true
  load()
}

async function load() {
  loading.value = true
  try {
    const res = await pagePapers(page.value, SIZE)
    list.value = [...list.value, ...res.list]
    hasMore.value = list.value.length < res.total
    if (hasMore.value) page.value += 1
  } catch {
    // unwrap 已提示
  } finally {
    loading.value = false
  }
}

async function onStart(p: PaperVO) {
  if (startingId.value) return
  startingId.value = p.id
  try {
    const sessionId = await startQuiz({ sourceType: 'PAPER', paperId: p.id })
    uni.navigateTo({ url: `/pages/quiz/index?id=${sessionId}` })
  } catch {
    // unwrap 已提示（如「这张卷还没有题目」）
  } finally {
    startingId.value = 0
  }
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 48rpx;
}
.hint {
  padding: 4rpx 8rpx 20rpx;
  font-size: $text-sm;
}
.item {
  padding: 24rpx 28rpx;
}
.item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}
.title {
  flex: 1;
  font-size: $text-lg;
  color: $app-ink;
}
.desc {
  margin-top: 8rpx;
  font-size: $text-sm;
  color: $app-ink-muted;
  line-height: 1.6;
}
.foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid $app-line-soft;
}
.count {
  font-size: $text-sm;
  color: $app-ink-muted;
}
.start {
  padding: 12rpx 28rpx;
  border-radius: $radius-sm;
  background: $app-brand-soft;
  color: $app-ink-blue;
  font-size: $text-sm;
}
.start--busy {
  opacity: 0.5;
}
.more {
  padding: 24rpx 0 8rpx;
  text-align: center;
  font-size: $text-sm;
}
</style>
