<template>
  <view class="page">
    <view class="hero">
      <view class="hello">{{ store.user?.nickname || store.user?.username || '同学' }}</view>
      <view class="sub">今天也来练几道吧</view>
    </view>

    <view class="entries">
      <view class="entry" @click="go('/pages/quick/index')">
        <view class="entry-main">
          <text class="entry-title">快速练习</text>
          <text class="entry-sub">按分类 / 题型 / 难度随机抽题</text>
        </view>
        <text class="arrow">›</text>
      </view>

      <view class="entry" @click="go('/pages/wrongbook/index')">
        <view class="entry-main">
          <text class="entry-title">错题本</text>
          <text class="entry-sub">答错的题，连续答对后自动移出</text>
        </view>
        <text class="arrow">›</text>
      </view>

      <view class="entry" @click="go('/pages/history/index')">
        <view class="entry-main">
          <text class="entry-title">答题记录</text>
          <text class="entry-sub">看每次练习的结果与解析</text>
        </view>
        <text class="arrow">›</text>
      </view>

      <view class="entry" @click="go('/pages/paper/index')">
        <view class="entry-main">
          <text class="entry-title">试卷</text>
          <text class="entry-sub">从已有试卷开始答题（只读）</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="logout" @click="onLogout">退出登录</view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { ensureLogin } from '@/utils/auth'

const store = useUserStore()

onShow(async () => {
  if (!ensureLogin()) return
  try {
    await store.loadUser()
  } catch {
    // 401 已由 unwrap 处理（清 token + 回登录页）
  }
})

function go(url: string) {
  uni.navigateTo({ url })
}

function onLogout() {
  uni.showModal({
    title: '提示',
    content: '确定退出登录？',
    success: (res) => {
      if (!res.confirm) return
      store.logout()
      uni.reLaunch({ url: '/pages/login/index' })
    }
  })
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 48rpx;
}
.hero {
  padding: 32rpx 8rpx 36rpx;
}
.hello {
  font-size: $text-2xl;
  font-weight: 500;
  color: $app-ink-strong;
}
.sub {
  margin-top: 8rpx;
  font-size: $text-md;
  color: $app-ink-muted;
}

.entries {
  background: $app-surface;
  border-radius: $radius-lg;
  box-shadow: $shadow-sm;
  overflow: hidden;
}
.entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx;
  border-bottom: 1rpx solid $app-line-soft;
}
.entry:last-child {
  border-bottom: none;
}
.entry-main {
  flex: 1;
}
.entry-title {
  display: block;
  font-size: $text-lg;
  color: $app-ink;
}
.entry-sub {
  display: block;
  margin-top: 4rpx;
  font-size: $text-sm;
  color: $app-ink-muted;
}
.arrow {
  flex: none;
  margin-left: 16rpx;
  font-size: 36rpx;
  color: $app-ink-subtle;
}

/* 退出是低频且不可逆的操作，降级为文字链，不再用整块危险色按钮抢视觉重心 */
.logout {
  margin-top: 40rpx;
  text-align: center;
  font-size: $text-sm;
  color: $app-ink-subtle;
}
</style>
