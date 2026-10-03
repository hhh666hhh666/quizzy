<template>
  <view class="page">
    <view class="hero">
      <view class="hello">{{ store.user?.nickname || store.user?.username || '同学' }}</view>
      <view class="sub">今天也来练几道吧</view>
    </view>

    <view class="card">
      <wd-cell title="快速练习" label="按分类 / 题型 / 难度随机抽题" is-link @click="goQuick" />
    </view>

    <view class="card">
      <wd-cell title="错题本" value="下一轮" />
      <wd-cell title="答题记录" value="下一轮" />
      <wd-cell title="试卷" value="下一轮" />
    </view>

    <view class="actions">
      <wd-button type="error" plain block @click="onLogout">退出登录</wd-button>
    </view>
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

function goQuick() {
  uni.navigateTo({ url: '/pages/quick/index' })
}

function onLogout() {
  store.logout()
  uni.reLaunch({ url: '/pages/login/index' })
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
}
.hero {
  padding: 32rpx 16rpx 40rpx;
}
.hello {
  font-size: 44rpx;
  font-weight: 600;
}
.sub {
  margin-top: 8rpx;
  color: $app-text-secondary;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
}
.actions {
  margin-top: 40rpx;
}
</style>
