<template>
  <view class="page">
    <view
      :class="['qz-card', 'item', s.status === 'ABANDONED' ? 'item--muted' : '']"
      v-for="s in list"
      :key="s.id"
      @click="onOpen(s)"
    >
      <view class="item-head">
        <text class="title">{{ s.title }}</text>
        <text :class="['qz-tag', statusTag(s.status)]">{{ statusLabel(s.status) }}</text>
      </view>
      <view class="meta">
        <text class="meta-item">{{ sourceLabel(s.sourceType) }}</text>
        <text class="meta-item">{{ answeredOf(s) }} / {{ s.questionCount }} 题</text>
      </view>
      <view class="foot">
        <text class="time">{{ formatTime(s.startTime) }}</text>
        <text class="score" v-if="s.status === 'COMPLETED'">
          {{ s.obtainedScore }} / {{ s.totalScore }} 分
        </text>
        <text class="go" v-else-if="s.status === 'IN_PROGRESS'">继续作答 ›</text>
      </view>
    </view>

    <view class="qz-empty" v-if="!loading && !list.length">还没有答题记录</view>
    <view class="more qz-muted" v-else-if="!hasMore && list.length">没有更多了</view>
    <view class="qz-loading" v-else-if="loading">加载中…</view>
  </view>
</template>

<script setup lang="ts">
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { listSessions } from '@/api/quiz'
import { ensureLogin } from '@/utils/auth'
import type { SessionVO } from '@/types'

const SIZE = 20
const list = ref<SessionVO[]>([])
const page = ref(1)
const hasMore = ref(true)
const loading = ref(false)

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
    const res = await listSessions(page.value, SIZE)
    list.value = [...list.value, ...res.list]
    hasMore.value = list.value.length < res.total
    if (hasMore.value) page.value += 1
  } catch {
    // unwrap 已提示
  } finally {
    loading.value = false
  }
}

function onOpen(s: SessionVO) {
  // 已放弃的会话没有结果可看（后端不下发），也不该再进去作答，所以不给跳转
  if (s.status === 'ABANDONED') {
    uni.showToast({ title: '这次练习已放弃', icon: 'none' })
    return
  }
  if (s.status === 'IN_PROGRESS') {
    uni.navigateTo({ url: `/pages/quiz/index?id=${s.id}` })
    return
  }
  uni.navigateTo({ url: `/pages/result/index?id=${s.id}` })
}

function answeredOf(s: SessionVO) {
  return s.questions.filter((q) => q.answered).length
}

function statusLabel(status: string) {
  return ({ IN_PROGRESS: '进行中', COMPLETED: '已完成', ABANDONED: '已放弃' } as Record<string, string>)[status] || status
}

function statusTag(status: string) {
  if (status === 'COMPLETED') return 'qz-tag--ok'
  if (status === 'IN_PROGRESS') return 'qz-tag--brand'
  return 'qz-tag--warn'
}

function sourceLabel(type: string) {
  return ({ QUICK: '快速练习', PAPER: '试卷', WRONG_BOOK: '错题本', FAVORITE: '收藏夹' } as Record<string, string>)[type] || type
}

/** 后端 LocalDateTime 是 ISO 串（jackson 的 date-format 不管它），这里截到分钟。 */
function formatTime(value: string) {
  return (value || '').replace('T', ' ').slice(0, 16)
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 48rpx;
}
.item {
  padding: 24rpx 28rpx;
}
.item--muted {
  opacity: 0.6;
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
.meta {
  display: flex;
  gap: 20rpx;
  margin-top: 8rpx;
}
.meta-item {
  font-size: $text-sm;
  color: $app-ink-muted;
}
.foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid $app-line-soft;
}
.time {
  font-size: $text-sm;
  color: $app-ink-subtle;
}
.score {
  font-size: $text-sm;
  color: $app-ink-muted;
}
.go {
  font-size: $text-sm;
  color: $app-brand;
}
.more {
  padding: 24rpx 0 8rpx;
  text-align: center;
  font-size: $text-sm;
}
</style>
