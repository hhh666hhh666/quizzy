<template>
  <view class="page">
    <view class="qz-card">
      <view class="row">
        <view class="label">分类</view>
        <picker mode="selector" :range="categoryNames" :value="categoryIndex" @change="onCategoryChange">
          <view class="picker">{{ categoryNames[categoryIndex] || '不限' }}</view>
        </picker>
      </view>

      <view class="row block">
        <view class="label">题型</view>
        <wd-checkbox-group v-model="rule.types" inline>
          <wd-checkbox model-value="SINGLE">单选</wd-checkbox>
          <wd-checkbox model-value="MULTI">多选</wd-checkbox>
          <wd-checkbox model-value="JUDGE">判断</wd-checkbox>
        </wd-checkbox-group>
      </view>

      <view class="row block">
        <view class="label">难度</view>
        <wd-checkbox-group v-model="rule.difficulties" inline>
          <wd-checkbox model-value="EASY">简单</wd-checkbox>
          <wd-checkbox model-value="MEDIUM">中等</wd-checkbox>
          <wd-checkbox model-value="HARD">困难</wd-checkbox>
        </wd-checkbox-group>
      </view>

      <view class="row">
        <view class="label">题量</view>
        <wd-input-number v-model="rule.count" :min="1" :max="200" />
      </view>

      <view class="row last">
        <view class="label">排除近期</view>
        <wd-input-number v-model="rule.excludeRecentDays" :min="0" :max="365" />
      </view>
    </view>

    <view class="tip">排除最近 N 天做过的题，0 表示不排除</view>

    <view :class="['qz-btn', 'qz-btn--primary', 'qz-btn--block', starting ? 'qz-btn--disabled' : '']" @click="onStart">
      {{ starting ? '正在抽题…' : '开始练习' }}
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { listCategories, type CategoryVO } from '@/api/question'
import { startQuiz } from '@/api/quiz'
import { ensureLogin } from '@/utils/auth'
import type { Difficulty, PaperRuleDTO, QuestionType } from '@/types'

const categories = ref<CategoryVO[]>([])
const categoryIndex = ref(0)
const starting = ref(false)

// 「不限」作为第 0 项，这样选择器只需要一个 index
const categoryNames = computed<string[]>(() => ['不限', ...categories.value.map((c) => c.name)])

const rule = ref<Omit<PaperRuleDTO, 'excludeRecentDays'> & { excludeRecentDays: number }>({
  categoryId: null,
  tagIds: [],
  types: [],
  difficulties: [],
  count: 20,
  excludeRecentDays: 0
})

onShow(() => {
  ensureLogin()
})

onLoad(async () => {
  try {
    categories.value = await listCategories()
  } catch {
    // 分类拉不到不阻塞练习，分类留「不限」
  }
})

function onCategoryChange(e: { detail: { value: number | string } }) {
  categoryIndex.value = Number(e.detail.value)
  const picked = categories.value[categoryIndex.value - 1]
  rule.value.categoryId = picked ? picked.id : null
}

async function onStart() {
  if (starting.value) return
  starting.value = true
  try {
    const payload = {
      sourceType: 'QUICK' as const,
      rule: {
        categoryId: rule.value.categoryId,
        tagIds: [],
        types: rule.value.types as QuestionType[],
        difficulties: rule.value.difficulties as Difficulty[],
        count: rule.value.count,
        excludeRecentDays: rule.value.excludeRecentDays
      }
    }
    const sessionId = await startQuiz(payload)
    uni.navigateTo({ url: `/pages/quiz/index?id=${sessionId}` })
  } catch {
    // unwrap 已提示（如「没有符合要求的题目」）
  } finally {
    starting.value = false
  }
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 48rpx;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid $app-line-soft;
}
.row.block {
  display: block;
}
.row.last {
  border-bottom: none;
}
.label {
  color: $app-ink-muted;
}
.row.block .label {
  margin-bottom: 12rpx;
}
.picker {
  color: $app-ink;
}
.tip {
  padding: 8rpx 8rpx 32rpx;
  font-size: $text-sm;
  color: $app-ink-muted;
}
</style>
