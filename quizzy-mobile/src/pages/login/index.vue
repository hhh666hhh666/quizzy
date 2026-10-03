<template>
  <view class="page">
    <view class="card">
      <view class="brand">Quizzy</view>

      <view class="tabs">
        <view :class="['tab', tab === 'login' ? 'active' : '']" @click="tab = 'login'">登录</view>
        <view :class="['tab', tab === 'register' ? 'active' : '']" @click="tab = 'register'">注册</view>
      </view>

      <view class="form">
        <wd-input v-model="form.username" label="用户名" placeholder="请输入用户名" no-border clearable />
        <wd-input
          v-model="form.password"
          label="密码"
          placeholder="请输入密码"
          show-password
          no-border
        />
        <wd-input
          v-if="tab === 'register'"
          v-model="form.nickname"
          label="昵称"
          placeholder="可留空"
          no-border
          clearable
        />
      </view>

      <wd-button type="primary" block :loading="loading" @click="onSubmit">
        {{ tab === 'login' ? '登录' : '注册并登录' }}
      </wd-button>

      <view class="hint">登录后即可刷题；题库与组卷在 PC 端维护。</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { reactive, ref } from 'vue'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const tab = ref<'login' | 'register'>('login')
const loading = ref(false)
const form = reactive({ username: '', password: '', nickname: '' })

onShow(() => {
  if (store.token) {
    uni.reLaunch({ url: '/pages/home/index' })
  }
})

async function onSubmit() {
  if (!form.username || !form.password) {
    uni.showToast({ title: '请填写用户名与密码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    if (tab.value === 'login') {
      await store.login(form.username, form.password)
    } else {
      await store.register(form.username, form.password, form.nickname)
    }
    uni.reLaunch({ url: '/pages/home/index' })
  } catch {
    // unwrap 已经弹过 toast，这里只需要停住
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  box-sizing: border-box;
}
.card {
  width: 100%;
  max-width: 640rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 48rpx 40rpx;
  box-sizing: border-box;
}
.brand {
  text-align: center;
  font-size: 44rpx;
  font-weight: 600;
  color: $app-primary;
  margin-bottom: 32rpx;
}
.tabs {
  display: flex;
  margin-bottom: 24rpx;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  color: $app-text-secondary;
  border-bottom: 4rpx solid transparent;
}
.tab.active {
  color: $app-primary;
  border-bottom-color: $app-primary;
}
.form {
  margin-bottom: 32rpx;
}
.hint {
  margin-top: 24rpx;
  font-size: 24rpx;
  color: $app-text-secondary;
  text-align: center;
}
</style>
