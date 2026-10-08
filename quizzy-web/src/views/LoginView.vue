<template>
  <div class="login-page flex min-h-screen items-center justify-center bg-canvas p-6 font-sans text-base text-ink">
    <!-- 挑定构图（2026-10-08）：双栏——左身份面是 tint 容器，右表单面是近白面，卡体不铺玻璃 -->
    <div class="login-card grid w-full max-w-2xl overflow-hidden rounded-xl bg-tint-blue shadow-sm sm:grid-cols-[1fr_1.15fr]">
      <!-- 左：身份面 -->
      <div class="flex flex-col justify-center p-6 sm:p-8">
        <h1 class="text-xl font-semibold tracking-tight">quizzy</h1>
        <p class="mt-2 text-sm text-ink-muted">个人自主刷题与记忆强化系统</p>
      </div>

      <!-- 右：表单面（登录 / 注册同卡两态） -->
      <div class="bg-surface p-6 sm:p-8">
        <form class="flex flex-col gap-4" novalidate @submit.prevent="onSubmit">
          <div class="flex flex-col gap-1.5">
            <Label for="auth-username" class="text-xs text-ink-muted">用户名</Label>
            <Input
              id="auth-username"
              v-model="form.username"
              :data-testid="mode === 'login' ? 'login-username' : 'register-username'"
              autocomplete="username"
              name="username"
              :spellcheck="false"
              placeholder="输入用户名…"
              class="bg-reader"
            />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="auth-password" class="text-xs text-ink-muted">密码</Label>
            <div class="relative">
              <Input
                id="auth-password"
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                :data-testid="mode === 'login' ? 'login-password' : 'register-password'"
                :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
              name="password"
                placeholder="输入密码…"
                class="bg-reader pr-12"
              />
              <button
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 rounded-sm text-xs text-ink-muted outline-none hover:text-ink focus-visible:ring-2 focus-visible:ring-focus"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                {{ showPassword ? '隐藏' : '显示' }}
              </button>
            </div>
          </div>

          <div v-if="mode === 'register'" class="flex flex-col gap-1.5">
            <Label for="auth-nickname" class="text-xs text-ink-muted">昵称（可留空）</Label>
            <Input id="auth-nickname" v-model="form.nickname" data-testid="register-nickname" class="bg-reader" />
          </div>

          <p v-if="errorMsg" class="rounded-md bg-danger-soft px-3 py-2 text-sm" role="alert">
            {{ errorMsg }}
          </p>

          <Button
            type="submit"
            class="w-full hover:opacity-90"
            :disabled="loading"
            :data-testid="mode === 'register' ? 'register-submit' : undefined"
          >
            {{ submitText }}
          </Button>

          <p class="text-center text-xs text-ink-muted">
            <template v-if="mode === 'login'">
              还没有账号？
              <button
                type="button"
                class="rounded-sm text-brand outline-none hover:underline focus-visible:ring-2 focus-visible:ring-focus"
                @click="switchMode('register')"
              >
                注册
              </button>
            </template>
            <template v-else>
              已有账号？
              <button
                type="button"
                class="rounded-sm text-brand outline-none hover:underline focus-visible:ring-2 focus-visible:ring-focus"
                @click="switchMode('login')"
              >
                登录
              </button>
            </template>
          </p>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

const router = useRouter()
const store = useUserStore()

const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const errorMsg = ref('')
const showPassword = ref(false)
const form = reactive({ username: '', password: '', nickname: '' })

const submitText = computed(() => {
  if (loading.value) return mode.value === 'login' ? '登录中…' : '注册中…'
  return mode.value === 'login' ? '登录' : '注册并登录'
})

async function onSubmit() {
  if (loading.value) return
  errorMsg.value = ''
  loading.value = true
  try {
    if (mode.value === 'login') {
      await store.login(form.username, form.password)
    } else {
      await store.register(form.username, form.password, form.nickname)
    }
    router.push('/questions')
  } catch (err) {
    // 接口错误走内联提示（登录 / 注册是 silent 调用，不弹全局 ElMessage——见 api/request.ts）
    errorMsg.value = err instanceof Error && err.message ? err.message : '操作失败，请重试'
  } finally {
    loading.value = false
  }
}

function switchMode(next: 'login' | 'register') {
  mode.value = next
  errorMsg.value = ''
  showPassword.value = false
}
</script>
