<template>
  <!--
    提示层。z-[70] 在弹窗(z-50)之上 + 显式 pointer-events:auto——
    这样即便 reka 模态开着（body 被挂 pointer-events:none），提示上的按钮也点得动。
  -->
  <div class="pointer-events-none fixed bottom-4 right-4 z-[70] flex w-80 flex-col gap-2">
    <div
      v-for="item in toasts"
      :key="item.id"
      class="toast pointer-events-auto flex items-start gap-2.5 rounded-lg border border-line bg-surface p-3 shadow-md"
      :class="`toast--${item.type}`"
      :role="item.type === 'error' ? 'alert' : 'status'"
    >
      <span class="mt-1.5 size-2 shrink-0 rounded-full" :class="dotClass(item.type)" aria-hidden="true" />
      <div class="min-w-0 flex-1 text-sm leading-relaxed text-ink">
        <span>{{ item.message }}</span>
        <button
          v-if="item.action"
          type="button"
          class="ml-2 shrink-0 text-brand hover:underline"
          @click="onAction(item)"
        >
          {{ item.action.label }}
        </button>
      </div>
      <button
        type="button"
        class="shrink-0 leading-none text-ink-subtle hover:text-ink"
        aria-label="关闭提示"
        @click="dismissToast(item.id)"
      >
        ×
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { dismissToast, toasts, type ToastItem, type ToastType } from '@/lib/toast'

function dotClass(type: ToastType) {
  return {
    success: 'bg-ok',
    info: 'bg-brand',
    warning: 'bg-warn',
    error: 'bg-danger'
  }[type]
}

function onAction(item: ToastItem) {
  item.action?.onClick(() => dismissToast(item.id))
}
</script>
