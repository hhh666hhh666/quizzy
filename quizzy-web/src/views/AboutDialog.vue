<template>
  <Dialog :open="visible" @update:open="(v: boolean) => emit('update:visible', v)">
    <DialogContent class="max-w-md">
      <DialogHeader>
        <DialogTitle class="text-center text-2xl font-semibold text-brand">Quizzy</DialogTitle>
        <DialogDescription class="text-center text-sm text-ink-muted">
          个人自学刷题工具，围绕「一道题反复练到会」组织。
        </DialogDescription>
      </DialogHeader>

      <div class="text-center">
        <div class="mt-2">
          <span class="text-xl font-semibold">{{ version }}</span>
          <!-- 只在领先 tag 时出现：提醒你跑的这份代码里还有未发版的东西 -->
          <span v-if="ahead > 0" class="mt-1 block text-xs text-ink-muted">+{{ ahead }} 提交 · {{ commit }}</span>
        </div>

        <div class="mt-4 text-left text-sm">
          <div class="flex items-center justify-between border-b border-line-soft py-2">
            <span class="text-ink-muted">构建时间</span>
            <span>{{ buildTimeText }}</span>
          </div>
          <div class="flex items-center justify-between border-b border-line-soft py-2">
            <span class="text-ink-muted">仓库</span>
            <a class="link-button text-brand hover:underline" :href="REPO_URL" target="_blank" rel="noopener">{{ REPO_URL }}</a>
          </div>
          <div class="flex items-center justify-between py-2">
            <span class="text-ink-muted">许可证</span>
            <span>MIT</span>
          </div>
        </div>
      </div>

      <DialogFooter>
        <Button class="w-full" @click="emit('update:visible', false)">关闭</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'

defineProps<{ visible: boolean }>()
const emit = defineEmits(['update:visible'])

const REPO_URL = 'https://github.com/hhh666hhh666/quizzy'

// 这几个是构建期注入的全局常量（声明见 src/env.d.ts）。不能直接在模板里用：
// <script setup> 的模板只认 setup 作用域内的绑定，未识别的标识符会被解析成 _ctx.*，
// 拿到 undefined。所以先在这里接成普通常量。
const version = __APP_VERSION__
const commit = __APP_COMMIT__
const ahead = Number(__APP_AHEAD__) || 0

// 未注入时（宿主机直跑）buildTime 是 unknown，直接显示会很怪
const buildTimeText = computed(() =>
  __APP_BUILD_TIME__ === 'unknown' ? '未注入（本地开发环境）' : __APP_BUILD_TIME__
)
</script>
