<template>
  <view class="page">
    <view class="card">
      <view class="brand">quizzy</view>
      <view class="sub">一道题反复练到会</view>

      <!-- 登录 / 注册两段切换：自绘（wot 的 tabs 视觉与我们这套不是一个调） -->
      <view class="seg">
        <view :class="['seg-item', tab === 'login' ? 'on' : '']" @click="tab = 'login'">登录</view>
        <view :class="['seg-item', tab === 'register' ? 'on' : '']" @click="tab = 'register'">注册</view>
      </view>

      <view class="field">
        <text class="label">用户名</text>
        <input
          class="input"
          v-model="form.username"
          placeholder="请输入用户名"
          placeholder-class="ph"
        />
      </view>
      <view class="field">
        <text class="label">密码</text>
        <input
          class="input"
          v-model="form.password"
          password
          placeholder="请输入密码"
          placeholder-class="ph"
        />
      </view>
      <view class="field" v-if="tab === 'register'">
        <text class="label">昵称</text>
        <input
          class="input"
          v-model="form.nickname"
          placeholder="可留空"
          placeholder-class="ph"
        />
      </view>

      <view class="qz-btn qz-btn--primary qz-btn--block" @click="onSubmit">
        {{ tab === 'login' ? (loading ? '登录中…' : '登录') : (loading ? '提交中…' : '注册并登录') }}
      </view>

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
  background: $app-surface;
  border-radius: $radius-xl;
  padding: 48rpx 40rpx;
  box-sizing: border-box;
  box-shadow: $shadow-md;
}
.brand {
  text-align: center;
  font-size: $text-2xl;
  font-weight: 500;
  color: $app-brand;
  letter-spacing: 2rpx;
}
.sub {
  text-align: center;
  margin-top: 8rpx;
  font-size: $text-sm;
  color: $app-ink-muted;
}

.seg {
  display: flex;
  gap: 8rpx;
  margin: 36rpx 0 28rpx;
  padding: 6rpx;
  background: $app-surface-2;
  border-radius: $radius-md;
}
.seg-item {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: $radius-sm;
  font-size: $text-md;
  color: $app-ink-muted;
}
.seg-item.on {
  background: $app-reader;
  color: $app-ink;
  font-weight: 500;
}

.field {
  margin-bottom: 24rpx;
}
.label {
  display: block;
  margin-bottom: 10rpx;
  font-size: $text-sm;
  color: $app-ink-muted;
}
.input {
  height: 88rpx;
  padding: 0 24rpx;
  background: $app-reader;
  border: 1rpx solid $app-line;
  border-radius: $radius-md;
  font-size: $text-md;
  color: $app-ink;
  box-sizing: border-box;
}
.ph {
  color: $app-ink-subtle;
}

.hint {
  margin-top: 28rpx;
  font-size: $text-sm;
  color: $app-ink-muted;
  text-align: center;
}
</style>
